package love.bside.app.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import love.bside.app.data.models.messaging.*
import love.bside.app.data.storage.TokenStorage

/**
 * PocketBase Real-time Client
 * 
 * TODO: Implement true SSE real-time subscriptions when Ktor SSE plugin is available
 * For now, using smart polling with short intervals to simulate real-time updates
 */
class PocketBaseRealtimeClient(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage,
    private val baseUrl: String
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Subscribe to real-time updates for messages in a conversation
     * Currently uses polling, will be upgraded to SSE
     */
    fun subscribeToMessages(conversationId: String): Flow<MessageEvent> = flow {
        var lastCreated = ""
        
        while (true) {
            try {
                val response = client.get("$baseUrl/api/collections/m_messages/records") {
                    bearerAuth(tokenStorage.getToken() ?: "")
                    parameter("filter", "conversationId='$conversationId'" + 
                        if (lastCreated.isNotEmpty()) " && created>'$lastCreated'" else "")
                    parameter("sort", "-created")
                    parameter("perPage", 20)
                }.body<PaginatedResponse<Message>>()
                
                response.items.forEach { message ->
                    emit(MessageEvent.NewMessage(message))
                    if (message.created > lastCreated) {
                        lastCreated = message.created
                    }
                }
            } catch (e: Exception) {
                println("❌ Message polling error: ${e.message}")
            }
            
            kotlinx.coroutines.delay(1000) // Poll every second for near real-time
        }
    }

    /**
     * Subscribe to typing indicators
     * Currently uses polling, will be upgraded to SSE
     */
    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingEvent> = flow {
        while (true) {
            try {
                val response = client.get("$baseUrl/api/collections/m_typing_indicators/records") {
                    bearerAuth(tokenStorage.getToken() ?: "")
                    parameter("filter", "conversationId='$conversationId' && isTyping=true")
                    parameter("sort", "-updated")
                    parameter("perPage", 10)
                }.body<PaginatedResponse<TypingIndicator>>()
                
                response.items.forEach { indicator ->
                    emit(TypingEvent.UserTyping(indicator.userId, indicator.isTyping))
                }
            } catch (e: Exception) {
                println("❌ Typing indicator polling error: ${e.message}")
            }
            
            kotlinx.coroutines.delay(500) // Poll every 500ms for typing indicators
        }
    }

    /**
     * Subscribe to conversation updates
     * Currently uses polling, will be upgraded to SSE
     */
    fun subscribeToConversations(userId: String): Flow<ConversationEvent> = flow {
        var lastUpdated = ""
        
        while (true) {
            try {
                val response = client.get("$baseUrl/api/collections/m_conversations/records") {
                    bearerAuth(tokenStorage.getToken() ?: "")
                    parameter("filter", "(participant1Id='$userId' || participant2Id='$userId')" +
                        if (lastUpdated.isNotEmpty()) " && updated>'$lastUpdated'" else "")
                    parameter("sort", "-updated")
                    parameter("perPage", 50)
                }.body<PaginatedResponse<Conversation>>()
                
                response.items.forEach { conversation ->
                    emit(ConversationEvent.Updated(conversation))
                    if (conversation.updated > lastUpdated) {
                        lastUpdated = conversation.updated
                    }
                }
            } catch (e: Exception) {
                println("❌ Conversation polling error: ${e.message}")
            }
            
            kotlinx.coroutines.delay(2000) // Poll every 2 seconds for conversations
        }
    }
}

@Serializable
private data class PaginatedResponse<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)
