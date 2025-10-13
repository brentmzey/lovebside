package love.bside.app.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.domain.models.Match
import love.bside.app.domain.repository.MatchRepository

data class MatchUiState(
    val isLoading: Boolean = false,
    val matches: List<Match> = emptyList(),
    val error: String? = null
)

class MatchViewModel(private val matchRepository: MatchRepository) {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(MatchUiState())
    val uiState: StateFlow<MatchUiState> = _uiState.asStateFlow()

    fun fetchMatches(userId: String) {
        _uiState.value = MatchUiState(isLoading = true)
        viewModelScope.launch {
            val result = matchRepository.getMatches(userId)
            _uiState.value = result.fold(
                onSuccess = { MatchUiState(matches = it) },
                onFailure = { MatchUiState(error = it.message) }
            )
        }
    }
}
