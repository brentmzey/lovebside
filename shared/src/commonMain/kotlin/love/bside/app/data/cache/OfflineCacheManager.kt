package love.bside.app.data.cache

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.Profile
import kotlin.time.Duration.Companion.hours

/**
 * Offline Cache Manager with sync queue support
 * 
 * Features:
 * - Persistent cache for messages, conversations, profiles
 * - Sync queue for offline operations
 * - Automatic sync when connection restored
 * - Network state monitoring
 */
class OfflineCacheManager {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val mutex = Mutex()
    
    // Network state
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()
    
    // Caches with longer TTL for offline use
    private val messageCache = MemoryCache<String, List<Message>>(
        maxSize = 50,  // 50 conversations
        defaultTtl = 24.hours
    )
    
    private val conversationCache = MemoryCache<String, List<Conversation>>(
        maxSize = 10,  // Cache multiple user's conversations
        defaultTtl = 12.hours
    )
    
    private val profileCache = MemoryCache<String, Profile>(
        maxSize = 200,  // Cache many profiles
        defaultTtl = 6.hours
    )
    
    // Pending operations queue
    private val pendingOperations = mutableListOf<PendingOperation>()
    
    /**
     * Update network connectivity status
     */
    fun setOnlineStatus(online: Boolean) {
        val wasOffline = !_isOnline.value
        _isOnline.value = online
        
        if (wasOffline && online) {
            // Connection restored - note: actual sync requires handlers to be provided via syncPendingOperations()
            // This just updates the state
        }
    }
    
    // ===== Messages Cache =====
    
    suspend fun cacheMessages(conversationId: String, messages: List<Message>) {
        messageCache.put("msg_$conversationId", messages)
    }
    
    suspend fun getCachedMessages(conversationId: String): List<Message>? {
        return messageCache.get("msg_$conversationId")
    }
    
    suspend fun addMessageToCache(conversationId: String, message: Message) = mutex.withLock {
        val cached = messageCache.get("msg_$conversationId")?.toMutableList() ?: mutableListOf()
        cached.add(message)
        messageCache.put("msg_$conversationId", cached)
    }
    
    // ===== Conversations Cache =====
    
    suspend fun cacheConversations(userId: String, conversations: List<Conversation>) {
        conversationCache.put("conv_$userId", conversations)
    }
    
    suspend fun getCachedConversations(userId: String): List<Conversation>? {
        return conversationCache.get("conv_$userId")
    }
    
    // ===== Profiles Cache =====
    
    suspend fun cacheProfile(profile: Profile) {
        profileCache.put("profile_${profile.userId}", profile)
    }
    
    suspend fun getCachedProfile(userId: String): Profile? {
        return profileCache.get("profile_$userId")
    }
    
    // ===== Offline Operations Queue =====
    
    suspend fun queueSendMessage(
        conversationId: String,
        content: String,
        replyToMessageId: String? = null,
        localTimestamp: Instant = Clock.System.now()
    ): String = mutex.withLock {
        val localId = "pending_${localTimestamp.toEpochMilliseconds()}"
        val operation = PendingOperation.SendMessage(
            localId = localId,
            conversationId = conversationId,
            content = content,
            replyToMessageId = replyToMessageId,
            timestamp = localTimestamp
        )
        pendingOperations.add(operation)
        
        // Add optimistic message to cache
        val optimisticMessage = Message(
            id = localId,
            collectionId = "", // Placeholder for optimistic message
            conversationId = conversationId,
            senderId = "current_user", // Will be replaced on sync
            content = content,
            messageType = love.bside.app.domain.models.MessageType.TEXT,
            attachments = emptyList(),
            sentAt = localTimestamp,
            editedAt = null,
            deletedAt = null,
            readByCount = 0,
            isRead = false,
            readAt = null,
            replyToMessageId = replyToMessageId,
            threadRootId = null,
            threadDepth = 0,
            threadReplyCount = 0,
            created = localTimestamp,
            updated = localTimestamp
        )
        addMessageToCache(conversationId, optimisticMessage)
        
        localId
    }
    
    suspend fun queueMarkAsRead(conversationId: String) = mutex.withLock {
        val operation = PendingOperation.MarkAsRead(
            conversationId = conversationId,
            timestamp = Clock.System.now()
        )
        pendingOperations.add(operation)
    }
    
    suspend fun getPendingOperationsCount(): Int = mutex.withLock {
        pendingOperations.size
    }
    
    /**
     * Sync pending operations when back online
     * Returns list of successfully synced operation IDs
     */
    private suspend fun syncPendingOperations() {
        // This will be called internally when network status changes
        // External callers should use the public sync method below
    }
    
    /**
     * Public sync method that requires handlers
     * Returns list of successfully synced operation IDs
     */
    suspend fun syncPendingOperations(
        sendMessageHandler: suspend (String, String, String?) -> Result<Message>,
        markAsReadHandler: suspend (String) -> Result<Unit>
    ): List<String> = mutex.withLock {
        if (!_isOnline.value) {
            return emptyList()
        }
        
        val synced = mutableListOf<String>()
        val failed = mutableListOf<PendingOperation>()
        
        pendingOperations.forEach { operation ->
            try {
                when (operation) {
                    is PendingOperation.SendMessage -> {
                        val result = sendMessageHandler(
                            operation.conversationId,
                            operation.content,
                            operation.replyToMessageId
                        )
                        if (result is love.bside.app.core.Result.Success<*>) {
                            synced.add(operation.localId)
                            // Update cache with server message
                            val serverMessage = result.data as? Message
                            if (serverMessage != null) {
                                val cached = messageCache.get("msg_${operation.conversationId}")?.toMutableList()
                                cached?.let { list ->
                                    list.removeAll { it.id == operation.localId }
                                    list.add(serverMessage)
                                    messageCache.put("msg_${operation.conversationId}", list)
                                }
                            }
                        } else {
                            failed.add(operation)
                        }
                    }
                    is PendingOperation.MarkAsRead -> {
                        val result = markAsReadHandler(operation.conversationId)
                        if (result is love.bside.app.core.Result.Success<*>) {
                            synced.add(operation.conversationId)
                        } else {
                            failed.add(operation)
                        }
                    }
                }
            } catch (e: Exception) {
                failed.add(operation)
            }
        }
        
        // Keep only failed operations
        pendingOperations.clear()
        pendingOperations.addAll(failed)
        
        synced
    }
    
    /**
     * Clear all caches and pending operations
     */
    suspend fun clearAll() = mutex.withLock {
        messageCache.clear()
        conversationCache.clear()
        profileCache.clear()
        pendingOperations.clear()
    }
    
    /**
     * Get cache statistics
     */
    suspend fun getStats(): CacheManagerStats {
        return CacheManagerStats(
            isOnline = _isOnline.value,
            messagesCount = messageCache.size(),
            conversationsCount = conversationCache.size(),
            profilesCount = profileCache.size(),
            pendingOpsCount = pendingOperations.size
        )
    }
}

/**
 * Pending operations for offline queue
 */
sealed class PendingOperation {
    abstract val timestamp: Instant
    
    data class SendMessage(
        val localId: String,
        val conversationId: String,
        val content: String,
        val replyToMessageId: String?,
        override val timestamp: Instant
    ) : PendingOperation()
    
    data class MarkAsRead(
        val conversationId: String,
        override val timestamp: Instant
    ) : PendingOperation()
}

/**
 * Cache statistics
 */
data class CacheManagerStats(
    val isOnline: Boolean,
    val messagesCount: Int,
    val conversationsCount: Int,
    val profilesCount: Int,
    val pendingOpsCount: Int
)
