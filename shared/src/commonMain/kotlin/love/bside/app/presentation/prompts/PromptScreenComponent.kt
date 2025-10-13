package love.bside.app.presentation.prompts

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.di.getDI
import love.bside.app.domain.models.Prompt
import love.bside.app.domain.repository.MatchRepository
import love.bside.app.ui.components.UiState

interface PromptScreenComponent {
    val uiState: StateFlow<PromptUiState>
}

data class PromptUiState(
    override val isLoading: Boolean = false,
    override val data: List<Prompt> = emptyList(),
    override val error: String? = null
) : UiState<List<Prompt>>

class DefaultPromptScreenComponent(
    componentContext: ComponentContext,
    private val matchId: String
) : PromptScreenComponent, ComponentContext by componentContext {

    private val di = getDI()
    private val matchRepository: MatchRepository by di.inject(MatchRepository::class)

    private val _uiState = MutableStateFlow(PromptUiState())
    override val uiState: StateFlow<PromptUiState> = _uiState.asStateFlow()

    init {
        fetchPrompts()
    }

    private fun fetchPrompts() {
        _uiState.value = PromptUiState(isLoading = true)
        coroutineScope().launch {
            val result = matchRepository.getPromptsForMatch(matchId)
            _uiState.value = result.fold(
                onSuccess = { PromptUiState(data = it) },
                onFailure = { PromptUiState(error = it.message) }
            )
        }
    }
}
