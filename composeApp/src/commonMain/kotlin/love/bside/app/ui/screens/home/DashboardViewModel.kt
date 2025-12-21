package love.bside.app.ui.screens.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.Match
import love.bside.app.domain.repository.MessagingRepository
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope

class DashboardViewModel(
    private val repository: MessagingRepository
) : ViewModel() {

    private val _matches = MutableStateFlow<List<Match>>(emptyList())
    val matches: StateFlow<List<Match>> = _matches.asStateFlow()
    
    // Simple verification check to show loading or empty state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMatches()
    }

    fun loadMatches() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (val result = repository.getMatches()) {
                    is Result.Success -> {
                        _matches.value = result.data
                    }
                    is Result.Error -> {
                        println("Error loading matches: ${result.exception}")
                    }
                }
            } catch (e: Exception) {
                println("Error loading matches: $e")
            } finally {
                _isLoading.value = false
            }
        }
    }
}
