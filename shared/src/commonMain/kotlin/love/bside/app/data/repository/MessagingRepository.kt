package love.bside.app.data.repository

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import love.bside.app.core.Result
import love.bside.app.data.api.MessagingApiClient
import love.bside.app.data.models.messaging.*
import kotlinx.datetime.Clock

/**
 * Repository for messaging functionality
 * Manages conversations, messages, and real-time subscriptions
 */
class MessagingRepository(
    private val apiClient: MessagingApiClient
) {
    // Cache for conversations
    private val conversationsCache = RepositoryCache<String, List<Conversation>>(ttlMillis = 30_000) // 30 seconds
    
    // Cache for messages per conversation
    private val messagesCache = mutableMapOf<String, RepositoryCache<Int, Page<Message>>>()
    
    // Active real-time subscriptions
    private val activeSubscriptions = mutableMapOf<String, Flow<MessageEvent>>()
    
    // ===== Conversations =====
    
    /**
     * Get all conversations for current user (with caching)
     */
    suspend fun getConversations(forceRefresh: Boolean = false): Result<List<Conversation>> {
        return withCache(
            cache = conversationsCache,
            key = "all",
            forceRefresh = forceRefresh
        ) {
            when (val result = apiClient.getConversations()) {
                is Result.Success -> result.data
                is Result.Error -> throw Exception(result.exception.message)
                is Result.Loading -> throw Exception("Loading")
            }
        }.let { Result.Success(it) }
    }
    
    /**
     * Get or create a conversation with another user
     */
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> {
        // Clear cache to force refresh after creation
        conversationsCache.clear()
        return apiClient.getOrCreateConversation(otherUserId)
    }
    
    /**
     * Get conversation by ID
     */
    suspend fun getConversation(conversationId: String): Result<Conversation> {
        return apiClient.getConversation(conversationId)
    }
    
    // ===== Messages =====
    
    /**
     * Get messages for a conversation with pagination and caching
     */
    suspend fun getMessages(
        conversationId: String,
        page: Int = 1,
        pageSize: Int = 50,
        forceRefresh: Boolean = false
    ): Result<Page<Message>> {
        // Get or create cache for this conversation
        val cache = messagesCache.getOrPut(conversationId) {
            RepositoryCache(ttlMillis = 60_000) // 1 minute
        }
        
        return withCache(
            cache = cache,
            key = page,
            forceRefresh = forceRefresh
        ) {
            when (val result = apiClient.getMessages(conversationId, page, pageSize)) {
                is Result.Success -> result.data
                is Result.Error -> throw Exception(result.exception.message)
                is Result.Loading -> throw Exception("Loading")
            }
        }.let { Result.Success(it) }
    }
    
    /**
     * Send a message
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT
    ): Result<Message> {
        // Invalidate message cache for this conversation
        messagesCache[conversationId]?.clear()
        
        // Invalidate conversations cache (to update last message)
        conversationsCache.clear()
        
        return apiClient.sendMessage(conversationId, content, messageType)
    }
    
    /**
     * Mark message as read
     */
    suspend fun markAsRead(messageId: String): Result<Unit> {
        return apiClient.markMessageAsRead(messageId)
    }
    
    /**
     * Mark multiple messages as read (batch)
     */
    suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> {
        if (messageIds.isEmpty()) return Result.Success(Unit)
        
        return batchProcess(messageIds, batchSize = 20) { batch ->
            apiClient.markMessagesAsRead(batch)
            batch
        }.let { Result.Success(Unit) }
    }
    
    /**
     * Delete a message
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> {
        // Invalidate caches
        messagesCache.values.forEach { it.clear() }
        return apiClient.deleteMessage(messageId)
    }
    
    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Message> {
        // Invalidate caches
        messagesCache.values.forEach { it.clear() }
        return apiClient.editMessage(messageId, newContent)
    }
    
    // ===== Typing Indicators =====
    
    /**
     * Update typing status (with automatic cleanup)
     */
    suspend fun updateTypingStatus(
        conversationId: String,
        isTyping: Boolean
    ): Result<Unit> {
        return apiClient.updateTypingStatus(conversationId, isTyping)
    }
    
    // ===== Real-time Subscriptions =====
    
    /**
     * Subscribe to real-time messages for a conversation
     * Manages subscription lifecycle and caching
     */
    fun subscribeToMessages(conversationId: String): Flow<MessageEvent> {
        return activeSubscriptions.getOrPut(conversationId) {
            apiClient.subscribeToMessages(conversationId)
                .onEach { event ->
                    // Update cache when new messages arrive
                    when (event) {
                        is MessageEvent.NewMessage -> {
                            messagesCache[conversationId]?.clear()
                            conversationsCache.clear()
                        }
                        is MessageEvent.MessageUpdated -> {
                            messagesCache[conversationId]?.clear()
                        }
                        is MessageEvent.MessageDeleted -> {
                            messagesCache[conversationId]?.clear()
                        }
                        is MessageEvent.MessageRead -> {
                            // No cache invalidation needed for read receipts
                        }
                    }
                }
                .shareIn(
                    scope = kotlinx.coroutines.GlobalScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    replay = 0
                )
        }
    }
    
    /**
     * Subscribe to typing indicators
     */
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingEvent> {
        return apiClient.subscribeToTypingIndicators(conversationId)
    }
    
    /**
     * Subscribe to conversation updates
     */
    fun subscribeToConversations(): Flow<ConversationEvent> {
        return apiClient.subscribeToConversations()
            .onEach {
                // Clear cache on conversation updates
                conversationsCache.clear()
            }
    }
    
    /**
     * Unsubscribe from a conversation
     */
    fun unsubscribeFromConversation(conversationId: String) {
        activeSubscriptions.remove(conversationId)
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        conversationsCache.clear()
        messagesCache.values.forEach { it.clear() }
    }
}

/**
 * Debouncer for typing indicators
 * Prevents sending too many typing status updates
 */
class TypingDebouncer(
    private val delayMillis: Long = 300,
    private val autoStopMillis: Long = 3000
) {
    private var lastTypingTime: Long = 0
    private var isCurrentlyTyping = false
    private var debounceJob: Job? = null
    private var autoStopJob: Job? = null
    
    /**
     * Handle text input change
     * Returns true if typing status should be updated
     */
    fun onTextChanged(
        text: String,
        scope: CoroutineScope,
        onTypingStatusChange: suspend (Boolean) -> Unit
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        
        // Cancel existing jobs
        debounceJob?.cancel()
        autoStopJob?.cancel()
        
        if (text.isEmpty()) {
            // Stop typing immediately if text is empty
            if (isCurrentlyTyping) {
                isCurrentlyTyping = false
                scope.launch {
                    onTypingStatusChange(false)
                }
            }
            return
        }
        
        // Debounce typing start
        debounceJob = scope.launch {
            delay(delayMillis)
            
            if (!isCurrentlyTyping) {
                isCurrentlyTyping = true
                onTypingStatusChange(true)
            }
            
            lastTypingTime = now
            
            // Auto-stop after inactivity
            autoStopJob = launch {
                delay(autoStopMillis)
                if (isCurrentlyTyping) {
                    isCurrentlyTyping = false
                    onTypingStatusChange(false)
                }
            }
        }
    }
    
    /**
     * Force stop typing indicator
     */
    fun forceStop(scope: CoroutineScope, onTypingStatusChange: suspend (Boolean) -> Unit) {
        debounceJob?.cancel()
        autoStopJob?.cancel()
        
        if (isCurrentlyTyping) {
            isCurrentlyTyping = false
            scope.launch {
                onTypingStatusChange(false)
            }
        }
    }
}
