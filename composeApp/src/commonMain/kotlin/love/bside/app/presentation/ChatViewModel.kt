package love.bside.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus
import love.bside.app.domain.repository.MessagingRepository

class ChatViewModel(
    private val repository: MessagingRepository,
    private val userId: String // USING STRING NOW, BUT SHOULD BE UUID V4 TYPE
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _typingStatus = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingStatus: StateFlow<Map<String, Boolean>> = _typingStatus.asStateFlow()

    private var currentConversationId: String? = null
    private var subscriptionJob: Job? = null
    private var typingSubscriptionJob: Job? = null

    fun loadConversation(conversationId: String) {
        currentConversationId = conversationId
        loadHistory(conversationId)
        subscribeToRealtime(conversationId)
    }

    private fun loadHistory(conversationId: String) {
        viewModelScope.launch {
            when (val result = repository.getMessages(conversationId)) {
                is Result.Success -> _messages.value = result.data.reversed() // Oldest top, newest bottom usually? Or verify sort. Repo sorts -sentAt (newest first). So reverse for chat UI (bottom up).
                is Result.Error -> { /* Handle error */ }
                else -> { /* Handle unknown state */ }
            }
        }
    }

    private fun subscribeToRealtime(conversationId: String) {
        subscriptionJob?.cancel()
        subscriptionJob = viewModelScope.launch {
            repository.subscribeToConversation(conversationId).collect { newMessage ->
                _messages.value = _messages.value + newMessage
            }
        }

        typingSubscriptionJob?.cancel()
        typingSubscriptionJob = viewModelScope.launch {
            repository.subscribeToTypingIndicators(conversationId).collect { status ->
                 // Logic to update typing map
                 // Simplified for now
            }
        }
    }

    fun sendMessage(content: String) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            // Optimistic update could go here
            repository.sendMessage(convId, content)
        }
    }

    fun setTyping(isTyping: Boolean) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            repository.setTypingStatus(convId, isTyping)
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
    }
}
