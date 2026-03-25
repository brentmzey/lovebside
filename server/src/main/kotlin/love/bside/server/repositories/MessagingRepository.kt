package love.bside.server.repositories

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.data.models.messaging.*
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.db.PBMessage
import love.bside.server.utils.toDomain
import love.bside.app.utils.CompressionService
import arrow.core.toOption
import arrow.core.getOrElse
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import java.util.UUID

/**
 * Repository for messaging operations
 * Handles conversations, messages, typing indicators, and read receipts
 */
class MessagingRepository(
    private val client: PocketBaseClient
) {
    
    companion object {
        private const val CONVERSATIONS_COLLECTION = "m_conversations"
        private const val MESSAGES_COLLECTION = "m_messages"
    }
    
    // ===== Messages =====
    
    suspend fun getMessages(
        conversationId: String,
        page: Int,
        perPage: Int
    ): love.bside.app.core.Result<love.bside.app.data.repository.Page<Message>> {
        val filter = "conversation = '$conversationId'"
        val sort = "-created"
        
        return when (val result = client.getList<PBMessage>(MESSAGES_COLLECTION, page, perPage, filter, sort)) {
            is Result.Success -> {
                val items = result.data.items.map { it.toDomain() }
                Result.Success(love.bside.app.data.repository.Page(
                    items = items,
                    page = result.data.page,
                    pageSize = result.data.perPage,
                    totalItems = result.data.totalItems,
                    totalPages = result.data.totalPages,
                    hasNext = result.data.page < result.data.totalPages,
                    hasPrevious = result.data.page > 1
                ))
            }
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    suspend fun createMessage(
        conversationId: String,
        senderId: String,
        receiverId: String,
        content: arrow.core.Option<String>,
        messageType: MessageType,
        sentAt: String
    ): Result<Message> {
        val body = buildJsonObject {
            put("conversation", conversationId)
            put("sender", senderId)
            put("content", content.getOrElse { "" })
            // Extreme compression
            val compressed = CompressionService.compressToBase64(content)
            if (compressed.isSome()) {
                put("contentBrotliBase64", (compressed as arrow.core.Some).value)
            }
            put("type", messageType.name.lowercase())
            put("status", "sent")
        }
        
        return when (val result = client.create<JsonObject, PBMessage>(MESSAGES_COLLECTION, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    suspend fun getMessageById(messageId: String): Result<Message> {
        return when (val result = client.getOne<PBMessage>(MESSAGES_COLLECTION, messageId)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
}
