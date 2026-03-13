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
import kotlinx.serialization.json.int
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import love.bside.app.data.models.MessageType
import love.bside.app.data.models.Attachment
import love.bside.app.data.models.Reaction
import love.bside.app.data.models.Presence
import love.bside.app.data.models.PresenceStatus
import love.bside.app.data.models.TypingStatus
import io.pocketbase.models.FileField
import io.pocketbase.models.QueryOptions
import kotlinx.serialization.json.JsonObject

class MessagingRepository(val pb: PocketBase) {

    suspend fun getConversations(): List<Conversation> {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: return emptyList()
        val result = pb.collection("m_conversation_participants").getList(
            QueryOptions(
                sort = "-updated",
                expand = "conversation_id",
                filter = "user_id = '$userId'"
            )
        )
        return result.items.mapNotNull { item ->
            val expand = item["expand"]?.jsonObject
            val conv = expand?.get("conversation_id")?.jsonObject
            conv?.toConversation(participants = listOf(userId)) 
        }
    }

    suspend fun getMessages(conversationId: String, page: Int = 1): List<Message> {
        val result = pb.collection("m_messages").getList(
            QueryOptions(
                page = page,
                sort = "-created",
                filter = "conversation_id = '$conversationId'",
                expand = "m_reactions(message_id),reply_to_message_id"
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
            put("type", if (attachments.isNotEmpty()) "image" else "text")
            replyToId?.let { put("reply_to_message_id", it) }
            threadRootId?.let { put("thread_root_id", it) }
            threadDepth?.let { put("thread_depth", it) }
            put("sent_at", kotlinx.datetime.Clock.System.now().toString())
        }.filterValues { it != null }

        val record = if (attachments.isNotEmpty()) {
            val files = attachments.map { 
                FileField(fieldName = "attachments", fileName = it.fileName, data = it.data)
            }
            pb.collection("m_messages").create(body, files)
        } else {
            pb.collection("m_messages").create(body)
        }
        
        return record.toMessage()
    }

    suspend fun createConversation(participants: List<String>, type: ConversationType, name: String? = null): Conversation {
         val convBody = mapOf(
            "type" to type.name.lowercase(),
            "name" to name
         )
         
        val convRecord = pb.collection("m_conversations").create(convBody)
        val convId = convRecord["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("Failed to create conversation")
        
        participants.distinct().forEach { uid ->
             val partBody = mapOf(
                 "conversation_id" to convId,
                 "user_id" to uid,
                 "role" to "member",
                 "joined_at" to kotlinx.datetime.Clock.System.now().toString()
             )
             pb.collection("m_conversation_participants").create(partBody)
         }
        
        return Conversation(
            id = convId,
            collectionId = convRecord["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = convRecord["collectionName"]?.jsonPrimitive?.content ?: "",
            type = type,
            participants = participants,
            name = name,
            lastMessageAt = convRecord["updated"]?.jsonPrimitive?.content ?: "",
            created = convRecord["created"]?.jsonPrimitive?.content ?: "",
            updated = convRecord["updated"]?.jsonPrimitive?.content ?: ""
        )
    }
    
    fun observeMessages(conversationId: String): Flow<Message> = callbackFlow {
        var unsubscribeFunc: (suspend () -> Unit)? = null
        
        val job = launch {
            try {
                unsubscribeFunc = pb.collection("m_messages").subscribe(callback = { event ->
                    if (event.action == io.pocketbase.models.RealtimeAction.create || 
                        event.action == io.pocketbase.models.RealtimeAction.update) {
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
            // cleanup
        }
    }

    fun subscribeToConversation(conversationId: String): Flow<Message> = observeMessages(conversationId)

    fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> = callbackFlow {
        var unsubscribeFunc: (suspend () -> Unit)? = null
        
        val job = launch {
            try {
                unsubscribeFunc = pb.collection("m_typing_status").subscribe(callback = { event ->
                    val record = event.record.jsonObject
                    val convId = record["conversation_id"]?.jsonPrimitive?.content
                    if (convId == conversationId) {
                        trySend(TypingStatus(
                            userId = record["user_id"]?.jsonPrimitive?.content ?: "",
                            conversationId = convId,
                            isTyping = record["is_typing"]?.jsonPrimitive?.boolean ?: false,
                            updated = record["updated"]?.jsonPrimitive?.content ?: ""
                        ))
                    }
                })
            } catch (e: Exception) {}
        }
        awaitClose { job.cancel() }
    }

    suspend fun setTypingStatus(conversationId: String, isTyping: Boolean) {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: return
        
        // Upsert logic
        val existing = try {
            pb.collection("m_typing_status").getList(
                QueryOptions(
                    page = 1,
                    perPage = 1,
                    filter = "user_id = '$userId' && conversation_id = '$conversationId'"
                )
            ).items.firstOrNull()
        } catch (e: Exception) { null }

        val body = mapOf(
            "user_id" to userId,
            "conversation_id" to conversationId,
            "is_typing" to isTyping
        )

        if (existing != null) {
            val id = existing["id"]?.jsonPrimitive?.content ?: return
            pb.collection("m_typing_status").update(id, body)
        } else {
            pb.collection("m_typing_status").create(body)
        }
    }

    suspend fun markAsRead(messageId: String) {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: return
        
        val body = mapOf(
            "message_id" to messageId,
            "user_id" to userId,
            "read_at" to kotlinx.datetime.Clock.System.now().toString()
        )
        
        // We create a read receipt record. 
        // In a full implementation, we might also update the message is_read field or unread_count in participant record.
        try {
            pb.collection("m_read_receipts").create(body)
        } catch (e: Exception) {
            // Probably already exists or error, ignore
        }
    }

    suspend fun addReaction(messageId: String, reaction: String): Reaction {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: throw IllegalStateException("User not logged in")
        
        val body = mapOf(
            "message_id" to messageId,
            "user_id" to userId,
            "reaction" to reaction
        )
        
        val record = pb.collection("m_reactions").create(body)
        return record.jsonObject.toReaction()
    }

    suspend fun removeReaction(messageId: String, reaction: String) {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: throw IllegalStateException("User not logged in")
        
        val records = pb.collection("m_reactions").getList(
            QueryOptions(
                page = 1,
                perPage = 1,
                filter = "message_id = '$messageId' && user_id = '$userId' && reaction = '$reaction'"
            )
        )
        
        if (records.items.isNotEmpty()) {
            val recordId = records.items[0]["id"]?.jsonPrimitive?.content ?: return
            pb.collection("m_reactions").delete(recordId)
        }
    }

    suspend fun setPresence(status: PresenceStatus, activityMessage: String? = null): Presence {
        val userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: throw IllegalStateException("User not logged in")
        
        val body = buildMap<String, Any?> {
            put("user_id", userId)
            put("status", status.name.lowercase())
            put("last_active", kotlinx.datetime.Clock.System.now().toString())
            activityMessage?.let { put("activity_message", it) }
        }.filterValues { it != null }

        val existing = try {
            pb.collection("m_presence").getList(
                QueryOptions(
                    page = 1,
                    perPage = 1,
                    filter = "user_id = '$userId'"
                )
            ).items.firstOrNull()
        } catch (e: Exception) { null }

        val record = if (existing != null) {
            val existingId = existing["id"]?.jsonPrimitive?.content ?: throw IllegalStateException("Existing record has no ID")
            pb.collection("m_presence").update(existingId, body)
        } else {
            pb.collection("m_presence").create(body)
        }
        
        return record.jsonObject.toPresence()
    }

    suspend fun getPresence(userId: String): Presence? {
        val records = pb.collection("m_presence").getList(
            QueryOptions(
                page = 1,
                perPage = 1,
                filter = "user_id = '$userId'"
            )
        )
        return records.items.firstOrNull()?.jsonObject?.toPresence()
    }

    private fun JsonObject.toReaction(): Reaction {
        return Reaction(
            id = this["id"]?.jsonPrimitive?.content ?: "",
            collectionId = this["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = this["collectionName"]?.jsonPrimitive?.content ?: "",
            created = this["created"]?.jsonPrimitive?.content ?: "",
            updated = this["updated"]?.jsonPrimitive?.content ?: "",
            reaction = this["reaction"]?.jsonPrimitive?.content ?: "",
            messageId = this["message_id"]?.jsonPrimitive?.content ?: "",
            userId = this["user_id"]?.jsonPrimitive?.content ?: ""
        )
    }

    private fun JsonObject.toPresence(): Presence {
        return Presence(
            id = this["id"]?.jsonPrimitive?.content ?: "",
            collectionId = this["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = this["collectionName"]?.jsonPrimitive?.content ?: "",
            created = this["created"]?.jsonPrimitive?.content ?: "",
            updated = this["updated"]?.jsonPrimitive?.content ?: "",
            userId = this["user_id"]?.jsonPrimitive?.content ?: "",
            status = try {
                PresenceStatus.valueOf((this["status"]?.jsonPrimitive?.content ?: "OFFLINE").uppercase())
            } catch (e: Exception) { PresenceStatus.OFFLINE },
            activityMessage = this["activity_message"]?.jsonPrimitive?.content?.ifEmpty { null },
            lastActive = this["last_active"]?.jsonPrimitive?.content ?: ""
        )
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
                    (this["type"]?.jsonPrimitive?.content ?: "TEXT").uppercase()
                ) 
            } catch (e: Exception) { MessageType.TEXT },
            replyToMessageId = this["reply_to_message_id"]?.jsonPrimitive?.content?.ifEmpty { null },
            threadRootId = this["thread_root_id"]?.jsonPrimitive?.content?.ifEmpty { null },
            threadDepth = this["thread_depth"]?.jsonPrimitive?.int ?: 0,
            attachments = this["attachments"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
            editedAt = this["edited_at"]?.jsonPrimitive?.content?.ifEmpty { null },
            deletedAt = this["deleted_at"]?.jsonPrimitive?.content?.ifEmpty { null },
            reactions = (this["expand"]?.jsonObject?.get("m_reactions(message_id)")?.jsonArray?.map { 
                it.jsonObject.toReaction() 
            } ?: emptyList()),
            replyToMessage = this["expand"]?.jsonObject?.get("reply_to_message_id")?.jsonObject?.toMessage()
        )
    }

    private fun JsonObject.toConversation(participants: List<String> = emptyList()): Conversation {
        return Conversation(
            id = this["id"]?.jsonPrimitive?.content ?: "",
            collectionId = this["collectionId"]?.jsonPrimitive?.content ?: "",
            collectionName = this["collectionName"]?.jsonPrimitive?.content ?: "",
            type = try {
                 ConversationType.valueOf((this["type"]?.jsonPrimitive?.content ?: "DIRECT").uppercase())
            } catch (e: Exception) { ConversationType.DIRECT },
            participants = if (this.containsKey("participants")) {
                this["participants"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            } else {
                participants
            },
            name = this["name"]?.jsonPrimitive?.content?.ifEmpty { null },
            lastMessageAt = this["updated"]?.jsonPrimitive?.content ?: "",
            created = this["created"]?.jsonPrimitive?.content ?: "",
            updated = this["updated"]?.jsonPrimitive?.content ?: ""
        )
    }
}