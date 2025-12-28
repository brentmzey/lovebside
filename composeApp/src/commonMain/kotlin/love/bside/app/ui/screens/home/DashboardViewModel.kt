package love.bside.app.ui.screens.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.Match
import love.bside.app.domain.repository.MessagingRepository

class DashboardViewModel(
    private val repository: MessagingRepository,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()
    
    // Simple verification check to show loading or empty state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        scope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.getMatches()) {
                    is Result.Success -> _matches.value = result.data
                    is Result.Error -> println("Error loading matches: ${result.exception}")
                    Result.Loading -> Unit
                }
            } catch (e: Exception) {
                println("Error loading matches: $e")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
