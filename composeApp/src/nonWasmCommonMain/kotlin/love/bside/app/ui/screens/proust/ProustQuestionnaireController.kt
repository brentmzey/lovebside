package love.bside.app.ui.screens.proust

import io.pocketbase.PocketBase
import io.pocketbase.config.RealtimeConfig
import io.pocketbase.config.RealtimeMode
import io.pocketbase.config.RealtimeTransportKind
import io.pocketbase.config.SmartPollingConfig
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RealtimeAction
import io.pocketbase.models.UnsubscribeFunc
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val PROMPTS_COLLECTION = "s_prompts"

/**
 * UI-facing representation of a single prompt/question.
 */
data class ProustQuestion(
    val id: String,
    val text: String,
    val category: String,
    val displayOrder: Int
)

/**
 * Draft answer that is stored locally while the user fills out the flow.
 */
data class ProustAnswerDraft(
    val questionId: String,
    val text: String,
    val updatedAt: Instant
)

/**
 * Immutable UI state consumed by the Compose screen.
 */
data class ProustQuestionnaireUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val questions: List<ProustQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val answers: Map<String, ProustAnswerDraft> = emptyMap(),
    val isRealtimeEnabled: Boolean = true,
    val isRealtimeConnected: Boolean = false,
    val activeTransport: RealtimeTransportKind = RealtimeTransportKind.INACTIVE,
    val realtimeFailures: Int = 0
) {
    val currentQuestion: ProustQuestion?
        get() = questions.getOrNull(currentIndex)

    val currentAnswer: String
        get() = currentQuestion?.let { answers[it.id]?.text }.orEmpty()

    val progressLabel: String
        get() = if (questions.isEmpty()) "0 / 0" else "${currentIndex + 1} / ${questions.size}"

    val completionPercent: Float
        get() = if (questions.isEmpty()) 0f else ((currentIndex + 1f) / questions.size.toFloat()).coerceIn(0f, 1f)
}

/**
 * Coordinates PocketBase reads + realtime updates and exposes a simple state flow for Compose.
 */
class ProustQuestionnaireController(
    rawBaseUrl: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val pocketBase = PocketBase(
        baseURL = sanitizePocketBaseBaseUrl(rawBaseUrl),
        realtimeConfig = RealtimeConfig(
            mode = RealtimeMode.HYBRID,
            smartPolling = SmartPollingConfig(
                initialDelayMs = 1_200,
                minDelayMs = 1_000,
                maxDelayMs = 12_000,
                jitterRatio = 0.30,
                deleteAfterMisses = 2,
                activationThreshold = 2,
                batchSize = 150
            )
        )
    )

    private val _state = MutableStateFlow(ProustQuestionnaireUiState(isLoading = true))
    val state: StateFlow<ProustQuestionnaireUiState> = _state.asStateFlow()

    private var realtimeJob: Job? = null
    private var unsubscribeFunc: UnsubscribeFunc? = null

    init {
        scope.launch {
            pocketBase.realtime.activeTransport.collect { transport ->
                _state.update { it.copy(activeTransport = transport) }
            }
        }
        scope.launch { loadQuestions(force = true) }
    }

    fun refresh() {
        scope.launch { loadQuestions(force = true) }
    }

    fun updateCurrentAnswer(text: String) {
        val questionId = state.value.currentQuestion?.id ?: return
        val draft = ProustAnswerDraft(questionId, text, Clock.System.now())
        _state.update { current ->
            current.copy(answers = current.answers + (questionId to draft))
        }
    }

    fun goToNextQuestion() {
        _state.update { current ->
            if (current.questions.isEmpty()) current
            else current.copy(currentIndex = (current.currentIndex + 1).coerceAtMost(current.questions.lastIndex))
        }
    }

    fun goToPreviousQuestion() {
        _state.update { current ->
            if (current.questions.isEmpty()) current
            else current.copy(currentIndex = (current.currentIndex - 1).coerceAtLeast(0))
        }
    }

    fun jumpToQuestion(questionId: String) {
        _state.update { current ->
            val newIndex = current.questions.indexOfFirst { it.id == questionId }
            if (newIndex < 0) current else current.copy(currentIndex = newIndex)
        }
    }

    fun setRealtimeEnabled(enabled: Boolean) {
        if (enabled == state.value.isRealtimeEnabled) return
        _state.update { it.copy(isRealtimeEnabled = enabled) }
        if (enabled) {
            startRealtimeSubscription(restart = true)
        } else {
            stopRealtime()
        }
    }

    fun dispose() {
        stopRealtime()
        scope.cancel()
        pocketBase.close()
    }

    private suspend fun loadQuestions(force: Boolean) {
        if (state.value.isLoading && !force) return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        val questions = runCatching { fetchQuestions() }
            .getOrElse { error ->
                _state.update { current ->
                    val fallback = if (current.questions.isEmpty()) sampleQuestions else current.questions
                    current.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to load prompts",
                        questions = fallback,
                        currentIndex = fallback.safeIndex(current.currentIndex)
                    )
                }
                startRealtimeSubscription(restart = false)
                return
            }

        val normalized = if (questions.isEmpty()) sampleQuestions else questions.sortedBy { it.displayOrder }
        _state.update {
            it.copy(
                isLoading = false,
                errorMessage = null,
                questions = normalized,
                currentIndex = normalized.safeIndex(0)
            )
        }
        if (state.value.isRealtimeEnabled) {
            startRealtimeSubscription(restart = true)
        }
    }

    private suspend fun fetchQuestions(): List<ProustQuestion> {
        val records = pocketBase.collection(PROMPTS_COLLECTION).getFullList(
            QueryOptions(
                sort = "+displayOrder",
                fields = "id,text,category,displayOrder"
            )
        )
        return records.mapNotNull { it.toQuestion() }
    }

    private fun startRealtimeSubscription(restart: Boolean) {
        if (!state.value.isRealtimeEnabled) return
        if (!restart && realtimeJob?.isActive == true) return
        realtimeJob?.cancel()
        realtimeJob = scope.launch {
            runCatching {
                unsubscribeFunc?.invoke()
                unsubscribeFunc = pocketBase.collection(PROMPTS_COLLECTION).subscribe(
                    recordId = "*",
                    callback = { event ->
                        val payload = event.record.jsonObjectOrNull()
                        when (event.action) {
                            RealtimeAction.delete -> {
                                val id = payload?.stringOrNull("id") ?: return@subscribe
                                _state.update { it.removeQuestion(id) }
                            }
                            RealtimeAction.create, RealtimeAction.update -> {
                                val question = payload?.toQuestion() ?: return@subscribe
                                _state.update { it.upsertQuestion(question) }
                            }
                        }
                    }
                )
                _state.update { it.copy(isRealtimeConnected = true, realtimeFailures = 0) }
            }.onFailure { error ->
                _state.update {
                    it.copy(
                        isRealtimeConnected = false,
                        realtimeFailures = it.realtimeFailures + 1,
                        errorMessage = error.message ?: "Realtime subscription failed"
                    )
                }
            }
        }
    }

    private fun stopRealtime() {
        realtimeJob?.cancel()
        realtimeJob = null
        scope.launch {
            runCatching { unsubscribeFunc?.invoke() }
            unsubscribeFunc = null
        }
        _state.update { it.copy(isRealtimeConnected = false, activeTransport = RealtimeTransportKind.INACTIVE) }
    }
}

