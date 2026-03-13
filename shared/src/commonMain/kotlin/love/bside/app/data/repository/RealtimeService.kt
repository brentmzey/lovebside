package love.bside.app.data.repository

import kotlinx.coroutines.flow.Flow
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus

/**
 * Service that provides real-time messaging capabilities. It uses PocketBase's WebSocket real-time
 * API and falls back to smart polling when needed.
 * 
 * **Design Philosophy:**
 * This service is ALWAYS active once started - it's never disabled for "synchronicity".
 * The SDK handles connection management, reconnection, and transport fallback automatically.
 * Applications should rely on optimistic updates and eventual consistency rather than
 * disabling real-time to ensure fresh reads after writes.
 */
interface RealtimeService {
    /** Subscribe to message events for a specific conversation. */
    fun subscribeToConversation(conversationId: String): Flow<Message>

    /** Subscribe to typing‑indicator events for a specific conversation. */
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus>

    /** Subscribe to read receipt events for a specific conversation. */
    fun subscribeToReadReceipts(
            conversationId: String
    ): Flow<Any> // Using Any for now as we don't have a ReadReceipt model

    /** Push a typing‑status change for the current user. */
    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean)
}
