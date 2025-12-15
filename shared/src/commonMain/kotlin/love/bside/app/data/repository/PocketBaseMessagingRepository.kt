package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.functional.getListTyped
import io.pocketbase.functional.getOneTyped
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel
import io.pocketbase.models.ListResult
import love.bside.app.data.repository.RealtimeServiceImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.*
import love.bside.app.domain.repository.MessagingRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.encodeToJsonElement

@Serializable
private data class ParticipantCreateRequest(
    val conversationId: String,
    val userId: String,
    val role: String,
    @kotlinx.serialization.SerialName("unread_count")
    val unreadCount: Int,
    val joinedAt: String,
    val isMuted: Boolean,
    val isPinned: Boolean
)

class PocketBaseMessagingRepository(
    private val pocketBase: PocketBase
) : MessagingRepository {
    private val realtimeService = RealtimeServiceImpl(pocketBase, this)

    // ============================= Conversations =============================
    override suspend fun getConversations(userId: String): Result<List<Conversation>> = runCatching {
        val participants = pocketBase.collection("m_conversation_participants")
            .getList(
                QueryOptions(
                    filter = "userId='$userId' && leftAt=null",
                    expand = "conversationId",
                    sort = "-updated"
                )
            )
        
        participants.items.mapNotNull { 
            val expand = it["expand"]?.jsonObject
            val conversationRecord = expand?.get("conversationId")?.jsonObject
            if (conversationRecord != null) mapRecordToConversation(conversationRecord) else null
        }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch conversations")) }
    )

    override suspend fun getConversation(conversationId: String): Result<Conversation> = runCatching {
        mapRecordToConversation(pocketBase.collection("m_conversations").getOne(conversationId))
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Conversation not found")) }
    )

    @kotlinx.serialization.Serializable
    private data class CreateConversationRequest(
        val conversationType: String,
        val totalMessageCount: Int,
        val maxParticipants: Int,
        val isArchived: Boolean,
        val participants: List<String> = emptyList(),
        val conversationName: String? = null,
        val lastMessageText: String? = null,
        val lastMessageAt: String? = null
    )

    @kotlinx.serialization.Serializable
    private data class CreateParticipantRequest(
        val conversationId: String,
        val userId: String,
        val role: String,
        val unreadCount: String,
        val joinedAt: String,
        val isMuted: Boolean,
        val isPinned: Boolean
    )

    @kotlinx.serialization.Serializable
    private data class CreateMessageRequest(
        val conversationId: String,
        val senderId: String,
        val content: String,
        val messageType: String,
        val sentAt: String,
        val readByCount: Int,
        val replyToMessageId: String? = null
    )

    override suspend fun createDirectConversation(participantIds: List<String>): Result<Conversation> {
        require(participantIds.size == 2) { "Direct conversations require exactly 2 participants" }
        return runCatching {
            val convBody = CreateConversationRequest(
                conversationType = "direct",
                totalMessageCount = 0,
                maxParticipants = 2,
                isArchived = false
            )
            // Use pocketBase.send directly to bypass RecordService restriction on Map<String, Any>
            // This allows passing DTOs which are properly serialized by Ktor
            val convJson = pocketBase.send<kotlinx.serialization.json.JsonObject>(
                path = "/api/collections/m_conversations/records",
                method = "POST",
                body = convBody
            )
            val convId = convJson["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to parse conversation ID")

            val now = Clock.System.now().toString()
            participantIds.forEach { userId ->
                val partBody = ParticipantCreateRequest(
                    conversationId = convId,
                    userId = userId,
                    role = "member",
                    unreadCount = 0,
                    joinedAt = now,
                    isMuted = false,
                    isPinned = false
                )
                pocketBase.send<io.pocketbase.models.RecordModel>(
                    path = "/api/collections/m_conversation_participants/records",
                    method = "POST",
                    body = partBody
                )
            }
            mapRecordToConversation(convJson)
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to create conversation", it)) }
        )

    }

    // ============================= Participants =============================
    override suspend fun getParticipants(conversationId: String): Result<List<ConversationParticipant>> = runCatching {
        pocketBase.collection("m_conversation_participants")
            .getList(
                QueryOptions(
                    filter = "conversationId='$conversationId' && leftAt=null",
                    sort = "joinedAt"
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToParticipant(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch participants", it)) }
    )

    override suspend fun addParticipants(conversationId: String, userIds: List<String>): Result<Unit> = runCatching {
        val now = Clock.System.now().toString()
        userIds.forEach { userId ->
            val body = ParticipantCreateRequest(
                conversationId = conversationId,
                userId = userId,
                role = "member",
                unreadCount = 0,
                joinedAt = now,
                isMuted = false,
                isPinned = false
            )

            pocketBase.send<io.pocketbase.models.RecordModel>(
                path = "/api/collections/m_conversation_participants/records",
                method = "POST",
                body = body
            )
        }
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to add participants", it)) }
    )

    // ============================= Messages =============================
    override suspend fun getMessages(conversationId: String, limit: Int): Result<List<Message>> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversationId='$conversationId' && deletedAt=null",
                    sort = "-sentAt",
                    perPage = limit
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages", it)) }
    )

    override suspend fun sendMessage(conversationId: String, content: String, replyToMessageId: String?): Result<Message> = runCatching {
        val model = pocketBase.authStore.model
        val userId = (model as? RecordModel)?.id 
            ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            ?: return Result.Error(AppException.Unknown("Not authenticated"))
        val now = Clock.System.now().toString()
        
        val body = CreateMessageRequest(
            conversationId = conversationId,
            senderId = userId,
            content = content,
            messageType = "text",
            sentAt = now,
            readByCount = 0,
            replyToMessageId = replyToMessageId
        )
        
        val created = pocketBase.send<kotlinx.serialization.json.JsonObject>(
            path = "/api/collections/m_messages/records",
            method = "POST",
            body = body
        )
        val createdId = created["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to get message ID")
        
        // update conversation last message fields - using Map<String, String> is fine here as it's not mixed
        val updateBody = mapOf(
            "lastMessageText" to content.take(100),
            "lastMessageAt" to now
        )
        pocketBase.collection("m_conversations").update(conversationId, updateBody)


        // fetch full message record and map
        val msgRecord = pocketBase.collection("m_messages").getOne(createdId)
        mapRecordToMessage(msgRecord)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to send message", it)) }
    )

    // ============================= Threading =============================
    override suspend fun getReplies(messageId: String): Result<List<Message>> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "replyToMessageId='$messageId' && deletedAt=null",
                    sort = "sentAt"
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch replies")) }
    )

    override suspend fun getThreadRoot(messageId: String): Result<Message> = runCatching {
        var current = mapRecordToMessage(pocketBase.collection("m_messages").getOne(messageId))
        while (current.replyToMessageId != null) {
            current = mapRecordToMessage(pocketBase.collection("m_messages").getOne(current.replyToMessageId!!))
        }
        current
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to get thread root")) }
    )

    override suspend fun getFullThread(rootMessageId: String): Result<List<Message>> = runCatching {
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        val result = mutableListOf<Message>()
        queue.add(rootMessageId)
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (!visited.add(id)) continue
            // fetch full message record and map
            val msg = mapRecordToMessage(pocketBase.collection("m_messages").getOne(id))
            result.add(msg)
            val replies = getReplies(id)
            if (replies is Result.Success) {
                queue.addAll(replies.data.map { it.id })
            }
        }
        result.sortedBy { it.sentAt }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch full thread")) }
    )

    override suspend fun countReplies(messageId: String): Result<Int> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "replyToMessageId='$messageId' && deletedAt=null",
                    perPage = 1
                )
            ).totalItems
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to count replies")) }
    )

    // ============================= Advanced Queries =============================
    override suspend fun searchMessages(query: String, conversationId: String): Result<List<Message>> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversationId='$conversationId' && content~'$query' && deletedAt=null",
                    sort = "-sentAt"
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Search failed")) }
    )

    override suspend fun getMessagesAfter(conversationId: String, timestamp: Instant, limit: Int): Result<List<Message>> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversationId='$conversationId' && sentAt>'$timestamp' && deletedAt=null",
                    sort = "sentAt",
                    perPage = limit
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages after")) }
    )

    override suspend fun getMessagesBefore(conversationId: String, timestamp: Instant, limit: Int): Result<List<Message>> = runCatching {
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversationId='$conversationId' && sentAt<'$timestamp' && deletedAt=null",
                    sort = "-sentAt",
                    perPage = limit
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages before")) }
    )

    override suspend fun createGroupConversation(name: String, participantIds: List<String>): Result<Conversation> {
        return Result.Error(AppException.Unknown("Not implemented yet"))
    }

    override suspend fun removeParticipant(conversationId: String, userId: String): Result<Unit> {
         return Result.Error(AppException.Unknown("Not implemented yet"))
    }

    override suspend fun updateParticipantSettings(conversationId: String, isMuted: Boolean?, isPinned: Boolean?): Result<Unit> {
         return Result.Error(AppException.Unknown("Not implemented yet"))
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> = runCatching {
        val model = pocketBase.authStore.model
        val userId = (model as? RecordModel)?.id 
            ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            ?: return Result.Error(AppException.Unknown("Not authenticated"))

        // Find participant record for this user
        val records = pocketBase.collection("m_conversation_participants").getList(
            QueryOptions(
                 filter = "conversationId='$conversationId' && userId='$userId'"
            )
        )
        
        if (records.items.isNotEmpty()) {
             val pId = (records.items[0] as RecordModel).id
             // Update lastReadAt and unreadCount
             val updateBody = mapOf(
                 "lastReadAt" to Clock.System.now().toString(),
                 "unreadCount" to 0
             )
             pocketBase.collection("m_conversation_participants").update(pId, updateBody)
        }
        Unit
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to mark as read", it)) }
    )

    // ============================= Real‑time placeholders =============================
    override fun subscribeToConversation(conversationId: String): Flow<Message> = realtimeService.subscribeToConversation(conversationId)
    override fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> = realtimeService.subscribeToTypingIndicators(conversationId)
    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> = try {
        realtimeService.setTypingStatus(conversationId, isTyping)
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(AppException.Unknown(e.message ?: "Failed to set typing status"))
    }

    // ============================= Mappers =============================
    fun mapRecordToConversation(recordElement: kotlinx.serialization.json.JsonElement): Conversation {
        val record = recordElement.jsonObject
        val createdStr = record["created"]?.jsonPrimitive?.content ?: ""
        val updatedStr = record["updated"]?.jsonPrimitive?.content ?: ""

        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getInt(key: String): Int = record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        fun getBoolean(key: String): Boolean = record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

        return Conversation(
            id = getString("id"),
            conversationType = when (getString("conversationType")) {
                "direct" -> ConversationType.DIRECT
                "group" -> ConversationType.GROUP
                "channel" -> ConversationType.CHANNEL
                else -> ConversationType.DIRECT
            },
            conversationName = getString("conversationName"),
            conversationAvatar = getString("conversationAvatar"),
            lastMessageText = getString("lastMessageText"),
            lastMessageAt = getString("lastMessageAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } },
            totalMessageCount = getInt("totalMessageCount"),
            maxParticipants = getInt("maxParticipants"),
            isArchived = getBoolean("isArchived"),
            created = if (createdStr.isNotEmpty()) try { Instant.parse(createdStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now(),
            updated = if (updatedStr.isNotEmpty()) try { Instant.parse(updatedStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now()
        )
    }

    fun mapRecordToParticipant(recordElement: kotlinx.serialization.json.JsonElement): ConversationParticipant {
        val record = recordElement.jsonObject
        val createdStr = record["created"]?.jsonPrimitive?.content ?: ""
        val updatedStr = record["updated"]?.jsonPrimitive?.content ?: ""

        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getInt(key: String): Int = record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        fun getBoolean(key: String): Boolean = record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

        return ConversationParticipant(
            id = getString("id"),
            conversationId = getString("conversationId"),
            userId = getString("userId"),
            role = when (getString("role")) {
                "admin" -> ParticipantRole.ADMIN
                "readonly" -> ParticipantRole.READONLY
                else -> ParticipantRole.MEMBER
            },
            unreadCount = getInt("unreadCount"),
            lastReadAt = getString("lastReadAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } },
            joinedAt = getString("joinedAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } } ?: Clock.System.now(),
            leftAt = getString("leftAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } },
            isMuted = getBoolean("isMuted"),
            isPinned = getBoolean("isPinned"),
            created = if (createdStr.isNotEmpty()) try { Instant.parse(createdStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now(),
            updated = if (updatedStr.isNotEmpty()) try { Instant.parse(updatedStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now()
        )
    }

    fun mapRecordToMessage(recordElement: kotlinx.serialization.json.JsonElement): Message {
        val record = recordElement.jsonObject
        val createdStr = record["created"]?.jsonPrimitive?.content ?: ""
        val updatedStr = record["updated"]?.jsonPrimitive?.content ?: ""
        
        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getInt(key: String): Int = record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        fun getList(key: String): List<String> = record[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        return Message(
            id = getString("id"),
            conversationId = getString("conversationId"),
            senderId = getString("senderId"),
            content = getString("content"),
            messageType = when (getString("messageType")) {
                "image" -> MessageType.IMAGE
                "file" -> MessageType.FILE
                "system" -> MessageType.SYSTEM
                else -> MessageType.TEXT
            },
            attachments = getList("attachments"),
            sentAt = try { Instant.parse(getString("sentAt")) } catch (e: Exception) { Clock.System.now() },
            editedAt = getString("editedAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } },
            deletedAt = getString("deletedAt").takeIf { it.isNotEmpty() }?.let { try { Instant.parse(it) } catch (e: Exception) { null } },
            readByCount = getInt("readByCount"),
            replyToMessageId = getString("replyToMessageId").takeIf { it.isNotEmpty() },
            threadRootId = getString("threadRootId").takeIf { it.isNotEmpty() },
            threadDepth = getInt("threadDepth"),
            threadReplyCount = getInt("threadReplyCount"),
            created = if (createdStr.isNotEmpty()) try { Instant.parse(createdStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now(),
            updated = if (updatedStr.isNotEmpty()) try { Instant.parse(updatedStr) } catch (e: Exception) { Clock.System.now() } else Clock.System.now()
        )
    }

    

}

// Extension to avoid 'booleanOrNull' import issues if missing, checking primitives
private val kotlinx.serialization.json.JsonPrimitive.booleanOrNull: Boolean? get() = try { if (content == "true") true else if (content == "false") false else null } catch(e: Exception) { null }
private val kotlinx.serialization.json.JsonPrimitive.longOrNull: Long? get() = content.toLongOrNull()
private val kotlinx.serialization.json.JsonPrimitive.doubleOrNull: Double? get() = content.toDoubleOrNull()

    private fun io.pocketbase.types.Message.toDomain(): Message = Message(
        id = this.id,
        conversationId = this.conversationId,
        senderId = this.senderId,
        content = this.content,
        messageType = when (this.messageType) {
            "image" -> MessageType.IMAGE
            "file" -> MessageType.FILE
            "system" -> MessageType.SYSTEM
            else -> MessageType.TEXT
        },
        attachments = this.attachments,
        sentAt = this.sentAt,
        editedAt = this.editedAt,
        deletedAt = this.deletedAt,
        readByCount = this.readByCount,
        replyToMessageId = this.replyToMessageId,
        threadRootId = this.threadRootId,
        threadDepth = this.threadDepth,
        threadReplyCount = this.threadReplyCount,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated)
    )