private fun List<ProustQuestion>.safeIndex(preferredIndex: Int): Int {
    if (isEmpty()) return 0
    return preferredIndex.coerceIn(0, lastIndex)
}

private fun ProustQuestionnaireUiState.upsertQuestion(question: ProustQuestion): ProustQuestionnaireUiState {
    val filtered = questions.filterNot { it.id == question.id }
    val newList = (filtered + question).sortedBy { it.displayOrder }
    val newIndex = if (currentQuestion?.id == question.id) newList.indexOfFirst { it.id == question.id } else currentIndex
    return copy(questions = newList, currentIndex = newList.safeIndex(newIndex))
}

private fun ProustQuestionnaireUiState.removeQuestion(questionId: String): ProustQuestionnaireUiState {
    val newList = questions.filterNot { it.id == questionId }
    val newAnswers = answers - questionId
    return copy(
        questions = newList,
        answers = newAnswers,
        currentIndex = newList.safeIndex(currentIndex)
    )
}

private fun JsonObject.toQuestion(): ProustQuestion? {
    val id = stringOrNull("id") ?: return null
    val text = stringOrNull("text") ?: return null
    val category = stringOrNull("category") ?: "Proust"
    val order = stringOrNull("displayOrder")?.toIntOrNull() ?: Int.MAX_VALUE
    return ProustQuestion(id = id, text = text, category = category, displayOrder = order)
}

private fun JsonObject.stringOrNull(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

private fun JsonElement.jsonObjectOrNull(): JsonObject? = this as? JsonObject

internal fun sanitizePocketBaseBaseUrl(raw: String): String {
    var normalized = raw.trim()
    if (normalized.isEmpty()) return "https://127.0.0.1:8090"
    normalized = normalized.trimEnd('/')
    if (normalized.endsWith("/api")) {
        normalized = normalized.removeSuffix("/api")
    }
    return normalized.ifEmpty { "https://127.0.0.1:8090" }
}

private val sampleQuestions = listOf(
    ProustQuestion(
        id = "sample_introduction",
        text = "What story from your life do you share when you're trying to help someone truly understand you?",
        category = "Origin",
        displayOrder = 1
    ),
    ProustQuestion(
        id = "sample_music",
        text = "Which song or album instantly transports you back to a vivid memory?",
        category = "Soundtrack",
        displayOrder = 2
    ),
    ProustQuestion(
        id = "sample_values",
        text = "When do you feel the most at peace with yourself?",
        category = "Values",
        displayOrder = 3
    ),
    ProustQuestion(
        id = "sample_memory",
        text = "Who is someone outside your family who helped shape your worldview?",
        category = "Mentors",
        displayOrder = 4
    )
)
