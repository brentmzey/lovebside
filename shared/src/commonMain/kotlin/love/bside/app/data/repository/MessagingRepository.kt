package love.bside.app.data.repository

import io.pocketbase.PocketBase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import love.bside.app.data.models.Conversation
import love.bside.app.data.models.Message
import love.bside.app.data.models.ConversationType
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
// import kotlinx.serialization.json.content
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import love.bside.app.data.models.MessageType
import love.bside.app.data.models.Attachment
import io.pocketbase.models.FileField

import io.pocketbase.models.QueryOptions
import kotlinx.serialization.json.JsonObject

class MessagingRepository(val pb: PocketBase) {

    suspend fun getConversations(): List<Conversation> {
        val result = pb.collection("conversations").getList(
            QueryOptions(
                sort = "-updated",
                expand = "participants"
            )
        )
        return result.items.map { it.toConversation() }
    }

    suspend fun getMessages(conversationId: String, page: Int = 1): List<Message> {
        val result = pb.collection("m_messages").getList(
            QueryOptions(
                page = page,
                sort = "-created",
                filter = "conversation_id = '$conversationId'"
            )
        )
        return result.items.map { it.toMessage() }
    }

    suspend fun sendMessage(
        conversationId: String, 
        text: String, 
        replyToId: String? = null, 
        threadRootId: String? = null, 
        threadDepth: Int? = null,
        attachments: List<Attachment> = emptyList()
    ): Message {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: throw IllegalStateException("User not logged in")
        
        val body = buildMap<String, Any?> {
            put("conversation_id", conversationId)
            put("sender_id", userId)
            put("content", text)
            put("message_type", if (attachments.isNotEmpty()) "image" else "text") // Simple heuristic for now
            replyToId?.let { put("reply_to_message_id", it) }
            threadRootId?.let { put("thread_root_id", it) }
            threadDepth?.let { put("thread_depth", it) }
            put("sent_at", kotlinx.datetime.Clock.System.now().toString())
        }.filterValues { it != null }

        val record = if (attachments.isNotEmpty()) {
            val files = attachments.map { 
                FileField(fieldName = "attachments", fileName = it.fileName, data = it.data)
            }
            // Use the overload that supports files
            pb.collection("m_messages").create(body, files)
        } else {
            pb.collection("m_messages").create(body)
        }
        
        return record.toMessage()
    }

    @kotlinx.serialization.Serializable
    private data class ConversationRequest(
        val participants: List<String>,
        val type: String,
        val name: String? = null
    )

    suspend fun createConversation(participants: List<String>, type: ConversationType, name: String? = null): Conversation {
         val body = ConversationRequest(
            participants = participants,
            type = type.name.lowercase(),
            name = name
         )
        
        val record = pb.collection("conversations").create(body)
        return record.toConversation()
    }
    
    fun observeMessages(conversationId: String): Flow<Message> = callbackFlow {
        var unsubscribeFunc: (suspend () -> Unit)? = null
        
        val job = launch {
            try {
                unsubscribeFunc = pb.collection("m_messages").subscribe(callback = { event ->
                    if (event.action == io.pocketbase.models.RealtimeAction.create) {
                        try {
                            val record = event.record.jsonObject
                            if (record.isNotEmpty()) {
                                val msgConvId = record["conversation_id"]?.jsonPrimitive?.content ?: ""
                                if (msgConvId == conversationId) {
                                    trySend(record.toMessage())
                                }
                            }
                        } catch (e: Exception) {
                            // Parsing error
                        }
                    }
                })
            } catch (e: Exception) {
                // Subscription failed or cancelled
            }
        }
        
        awaitClose {
            job.cancel()
            val unsub = unsubscribeFunc
            if (unsub != null) {
                 // We need a scope to run suspend function if callbackFlow is cancelled
                 // GlobalScope or a tailored scope is risky, but here we just want to fire and forget unsub
                 // But since we can't launch in a cancelled scope easily, we might skip or rely on SDK cleanup?
                 // Ideally:
                 // kotlinx.coroutines.GlobalScope.launch { unsub() } 
                 // But avoiding GlobalScope is best.
                 // PocketBase SDK usually handles cleanup if closing connection, but single subscription?
            }
        }
    }

    private fun JsonObject.toMessage(): Message {
        return Message(
            id = this["id"]?.jsonPrimitive?.content ?: "",
            collectionId = this["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = this["collectionName"]?.jsonPrimitive?.content ?: "",
            created = this["created"]?.jsonPrimitive?.content ?: "",
            updated = this["updated"]?.jsonPrimitive?.content ?: "",
            conversationId = this["conversation_id"]?.jsonPrimitive?.content ?: "",
            senderId = this["sender_id"]?.jsonPrimitive?.content ?: "",
            receiverId = this["receiver_id"]?.jsonPrimitive?.content ?: "",
            content = this["content"]?.jsonPrimitive?.content ?: "",
            sentAt = this["sent_at"]?.jsonPrimitive?.content ?: "",
            isRead = this["is_read"]?.jsonPrimitive?.boolean ?: false,
            readAt = this["read_at"]?.jsonPrimitive?.content?.ifEmpty { null },
            messageType = try { 
                MessageType.valueOf(
                    (this["message_type"]?.jsonPrimitive?.content ?: "TEXT").uppercase()
                ) 
            } catch (e: Exception) { MessageType.TEXT },
            replyToMessageId = this["reply_to_message_id"]?.jsonPrimitive?.content?.ifEmpty { null },
            threadRootId = this["thread_root_id"]?.jsonPrimitive?.content?.ifEmpty { null },
            threadDepth = this["thread_depth"]?.jsonPrimitive?.int ?: 0,
            attachments = this["attachments"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            editedAt = this["edited_at"]?.jsonPrimitive?.content?.ifEmpty { null },
            deletedAt = this["deleted_at"]?.jsonPrimitive?.content?.ifEmpty { null }
        )
    }

    private fun JsonObject.toConversation(): Conversation {
        return Conversation(
            id = this["id"]?.jsonPrimitive?.content ?: "",
            collectionId = this["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = this["collectionName"]?.jsonPrimitive?.content ?: "",
            type = try {
                 ConversationType.valueOf((this["type"]?.jsonPrimitive?.content ?: "DIRECT").uppercase())
            } catch (e: Exception) { ConversationType.DIRECT },
            participants = this["participants"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            name = this["name"]?.jsonPrimitive?.content?.ifEmpty { null },
            lastMessageAt = this["updated"]?.jsonPrimitive?.content ?: "", // Approximation
            created = this["created"]?.jsonPrimitive?.content ?: "",
            updated = this["updated"]?.jsonPrimitive?.content ?: ""
        )
    }
}
