package love.bside.app.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import love.bside.app.core.Result
import love.bside.app.core.appConfig
import love.bside.app.data.models.messaging.*
import love.bside.app.data.repository.Page
import love.bside.app.data.storage.TokenStorage

/**
 * API client for real-time messaging functionality
 * Handles conversations, messages, typing indicators, and read receipts
 */
class MessagingApiClient(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) {
    private val config = appConfig()
    private val baseUrl = when (config.environment) {
        love.bside.app.core.Environment.DEVELOPMENT -> "http://localhost:8080/api/v1"
        love.bside.app.core.Environment.STAGING -> "https://staging.bside.love/api/v1"
        love.bside.app.core.Environment.PRODUCTION -> "https://www.bside.love/api/v1"
    }

    // ===== Conversations =====

    /**
     * Get all conversations for the current user
     */
    suspend fun getConversations(): Result<List<Conversation>> = safeApiCall {
        client.get("$baseUrl/conversations") {
            bearerAuth(tokenStorage.getToken() ?: "")
        }.body()
    }

    /**
     * Get or create a conversation with another user
     */
    suspend fun getOrCreateConversation(otherUserId: String): Result<Conversation> = safeApiCall {
        client.post("$baseUrl/conversations") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("otherUserId" to otherUserId))
        }.body()
    }

    /**
     * Get a specific conversation by ID
     */
    suspend fun getConversation(conversationId: String): Result<Conversation> = safeApiCall {
        client.get("$baseUrl/conversations/$conversationId") {
            bearerAuth(tokenStorage.getToken() ?: "")
        }.body()
    }

    // ===== Messages =====

    /**
     * Get messages for a conversation with pagination
     */
    suspend fun getMessages(
        conversationId: String,
        page: Int = 1,
        pageSize: Int = 50
    ): Result<Page<Message>> = safeApiCall {
        client.get("$baseUrl/conversations/$conversationId/messages") {
            bearerAuth(tokenStorage.getToken() ?: "")
            parameter("page", page)
            parameter("perPage", pageSize)
        }.body()
    }

    /**
     * Send a message in a conversation
     */
    suspend fun sendMessage(
        conversationId: String,
        content: String,
        messageType: MessageType = MessageType.TEXT
    ): Result<Message> = safeApiCall {
        client.post("$baseUrl/conversations/$conversationId/messages") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "content" to content,
                "messageType" to messageType.name.lowercase()
            ))
        }.body()
    }

    /**
     * Mark a message as read
     */
    suspend fun markMessageAsRead(messageId: String): Result<Unit> = safeApiCall {
        client.post("$baseUrl/messages/$messageId/read") {
            bearerAuth(tokenStorage.getToken() ?: "")
        }
        Unit
    }

    /**
     * Mark multiple messages as read (batch operation)
     */
    suspend fun markMessagesAsRead(messageIds: List<String>): Result<Unit> = safeApiCall {
        client.post("$baseUrl/messages/read/batch") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("messageIds" to messageIds))
        }
        Unit
    }

    /**
     * Delete a message (soft delete)
     */
    suspend fun deleteMessage(messageId: String): Result<Unit> = safeApiCall {
        client.delete("$baseUrl/messages/$messageId") {
            bearerAuth(tokenStorage.getToken() ?: "")
        }
        Unit
    }

    /**
     * Edit a message
     */
    suspend fun editMessage(messageId: String, newContent: String): Result<Message> = safeApiCall {
        client.put("$baseUrl/messages/$messageId") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("content" to newContent))
        }.body()
    }

    // ===== Typing Indicators =====

    /**
     * Update typing status for a conversation
     */
    suspend fun updateTypingStatus(
        conversationId: String,
        isTyping: Boolean
    ): Result<Unit> = safeApiCall {
        client.post("$baseUrl/conversations/$conversationId/typing") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("isTyping" to isTyping))
        }
        Unit
    }

    // ===== Real-time Subscriptions =====

    /**
     * Subscribe to real-time message events for a conversation
     * Uses PocketBase real-time subscriptions
     */
    fun subscribeToMessages(conversationId: String): Flow<MessageEvent> = flow {
        // Note: This uses PocketBase's realtime subscription API
        // The actual SSE connection is managed by PocketBaseClient
        
        // Subscribe to m_messages collection with filter for this conversation
        val subscription = client.get("$baseUrl/api/realtime/subscribe") {
            bearerAuth(tokenStorage.getToken() ?: "")
            parameter("collection", "m_messages")
            parameter("filter", "conversationId='$conversationId'")
        }
        
        // Parse SSE events and emit MessageEvents
        // Implementation depends on Ktor SSE plugin
        // For production, this would use kotlinx.coroutines.flow with SSE parsing
        
        // Placeholder: emit events from polling fallback
        while (true) {
            kotlinx.coroutines.delay(2000) // Poll every 2 seconds as fallback
            // In production, this would be replaced with true SSE streaming
        }
    }

    /**
     * Subscribe to typing indicators for a conversation
     */
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingEvent> = flow {
        // Subscribe to m_typing_indicators with filter for this conversation
        while (true) {
            kotlinx.coroutines.delay(500) // Faster polling for typing indicators
            // TODO: Replace with SSE subscription
        }
    }

    /**
     * Subscribe to conversation updates (new messages, unread counts, etc)
     */
    fun subscribeToConversations(): Flow<ConversationEvent> = flow {
        // Subscribe to m_conversations collection for current user
        while (true) {
            kotlinx.coroutines.delay(2000)
            // TODO: Replace with SSE subscription
        }
    }

    // ===== Helper Functions =====

    private suspend inline fun <reified T> safeApiCall(crossinline call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(love.bside.app.core.AppException.Network.ServerError(
                statusCode = 500,
                message = e.message ?: "Unknown error"
            ))
        }
    }
}

/**
 * Request models for messaging API
 */
@kotlinx.serialization.Serializable
data class SendMessageRequest(
    val content: String,
    val messageType: String = "text"
)

@kotlinx.serialization.Serializable
data class CreateConversationRequest(
    val otherUserId: String
)

@kotlinx.serialization.Serializable
data class TypingStatusRequest(
    val isTyping: Boolean
)

@kotlinx.serialization.Serializable
data class EditMessageRequest(
    val content: String
)

@kotlinx.serialization.Serializable
data class BatchReadRequest(
    val messageIds: List<String>
)
