package love.bside.app.presentation.matches

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.data.storage.SessionManager
import love.bside.app.domain.models.Match
import love.bside.app.domain.repository.MatchRepository
import love.bside.app.ui.components.UiState
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

interface MatchScreenComponent {
    val uiState: StateFlow<MatchUiState>
    fun onMatchClicked(matchId: String)
}

data class MatchUiState(
    override val isLoading: Boolean = false,
    override val data: List<Match> = emptyList(),
    override val error: String? = null
) : UiState<List<Match>>

class DefaultMatchScreenComponent(
    componentContext: ComponentContext,
    private val onMatchClicked: (String) -> Unit
) : MatchScreenComponent, ComponentContext by componentContext, KoinComponent {

    private val matchRepository: MatchRepository by inject()
    private val sessionManager: SessionManager by inject()

    private val _uiState = MutableStateFlow(MatchUiState())
    override val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    private val userId: String
        get() = sessionManager.getProfile()?.userId ?: ""

    init {
        fetchMatches()
    }

    private fun fetchMatches() {
        if (userId.isEmpty()) return
        _uiState.value = MatchUiState(isLoading = true)
        coroutineScope().launch {
            val result = matchRepository.getMatches(userId)
            _uiState.value = result.fold(
                onSuccess = { MatchUiState(data = it) },
                onFailure = { MatchUiState(error = it.message) }
            )
        }
    }

    override fun onMatchClicked(matchId: String) {
        onMatchClicked.invoke(matchId)
    }
}
