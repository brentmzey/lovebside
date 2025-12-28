package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.models.ConversationParticipant
import love.bside.app.domain.models.ConversationType
import love.bside.app.domain.models.Match
import love.bside.app.domain.models.MatchExpand
import love.bside.app.domain.models.MatchStatus
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.MessageType
import love.bside.app.domain.models.MessagingSettings
import love.bside.app.domain.models.ParticipantRole
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.models.TypingStatus
import love.bside.app.domain.models.UserAnswer
import love.bside.app.domain.repository.MessagingRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import love.bside.app.data.remote.RateLimiter
import love.bside.app.utils.parsePocketBaseInstant
import love.bside.app.utils.parsePocketBaseInstantOr
import io.pocketbase.functional.getListTyped
import io.pocketbase.types.Questionnaire as SdkQuestionnaire
import io.pocketbase.types.UserAnswer as SdkUserAnswer
import kotlin.time.Duration.Companion.seconds

@Serializable
private data class ParticipantCreateRequest(
    @kotlinx.serialization.SerialName("conversation_id")
    val conversationId: String,
    @kotlinx.serialization.SerialName("user_id")
    val userId: String,
    val role: String,
    @kotlinx.serialization.SerialName("unread_count")
    val unreadCount: Int,
    @kotlinx.serialization.SerialName("joined_at")
    val joinedAt: String,
    @kotlinx.serialization.SerialName("is_muted")
    val isMuted: Boolean,
    @kotlinx.serialization.SerialName("is_pinned")
    val isPinned: Boolean
)

