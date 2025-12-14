package love.bside.app.data.repository

import kotlinx.coroutines.flow.Flow
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus

/**
 * Service that provides real‑time messaging capabilities.
 * It uses PocketBase's WebSocket real‑time API and falls back to smart polling when needed.
 */
interface RealtimeService {
    /** Subscribe to message events for a specific conversation. */
    fun subscribeToConversation(conversationId: String): Flow<Message>

    /** Subscribe to typing‑indicator events for a specific conversation. */
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus>

    /** Push a typing‑status change for the current user. */
    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean)
}
