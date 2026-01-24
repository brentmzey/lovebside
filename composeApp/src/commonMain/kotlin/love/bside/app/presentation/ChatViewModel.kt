package love.bside.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.Message
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
        markAsRead(conversationId)
    }

    private fun loadHistory(conversationId: String) {
        viewModelScope.launch {
            when (val result = repository.getMessages(conversationId)) {
                is Result.Success ->
                        _messages.value =
                                result.data
                                        .reversed() // Oldest top, newest bottom usually? Or verify
                // sort. Repo sorts -sentAt (newest first). So
                // reverse for chat UI (bottom up).
                is Result.Error -> {
                    /* Handle error */
                }
                else -> {
                    /* Handle unknown state */
                }
            }
        }
    }

    private fun subscribeToRealtime(conversationId: String) {
        subscriptionJob?.cancel()
        subscriptionJob =
                viewModelScope.launch {
                    repository.subscribeToConversation(conversationId).collect { newMessage ->
                        // Handle Create vs Update
                        val currentList = _messages.value.toMutableList()
                        val index = currentList.indexOfFirst { it.id == newMessage.id }

                        if (index != -1) {
                            // Update existing (e.g. Read Receipt)
                            currentList[index] = newMessage
                        } else {
                            // New Message
                            currentList.add(newMessage)
                        }

                        _messages.value = currentList

                        // Mark as read if it's a new message from someone else
                        // (Optimization: only if we haven't read it yet)
                        if (newMessage.senderId != userId && newMessage.readAt == null) {
                            markAsRead(conversationId)
                        }
                    }
                }

        typingSubscriptionJob?.cancel()
        typingSubscriptionJob =
                viewModelScope.launch {
                    repository.subscribeToTypingIndicators(conversationId).collect { status ->
                        val currentMap = _typingStatus.value.toMutableMap()
                        // If it's me, ignore (local state handles it, or we rely on server echo?)
                        // Usually ignore self to avoid loop, but if server echoes, filtering here
                        // is good.
                        if (status.userId != userId) {
                            if (status.isTyping) {
                                currentMap[conversationId] =
                                        true // Using convId for now as simple boolean
                                // TODO: Map userId to typing status for "User X is typing..."
                            } else {
                                currentMap.remove(conversationId)
                            }
                            _typingStatus.value = currentMap
                        }
                    }
                }
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            // Optimistic update could go here
            repository.sendMessage(convId, content, replyToId)
        }
    }

    fun sendAttachment(file: love.bside.app.domain.repository.AttachmentData) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            repository.sendMessage(
                            conversationId = convId,
                            content = "",
                            replyToMessageId = null,
                            attachments = listOf(file)
                    )
                    .onError { e -> println("Failed to send attachment: ${e.message}") }
        }
    }

    private var lastTypingSentAt: Long = 0
    private var typingResetJob: Job? = null
    private val TYPING_THROTTLE_MS = 3000L

    fun onTyping(text: String) {
        // 1. Send "isTyping = true" if throttled
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        if (now - lastTypingSentAt > TYPING_THROTTLE_MS) {
            setTyping(true)
            lastTypingSentAt = now
        }

        // 2. Schedule "isTyping = false" after inactivity (debounce)
        typingResetJob?.cancel()
        typingResetJob =
                viewModelScope.launch {
                    kotlinx.coroutines.delay(TYPING_THROTTLE_MS)
                    setTyping(false)
                }
    }

    private fun setTyping(isTyping: Boolean) {
        val convId = currentConversationId ?: return
        viewModelScope.launch {
            try {
                repository.setTypingStatus(convId, isTyping)
            } catch (e: Exception) {
                // Ignore typing errors silently
            }
        }
    }

    fun markAsRead(messageId: String) {
        viewModelScope.launch {
            try {
                repository.markAsRead(messageId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        subscriptionJob?.cancel()
        typingSubscriptionJob?.cancel()
    }
}