class PocketBaseMessagingRepository(
    private val pocketBase: PocketBase,
    realtimeService: RealtimeService? = null,
    private val offlineCache: love.bside.app.data.cache.OfflineCacheManager? = null,
    private val networkMonitor: love.bside.app.core.NetworkMonitor? = null
) : MessagingRepository {
    private val realtimeService: RealtimeService = realtimeService ?: RealtimeServiceImpl(pocketBase, this)
    // "Smart Queue" to serialize writes (Single Writer for SQLite)
    private val writeMutex = Mutex()
    
    // Rate Limiter: 60 requests per minute (1 per second) to stay safe from 429s
    private val rateLimiter = RateLimiter(maxRequests = 60, timeWindow = 60.seconds, burstCapacity = 5)
    
    init {
        // Monitor network state and update cache when online
        networkMonitor?.let { monitor ->
            offlineCache?.let { cache ->
                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    monitor.isOnline.collect { isOnline ->
                        cache.setOnlineStatus(isOnline)
                    }
                }
            }
        }
    }

    // ============================= Conversations =============================
    override suspend fun getConversations(userId: String): Result<List<Conversation>> {
        // Try cache first if offline
        if (networkMonitor?.checkConnectivity() == false) {
            offlineCache?.getCachedConversations(userId)?.let { cached ->
                return Result.Success(cached)
            }
        }
        
        return runCatching {
            //rateLimiter.acquireToken()
            val participants = pocketBase.collection("m_conversation_participants")
                .getList(
                    QueryOptions(
                        filter = "user_id='$userId'", // Removed left_at check (hard delete handling)
                        expand = "conversation_id",
                        sort = "-updated"
                    )
                )
            
            participants.items.mapNotNull { 
                val expand = it["expand"]?.jsonObject
                val conversationRecord = expand?.get("conversation_id")?.jsonObject
                if (conversationRecord != null) mapRecordToConversation(conversationRecord) else null
            }.also { conversations ->
                // Cache for offline use
                offlineCache?.cacheConversations(userId, conversations)
            }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { 
                // Return cached on error
                offlineCache?.getCachedConversations(userId)?.let { cached ->
                    return@fold Result.Success(cached)
                }
                Result.Error(AppException.Unknown(it.message ?: "Failed to fetch conversations"))
            }
        )
    }

    override suspend fun getConversation(conversationId: String): Result<Conversation> = runCatching {
        rateLimiter.acquireToken()
        mapRecordToConversation(pocketBase.collection("m_conversations").getOne(conversationId))
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Conversation not found")) }
    )

    @kotlinx.serialization.Serializable
    private data class CreateConversationRequest(
        val type: String,
        @kotlinx.serialization.SerialName("is_archived")
        val isArchived: Boolean,
        val name: String? = null
    )

    @kotlinx.serialization.Serializable
    private data class CreateMessageRequest(
        @kotlinx.serialization.SerialName("conversation_id")
        val conversationId: String,
        @kotlinx.serialization.SerialName("sender_id")
        val senderId: String,
        val content: String,
        val type: String, // Renamed from message_type
        @kotlinx.serialization.SerialName("sent_at")
        val sentAt: String,
        @kotlinx.serialization.SerialName("reply_to_message_id")
        val replyToMessageId: String? = null,
        @kotlinx.serialization.SerialName("thread_root_id")
        val threadRootId: String? = null,
        @kotlinx.serialization.SerialName("thread_depth")
        val threadDepth: Int = 0
    )

    override suspend fun createDirectConversation(participantIds: List<String>): Result<Conversation> {
        require(participantIds.size in 1..2) { "Direct conversations require 1 or 2 participants" }
        return runCatching {
            rateLimiter.acquireToken()
            val convBody = CreateConversationRequest(
                type = "direct",
                isArchived = false
            )
            // Use pocketBase.send directly to bypass RecordService restriction on Map<String, Any>
            val convJson = pocketBase.send<kotlinx.serialization.json.JsonObject>(
                path = "/api/collections/m_conversations/records",
                method = "POST",
                body = convBody
            )
            val convId = convJson["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to parse conversation ID")

            val now = Clock.System.now().toString()
            participantIds.distinct().forEach { userId ->
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
        rateLimiter.acquireToken()
        pocketBase.collection("m_conversation_participants")
            .getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId'", // Removed left_at=null check
                    sort = "joined_at"
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToParticipant(it) }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch participants", it)) }
    )

    override suspend fun addParticipants(conversationId: String, userIds: List<String>): Result<Unit> = runCatching {
        rateLimiter.acquireToken()
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
    override suspend fun getMessages(conversationId: String, page: Int, perPage: Int): Result<List<Message>> {
        // Try cache first if offline
        if (networkMonitor?.checkConnectivity() == false) {
            offlineCache?.getCachedMessages(conversationId)?.let { cached ->
                return Result.Success(cached)
            }
        }
        
        return runCatching {
            rateLimiter.acquireToken()
            pocketBase.collection("m_messages")
                .getList(
                    QueryOptions(
                        page = page,
                        perPage = perPage,
                        filter = "conversation_id='$conversationId' && deleted_at=''",
                        sort = "-sent_at"
                    )
                )
        }.fold(
            onSuccess = { 
                val messages = it.items.map { mapRecordToMessage(it) }.filter { it.deletedAt == null }
                // Cache for offline use
                offlineCache?.cacheMessages(conversationId, messages)
                Result.Success(messages)
            },
            onFailure = { 
                // Return cached on error
                offlineCache?.getCachedMessages(conversationId)?.let { cached ->
                    return@fold Result.Success(cached)
                }
                Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages", it))
            }
        )
    }

    override suspend fun sendMessage(conversationId: String, content: String, replyToMessageId: String?): Result<Message> = writeMutex.withLock {
        // If offline, queue the message
        if (networkMonitor?.checkConnectivity() == false) {
            offlineCache?.let { cache ->
                val localId = cache.queueSendMessage(conversationId, content, replyToMessageId)
                // Return the optimistic message from cache
                cache.getCachedMessages(conversationId)?.find { it.id == localId }?.let { msg ->
                    return Result.Success(msg)
                }
            }
            return Result.Error(AppException.Unknown("Offline - message queued"))
        }
        
        // Rate Limit Check
        rateLimiter.acquireToken()
        
        runCatching {
            val model = pocketBase.authStore.model
            val userId = (model as? RecordModel)?.id 
                ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                ?: return@runCatching Result.Error(AppException.Unknown("Not authenticated"))
            val now = Clock.System.now().toString()
            
            // Resolve threadRootId and Depth if replying
            var threadRootId: String? = null
            var currentDepth = 0
            
            if (replyToMessageId != null) {
                try {
                    // Fetch the message we are replying to
                    val replyTo = pocketBase.collection("m_messages").getOne(replyToMessageId)
                    val replyToObj = replyTo.jsonObject
                    val replyToId = replyToObj["id"]?.jsonPrimitive?.content ?: replyToMessageId
                    val existingRoot = replyToObj["thread_root_id"]?.jsonPrimitive?.content

                    // Logic for Thread Depth
                    val parentDepth = replyToObj["thread_depth"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                    currentDepth = parentDepth + 1
                    
                    // Logic for Thread Root
                    threadRootId = existingRoot.takeIf { !it.isNullOrEmpty() } ?: replyToId
                    
                    // Note: thread_reply_count removed from schema
                } catch (e: Exception) {
                    println("Warning: Reply parent $replyToMessageId not found: $e")
                }
            }
            
            val body = CreateMessageRequest(
                conversationId = conversationId,
                senderId = userId as String,
                content = content,
                type = "text",
                sentAt = now,
                replyToMessageId = replyToMessageId,
                threadRootId = threadRootId,
                threadDepth = currentDepth
            )
            
            val created = pocketBase.send<kotlinx.serialization.json.JsonObject>(
                path = "/api/collections/m_messages/records",
                method = "POST",
                body = body
            )
            val createdId = created["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to get message ID")
            
            // update conversation last message fields
            val updateBody = mapOf(
                "last_message_text" to content.take(100),
                "last_message_at" to now
            )
            pocketBase.collection("m_conversations").update(conversationId, updateBody)

            // fetch full message record and map
            val msgRecord = pocketBase.collection("m_messages").getOne(createdId)
            val message = mapRecordToMessage(msgRecord)
            
            // Add to cache
            offlineCache?.addMessageToCache(conversationId, message)
            
            message
        }.fold(
            onSuccess = { Result.Success(it as Message) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to send message", it)) }
        )
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> = writeMutex.withLock {
        rateLimiter.acquireToken()
        runCatching {
            val now = Clock.System.now().toString()
            // Soft Delete: Update deletedAt timestamp
            val updateBody = buildJsonObject {
                put("deleted_at", now)
            }
            pocketBase.send<JsonObject>(
                path = "/api/collections/m_messages/records/$messageId",
                method = "PATCH",
                body = updateBody.toString()
            )
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to delete message", it)) }
        )
    }

    // ============================= Threading =============================
    override suspend fun getReplies(messageId: String): Result<List<Message>> = runCatching {
        rateLimiter.acquireToken()
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "reply_to_message_id='$messageId' && deleted_at=''",
                    sort = "sent_at"
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }.filter { it.deletedAt == null }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch replies")) }
    )

    override suspend fun getThreadRoot(messageId: String): Result<Message> = runCatching {
        rateLimiter.acquireToken()
        var current = mapRecordToMessage(pocketBase.collection("m_messages").getOne(messageId))
        if (!current.threadRootId.isNullOrEmpty()) {
             return@runCatching mapRecordToMessage(pocketBase.collection("m_messages").getOne(current.threadRootId!!))
        }
        while (current.replyToMessageId != null) {
            current = mapRecordToMessage(pocketBase.collection("m_messages").getOne(current.replyToMessageId!!))
        }
        current
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to get thread root")) }
    )

    override suspend fun getFullThread(rootMessageId: String): Result<List<Message>> = runCatching {
        rateLimiter.acquireToken()
        val records = pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "(id='$rootMessageId' || thread_root_id='$rootMessageId') && deleted_at=''",
                    sort = "sent_at",
                    perPage = 500
                )
            )
        records.items.map { mapRecordToMessage(it) }.filter { it.deletedAt == null }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch full thread")) }
    )

    override suspend fun countReplies(messageId: String): Result<Int> = runCatching {
        rateLimiter.acquireToken()
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "reply_to_message_id='$messageId' && deleted_at=''",
                    perPage = 1
                )
            ).totalItems
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to count replies")) }
    )

    // ============================= Advanced Queries =============================
    override suspend fun searchMessages(query: String, conversationId: String): Result<List<Message>> = runCatching {
        rateLimiter.acquireToken()
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId' && content~'$query' && deleted_at=''",
                    sort = "-sent_at"
                )
            )
    }.fold(
        onSuccess = { 
            // Filter client-side
            val items = it.items.map { record -> mapRecordToMessage(record) }
            val filtered = items.filter { msg -> msg.content.contains(query, ignoreCase = true) && msg.deletedAt == null }
            Result.Success(filtered)
        },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Search failed")) }
    )

    override suspend fun getMessagesAfter(conversationId: String, timestamp: Instant, limit: Int): Result<List<Message>> = runCatching {
        rateLimiter.acquireToken()
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId' && sent_at>'$timestamp' && deleted_at=''",
                    sort = "sent_at",
                    perPage = limit
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }.filter { it.deletedAt == null }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages after")) }
    )

    override suspend fun getMessagesBefore(conversationId: String, timestamp: Instant, limit: Int): Result<List<Message>> = runCatching {
        rateLimiter.acquireToken()
        pocketBase.collection("m_messages")
            .getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId' && sent_at<'$timestamp' && deleted_at=''",
                    sort = "-sent_at",
                    perPage = limit
                )
            )
    }.fold(
        onSuccess = { Result.Success(it.items.map { mapRecordToMessage(it) }.filter { it.deletedAt == null }) },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch messages before")) }
    )

    override suspend fun createGroupConversation(name: String, participantIds: List<String>): Result<Conversation> {
        return runCatching {
            rateLimiter.acquireToken()
            val convBody = CreateConversationRequest(
                type = "group",
                isArchived = false,
                name = name
            )
            val convJson = pocketBase.send<kotlinx.serialization.json.JsonObject>(
                path = "/api/collections/m_conversations/records",
                method = "POST",
                body = convBody
            )
            val convId = convJson["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to parse conversation ID")

            val now = Clock.System.now().toString()
            participantIds.distinct().forEach { userId ->
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
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to create group conversation", it)) }
        )
    }

    override suspend fun removeParticipant(conversationId: String, userId: String): Result<Unit> = writeMutex.withLock {
        rateLimiter.acquireToken()
        runCatching {
             val records = pocketBase.collection("m_conversation_participants").getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId' && user_id='$userId'", // Removed left_at check
                    perPage = 1
                )
            )
            
            if (records.items.isNotEmpty()) {
                val item = records.items[0]
                val pId = (item as? JsonObject)?.get("id")?.jsonPrimitive?.content ?: (item as? RecordModel)?.id
                
                if (!pId.isNullOrEmpty()) {
                    // Hard Delete
                    pocketBase.collection("m_conversation_participants").delete(pId)
                }
            }
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to remove participant", it)) }
        )
    }

    override suspend fun updateParticipantSettings(conversationId: String, isMuted: Boolean?, isPinned: Boolean?): Result<Unit> = writeMutex.withLock {
        rateLimiter.acquireToken()
        runCatching {
            val model = pocketBase.authStore.model
            val userId = (model as? RecordModel)?.id 
                ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                ?: throw AppException.Unknown("Not authenticated")

             val records = pocketBase.collection("m_conversation_participants").getList(
                QueryOptions(
                    filter = "conversation_id='$conversationId' && user_id='$userId'",
                    perPage = 1
                )
            )
            
            if (records.items.isNotEmpty()) {
                val item = records.items[0]
                val pId = (item as? JsonObject)?.get("id")?.jsonPrimitive?.content ?: (item as? RecordModel)?.id
                
                if (!pId.isNullOrEmpty()) {
                    val updateBody = buildJsonObject {
                        if (isMuted != null) put("is_muted", isMuted)
                        if (isPinned != null) put("is_pinned", isPinned)
                    }
                    if (updateBody.isNotEmpty()) {
                        pocketBase.send<JsonObject>(
                            path = "/api/collections/m_conversation_participants/records/$pId",
                            method = "PATCH",
                            body = updateBody.toString()
                        )
                    }
                }
            }
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to update participant settings", it)) }
        )
    }

    override suspend fun markAsRead(conversationId: String): Result<Unit> = writeMutex.withLock {
        // Queue if offline
        if (networkMonitor?.checkConnectivity() == false) {
            offlineCache?.queueMarkAsRead(conversationId)
            return Result.Success(Unit)
        }
        
        // Rate Limit Check
        rateLimiter.acquireToken()
        runCatching {
            val model = pocketBase.authStore.model
            val userId = (model as? RecordModel)?.id 
                ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                ?: return@runCatching Result.Error(AppException.Unknown("Not authenticated"))

            // Find participant record for this user
            val records = pocketBase.collection("m_conversation_participants").getList(
                QueryOptions(
                     filter = "conversation_id='$conversationId' && user_id='$userId'"
                )
            )
            
            if (records.items.isNotEmpty()) {
                 val item = records.items[0]
                 val pId = (item as? JsonObject)?.get("id")?.jsonPrimitive?.content ?: (item as? RecordModel)?.id
                 
                 if (!pId.isNullOrEmpty()) {
                     val updateBody = buildJsonObject {
                         put("unread_count", 0)
                     }
                     pocketBase.send<JsonObject>(
                         path = "/api/collections/m_conversation_participants/records/$pId",
                         method = "PATCH",
                         body = updateBody.toString()
                     )
                 }
            }
            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to mark as read", it)) }
        )
    }

    // ============================= Real‑time placeholders =============================
    override fun subscribeToConversation(conversationId: String): Flow<Message> = realtimeService.subscribeToConversation(conversationId)
    override fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> = realtimeService.subscribeToTypingIndicators(conversationId)
    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> = writeMutex.withLock {
        rateLimiter.acquireToken()
        try {
            realtimeService.setTypingStatus(conversationId, isTyping)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppException.Unknown(e.message ?: "Failed to set typing status"))
        }
    }

    // ============================= Proust Questionnaire =============================
    override suspend fun getQuestionnaire(): Result<List<ProustQuestionnaire>> {
        logDebug("Fetching questionnaire")
        return try {
            val result = pocketBase.collection("t_proust_question") // Correct collection name
                .getListTyped<SdkQuestionnaire>(
                    QueryOptions(sort = "question") // sort? schema has no 'order', maybe text default
                )
            
            result.fold(
                ifLeft = { Result.Error(AppException.Network.ServerError(it.statusCode, it.message ?: "Failed to fetch questionnaire")) },
                ifRight = { list ->
                    Result.Success(list.items.map { it.toDomain() })
                }
            )
        } catch (e: Exception) {
            Result.Error(AppException.Unknown(e.message ?: "Failed to fetch questionnaire"))
        }
    }

    override suspend fun getUserAnswers(): Result<List<UserAnswer>> {
        return runCatching {
            val model = pocketBase.authStore.model
            val userId = (model as? RecordModel)?.id 
                 ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                 ?: throw AppException.Unknown("Not authenticated")

            val result = pocketBase.collection("t_user_questionnaire_responses")
                .getListTyped<SdkUserAnswer>(
                    QueryOptions(
                        filter = "user_id='$userId'",
                        sort = "-updated"
                    )
                )

            result.fold(
                ifLeft = { throw AppException.Network.ServerError(it.statusCode, it.message ?: "Failed to fetch answers") },
                ifRight = { list ->
                    list.items.map { fetched ->
                         UserAnswer(
                             id = fetched.id,
                             created = kotlinx.datetime.Instant.parse(fetched.created),
                             updated = kotlinx.datetime.Instant.parse(fetched.updated),
                             userId = fetched.userId,
                             questionId = fetched.questionId,
                             answerText = fetched.answerText
                         )
                    }
                }
            )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch user answers", it)) }
        )
    }

    override suspend fun submitQuestionnaireResponse(questionId: String, answer: String): Result<UserAnswer> {
        return runCatching {
             val model = pocketBase.authStore.model
             val userId = (model as? RecordModel)?.id 
                 ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                 ?: throw AppException.Unknown("Not authenticated")

             // Check if response exists
             val existing = pocketBase.collection("t_user_questionnaire_responses")
                 .getListTyped<SdkUserAnswer>(
                     QueryOptions(
                         filter = "user_id='$userId' && question_id='$questionId'",
                         perPage = 1
                     )
                 )
             
             existing.fold(
                 ifLeft = { throw AppException.Network.ServerError(it.statusCode, it.message ?: "Failed to check existing response") },
                 ifRight = { list ->
                     val body = mapOf(
                         "user_id" to userId,
                         "question_id" to questionId,
                         "response" to answer
                     )
                     
                     val record: kotlinx.serialization.json.JsonObject = if (list.items.isNotEmpty()) {
                         // Update
                         pocketBase.collection("t_user_questionnaire_responses")
                             .update(list.items.first().id, body)
                     } else {
                         // Create
                         pocketBase.collection("t_user_questionnaire_responses")
                             .create(body)
                     }
                     
                     val recordId = record["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to get response ID")

                     // Fetch updated to ensure we have fresh data
                     val fetched = pocketBase.collection("t_user_questionnaire_responses").getOne(recordId)
                     val json = fetched.jsonObject
                     
                     UserAnswer(
                         id = json["id"]?.jsonPrimitive?.content ?: "",
                         created = (json["created"]?.jsonPrimitive?.content ?: "").parsePocketBaseInstantOr(),
                         updated = (json["updated"]?.jsonPrimitive?.content ?: "").parsePocketBaseInstantOr(),
                         userId = json["user_id"]?.jsonPrimitive?.content ?: "",
                         questionId = json["question_id"]?.jsonPrimitive?.content ?: "",
                         answerText = json["response"]?.jsonPrimitive?.content ?: ""
                     )
                 }
             )
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to submit response", it)) }
        )
    }

    override suspend fun getMatches(): Result<List<Match>> = runCatching {
         val model = pocketBase.authStore.model
         val currentUserId = (model as? RecordModel)?.id 
             ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
             ?: throw AppException.Unknown("Not authenticated")

         val matchesResult = pocketBase.collection("m_matches")
             .getList(
                 QueryOptions(
                     filter = "user_id='$currentUserId' || matched_user_id='$currentUserId'",
                     sort = "-match_score"
                 )
             )
         
         fun getString(obj: JsonObject, key: String): String = obj[key]?.jsonPrimitive?.content ?: ""

         val matches = matchesResult.items.map { item ->
             val json = item.jsonObject
             
             val uid = getString(json, "user_id")
             val mid = getString(json, "matched_user_id")
             val otherUserId = if (uid == currentUserId) mid else uid
             
             var match = Match(
                 id = getString(json, "id"),
                 userId = uid,
                 matchedUserId = mid,
                 matchScore = json["match_score"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0,
                 status = when (getString(json, "status")) {
                     "accepted" -> MatchStatus.ACCEPTED
                     "rejected" -> MatchStatus.REJECTED
                     else -> MatchStatus.PENDING
                 }
             )
             
             match to otherUserId
         }

         if (matches.isEmpty()) {
             Result.Success(emptyList())
         } else {
             val otherUserIds = matches.map { it.second }.distinct()
             val profileFilter = otherUserIds.joinToString(" || ") { "userId='$it'" }
             
             val profilesResult = pocketBase.collection("s_profiles")
                 .getList(
                     QueryOptions(
                         filter = profileFilter,
                         perPage = otherUserIds.size + 5
                     )
                 )
             
             val profileMap = profilesResult.items.associate { item ->
                  val json = item.jsonObject
                  val pUserId = getString(json, "userId")
                  
                  val rawBirthDate = getString(json, "birthDate")
                  val birthDateStr = if (rawBirthDate.length >= 10) rawBirthDate.substring(0, 10) else "2000-01-01"
                  
                  val profile = Profile(
                      id = getString(json, "id"),
                      created = (getString(json, "created")).parsePocketBaseInstantOr(),
                      updated = (getString(json, "updated")).parsePocketBaseInstantOr(),
                      userId = pUserId,
                      firstName = getString(json, "firstName"),
                      lastName = getString(json, "lastName"),
                      birthDate = kotlinx.datetime.LocalDate.parse(birthDateStr),
                      bio = getString(json, "bio"),
                      location = getString(json, "location"),
                      seeking = when(getString(json, "seeking")) {
                          "Friendship" -> SeekingStatus.FRIENDSHIP
                          "Relationship" -> SeekingStatus.RELATIONSHIP
                          else -> SeekingStatus.BOTH
                      },
                      profilePicture = getString(json, "profilePicture"),
                      photos = json["photos"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                      aboutMe = getString(json, "aboutMe"),
                      height = json["height"]?.jsonPrimitive?.content?.toDoubleOrNull(),
                      occupation = getString(json, "occupation"),
                      education = getString(json, "education"),
                      interests = json["interests"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                  )
                  
                  pUserId to profile
             }
             
             val finalMatches = matches.map { (match, otherId) ->
                 match.copy(expand = MatchExpand(matchedUserProfile = profileMap[otherId]))
             }
             
             Result.Success(finalMatches)
         }
    }.fold(
        onSuccess = { it },
        onFailure = { Result.Error(AppException.Unknown(it.message ?: "Failed to fetch matches", it)) }
    )

    private fun SdkQuestionnaire.toDomain(): ProustQuestionnaire {
         return ProustQuestionnaire(
             id = this.id,
             created = kotlinx.datetime.Instant.parse(this.created),
             updated = kotlinx.datetime.Instant.parse(this.updated),
             questionText = this.questionText,
             questionOrder = this.questionOrder,
             isActive = this.isActive
         )
    }

    // ============================= Mappers =============================
    fun mapRecordToConversation(recordElement: kotlinx.serialization.json.JsonElement): Conversation {
        val record = recordElement.jsonObject
        
        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getBoolean(key: String): Boolean = record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

        return Conversation(
            id = getString("id"),
            conversationType = when (getString("type")) {
                "direct" -> ConversationType.DIRECT
                "group" -> ConversationType.GROUP
                "channel" -> ConversationType.CHANNEL
                else -> ConversationType.DIRECT
            },
            conversationName = getString("name"),
            conversationAvatar = getString("avatar"),
            lastMessageText = getString("last_message_text"),
            lastMessageAt = getString("last_message_at").parsePocketBaseInstant(),
            totalMessageCount = 0,
            maxParticipants = 0,
            isArchived = getBoolean("isArchived"),
            created = getString("created").parsePocketBaseInstantOr(),
            updated = getString("updated").parsePocketBaseInstantOr()
        )
    }

    fun mapRecordToParticipant(recordElement: kotlinx.serialization.json.JsonElement): ConversationParticipant {
        val record = recordElement.jsonObject

        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getInt(key: String): Int = record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        fun getBoolean(key: String): Boolean = record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

        return ConversationParticipant(
            id = getString("id"),
            conversationId = getString("conversation_id"),
            userId = getString("user_id"),
            role = when (getString("role")) {
                "admin" -> ParticipantRole.ADMIN
                "readonly" -> ParticipantRole.READONLY
                else -> ParticipantRole.MEMBER
            },
            unreadCount = getInt("unread_count"),
            lastReadAt = null,
            joinedAt = getString("joined_at").parsePocketBaseInstantOr(),
            leftAt = null,
            isMuted = getBoolean("is_muted"),
            isPinned = getBoolean("is_pinned"),
            created = getString("created").parsePocketBaseInstantOr(),
            updated = getString("updated").parsePocketBaseInstantOr()
        )
    }

    fun mapRecordToMessage(recordElement: kotlinx.serialization.json.JsonElement): Message {
        val record = recordElement.jsonObject
        
        fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
        fun getInt(key: String): Int = record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        fun getList(key: String): List<String> = record[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        return Message(
            id = getString("id"),
            conversationId = getString("conversation_id"),
            senderId = getString("sender_id"),
            content = getString("content"),
            messageType = when (getString("type")) {
                "image" -> MessageType.IMAGE
                "file" -> MessageType.FILE
                "system" -> MessageType.SYSTEM
                else -> MessageType.TEXT
            },
            attachments = getList("attachments"),
            sentAt = getString("sent_at").parsePocketBaseInstantOr(),
            editedAt = getString("edited_at").parsePocketBaseInstant(),
            deletedAt = getString("deleted_at").parsePocketBaseInstant(),
            readByCount = 0,
            replyToMessageId = getString("reply_to_message_id").takeIf { it.isNotEmpty() },
            threadRootId = getString("thread_root_id").takeIf { it.isNotEmpty() },
            threadDepth = getInt("thread_depth"),
            threadReplyCount = 0,
            created = getString("created").parsePocketBaseInstantOr(),
            updated = getString("updated").parsePocketBaseInstantOr()
        )
    }

    override suspend fun getGlobalSettings(): Result<MessagingSettings> = runCatching {
        rateLimiter.acquireToken()
        val model = pocketBase.authStore.model
        val userId = (model as? RecordModel)?.id 
            ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            ?: return@runCatching Result.Error(AppException.Unknown("Not authenticated"))
            
        // Fetch user properties
        val userProps = pocketBase.collection("t_user_property")
            .getList(QueryOptions(filter = "user_id='$userId'"))
            .items
            .associate { 
                val key = it["key"]?.jsonPrimitive?.content ?: ""
                val value = it["value"]?.jsonPrimitive?.content ?: ""
                key to value
            }

        // Defaults
        val readReceipts = userProps["messaging.read_receipts_enabled"]?.toBoolean() ?: true
        val typingStatus = userProps["messaging.typing_status_enabled"]?.toBoolean() ?: true

        Result.Success(MessagingSettings(
            readReceiptsEnabled = readReceipts,
            typingStatusEnabled = typingStatus
        ))
    }.getOrElse { 
        Result.Error(AppException.Unknown("Failed to get settings", it))
    }

    override suspend fun updateGlobalSettings(settings: MessagingSettings): Result<Unit> = writeMutex.withLock {
        rateLimiter.acquireToken()
        runCatching {
             val model = pocketBase.authStore.model
             val userId = (model as? RecordModel)?.id 
                ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
                ?: return@runCatching Result.Error(AppException.Unknown("Not authenticated"))
            
            // Helper to upsert
            suspend fun upsertProperty(key: String, value: String) {
                // Check if exists
                val existing = pocketBase.collection("t_user_property")
                    .getList(QueryOptions(filter = "user_id='$userId' && key='$key'", perPage = 1))
                    .items.firstOrNull()
                
                if (existing != null) {
                    val existingId = existing["id"]?.jsonPrimitive?.content ?: return
                    pocketBase.collection("t_user_property").update(existingId, mapOf("value" to value))
                } else {
                    pocketBase.collection("t_user_property").create(mapOf(
                        "user_id" to userId,
                        "key" to key,
                        "value" to value
                    ))
                }
            }

            upsertProperty("messaging.read_receipts_enabled", settings.readReceiptsEnabled.toString())
            upsertProperty("messaging.typing_status_enabled", settings.typingStatusEnabled.toString())

            Unit
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { Result.Error(AppException.Unknown("Failed to update settings", it)) }
        )
    }
}
