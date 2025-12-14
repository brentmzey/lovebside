package love.bside.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.repository.MessagingRepository

class MessagingViewModel(
    private val repository: MessagingRepository,
    private val userId: String // USING STRING NOW, BUT SHOULD BE UUID V4 TYPE
) : ViewModel() {

    private val _uiState = MutableStateFlow<MessagingUiState>(MessagingUiState.Loading)
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.value = MessagingUiState.Loading
            when (val result = repository.getConversations(userId)) {
                is Result.Success -> {
                    _uiState.value = MessagingUiState.Success(result.data)
                }
                is Result.Error -> {
                    _uiState.value = MessagingUiState.Error(result.exception.message ?: "Failed to load conversations")
                }
                else -> { /* Should not happen with sealed Result */ }
            }
        }
    }
}

sealed class MessagingUiState {
    data object Loading : MessagingUiState()
    data class Success(val conversations: List<Conversation>) : MessagingUiState()
    data class Error(val message: String) : MessagingUiState()
}
