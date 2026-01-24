package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.functional.getListTyped
import io.pocketbase.models.QueryOptions
import io.pocketbase.models.RecordModel
import io.pocketbase.types.Questionnaire as SdkQuestionnaire
import io.pocketbase.types.UserAnswer as SdkUserAnswer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.data.DatabaseCollections
import love.bside.app.data.mapPocketBaseError
import love.bside.app.data.remote.RateLimiter
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
import love.bside.app.domain.repository.AttachmentData
import love.bside.app.domain.repository.MessagingRepository
import love.bside.app.utils.parsePocketBaseInstant
import love.bside.app.utils.parsePocketBaseInstantOr

@Serializable
private data class ParticipantCreateRequest(
        @kotlinx.serialization.SerialName("conversation_id") val conversationId: String,
        @kotlinx.serialization.SerialName("user_id") val userId: String,
        val role: String,
        @kotlinx.serialization.SerialName("unread_count") val unreadCount: Int,
        @kotlinx.serialization.SerialName("joined_at") val joinedAt: String,
        @kotlinx.serialization.SerialName("is_muted") val isMuted: Boolean,
        @kotlinx.serialization.SerialName("is_pinned") val isPinned: Boolean
)

class PocketBaseMessagingRepository(
        private val pocketBase: PocketBase,
        realtimeService: RealtimeService? = null,
        private val offlineCache: love.bside.app.data.cache.OfflineCacheManager? = null,
        private val networkMonitor: love.bside.app.core.NetworkMonitor? = null
) : MessagingRepository {
        private val realtimeService: RealtimeService =
                realtimeService ?: RealtimeServiceImpl(pocketBase, this)
        // "Smart Queue" to serialize writes (Single Writer for SQLite)
        private val writeMutex = Mutex()

        // Rate Limiter: 60 requests per minute (1 per second) to stay safe from 429s
        private val rateLimiter =
                RateLimiter(maxRequests = 60, timeWindow = 60.seconds, burstCapacity = 5)

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
                                // rateLimiter.acquireToken()
                                val participants =
                                        pocketBase
                                                .collection(
                                                        DatabaseCollections
                                                                .M_CONVERSATION_PARTICIPANTS
                                                )
                                                .getList(
                                                        QueryOptions(
                                                                filter =
                                                                        "user_id='$userId'", // Removed left_at
                                                                // check (hard
                                                                // delete handling)
                                                                expand = "conversation_id",
                                                                sort = "-updated"
                                                        )
                                                )

                                participants.items
                                        .mapNotNull {
                                                val expand = it["expand"]?.jsonObject
                                                val conversationRecord =
                                                        expand?.get("conversation_id")?.jsonObject
                                                if (conversationRecord != null)
                                                        mapRecordToConversation(conversationRecord)
                                                else null
                                        }
                                        .also { conversations ->
                                                // Cache for offline use
                                                offlineCache?.cacheConversations(
                                                        userId,
                                                        conversations
                                                )
                                        }
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        // Return cached on error
                                        offlineCache?.getCachedConversations(userId)?.let { cached
                                                ->
                                                return@fold Result.Success(cached)
                                        }
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch conversations",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        override suspend fun getConversation(conversationId: String): Result<Conversation> =
                runCatching {
                                rateLimiter.acquireToken()
                                mapRecordToConversation(
                                        pocketBase
                                                .collection(DatabaseCollections.M_CONVERSATIONS)
                                                .getOne(conversationId)
                                )
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch conversation",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        @kotlinx.serialization.Serializable
        private data class CreateConversationRequest(
                val type: String,
                @kotlinx.serialization.SerialName("is_archived") val isArchived: Boolean,
                val name: String? = null
        )

        @kotlinx.serialization.Serializable
        private data class CreateMessageRequest(
                @kotlinx.serialization.SerialName("conversation_id") val conversationId: String,
                @kotlinx.serialization.SerialName("sender_id") val senderId: String,
                val content: String,
                val type: String, // Renamed from message_type
                @kotlinx.serialization.SerialName("sent_at") val sentAt: String,
                @kotlinx.serialization.SerialName("reply_to_message_id")
                val replyToMessageId: String? = null,
                @kotlinx.serialization.SerialName("thread_root_id")
                val threadRootId: String? = null,
                @kotlinx.serialization.SerialName("thread_depth") val threadDepth: Int = 0
        )

        override suspend fun createDirectConversation(
                participantIds: List<String>
        ): Result<Conversation> {
                require(participantIds.size in 1..2) {
                        "Direct conversations require 1 or 2 participants"
                }
                return runCatching {
                                rateLimiter.acquireToken()
                                val convBody =
                                        CreateConversationRequest(
                                                type = "direct",
                                                isArchived = false
                                        )
                                // Use pocketBase.send directly to bypass RecordService restriction
                                // on
                                // Map<String, Any>
                                val convJson =
                                        pocketBase.send<kotlinx.serialization.json.JsonObject>(
                                                path =
                                                        "/api/collections/${DatabaseCollections.M_CONVERSATIONS}/records",
                                                method = "POST",
                                                body = convBody
                                        )
                                val convId =
                                        convJson["id"]?.jsonPrimitive?.content
                                                ?: throw AppException.Unknown(
                                                        "Failed to parse conversation ID"
                                                )

                                val now = Clock.System.now().toString()
                                participantIds.distinct().forEach { userId ->
                                        val partBody =
                                                ParticipantCreateRequest(
                                                        conversationId = convId,
                                                        userId = userId,
                                                        role = "member",
                                                        unreadCount = 0,
                                                        joinedAt = now,
                                                        isMuted = false,
                                                        isPinned = false
                                                )
                                        pocketBase.send<io.pocketbase.models.RecordModel>(
                                                path =
                                                        "/api/collections/${DatabaseCollections.M_CONVERSATION_PARTICIPANTS}/records",
                                                method = "POST",
                                                body = partBody
                                        )
                                }
                                mapRecordToConversation(convJson)
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "create conversation",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        // ============================= Participants =============================
        override suspend fun getParticipants(
                conversationId: String
        ): Result<List<ConversationParticipant>> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_CONVERSATION_PARTICIPANTS)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "conversation_id='$conversationId'", // Removed left_at=null check
                                                        sort = "joined_at"
                                                )
                                        )
                        }
                        .fold(
                                onSuccess = {
                                        Result.Success(it.items.map { mapRecordToParticipant(it) })
                                },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch participants",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun addParticipants(
                conversationId: String,
                userIds: List<String>
        ): Result<Unit> =
                runCatching {
                                // TODO: Restore implementation
                                Unit
                        }
                        .fold(
                                onSuccess = { Result.Success(Unit) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "add participants",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        // ============================= Messages =============================
        override suspend fun getMessages(
                conversationId: String,
                page: Int,
                perPage: Int
        ): Result<List<Message>> {
                // Try cache first if offline
                if (networkMonitor?.checkConnectivity() == false) {
                        offlineCache?.getCachedMessages(conversationId)?.let { cached ->
                                return Result.Success(cached)
                        }
                }

                return runCatching {
                                rateLimiter.acquireToken()
                                val messageRecords = pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        page = page,
                                                        perPage = perPage,
                                                        filter =
                                                                "conversation_id='$conversationId' && deleted_at=''",
                                                        sort = "-sent_at",
                                                        expand = "reply_to_message_id"
                                                )
                                        )

                                val messageIds = messageRecords.items.mapNotNull { it["id"]?.jsonPrimitive?.content }
                                val userId = pocketBase.authStore.model?.get("id")?.jsonPrimitive?.content

                                val readMessageIds = if (userId != null && messageIds.isNotEmpty()) {
                                        val filter = "user_id='$userId' && (${messageIds.joinToString(" || ") { "message_id='$it'" }})"
                                        pocketBase.collection("m_read_receipts")
                                                .getFullList(QueryOptions(filter = filter))
                                                .mapNotNull { it["message_id"]?.jsonPrimitive?.content }
                                                .toSet()
                                } else {
                                        emptySet()
                                }

                                val messages =
                                        messageRecords.items.map { mapRecordToMessage(it, readMessageIds) }.filter {
                                                it.deletedAt == null
                                        }
                                // Cache for offline use
                                offlineCache?.cacheMessages(conversationId, messages)
                                Result.Success(messages)
                        }
                        .fold(
                                onSuccess = { it },
                                onFailure = {
                                        // Return cached on error
                                        offlineCache?.getCachedMessages(conversationId)?.let {
                                                cached ->
                                                return@fold Result.Success(cached)
                                        }
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch messages",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        override suspend fun sendMessage(
                conversationId: String,
                content: String,
                replyToMessageId: String?,
                attachments: List<AttachmentData>?
        ): Result<Message> =
                writeMutex.withLock {
                        // If offline, queue the message
                        if (networkMonitor?.checkConnectivity() == false) {
                                offlineCache?.let { cache ->
                                        // TODO: Handle offline attachments (store locally and sync
                                        // later)
                                        val localId =
                                                cache.queueSendMessage(
                                                        conversationId,
                                                        content,
                                                        replyToMessageId
                                                )
                                        // Return the optimistic message from cache
                                        cache.getCachedMessages(conversationId)
                                                ?.find { it.id == localId }
                                                ?.let { msg ->
                                                        return Result.Success(msg)
                                                }
                                }
                                return Result.Error(
                                        AppException.Unknown("Offline - message queued")
                                )
                        }

                        // Rate Limit Check
                        rateLimiter.acquireToken()

                        runCatching {
                                        val model = pocketBase.authStore.model
                                        val userId =
                                                (model as? RecordModel)?.id
                                                        ?: (model as?
                                                                        kotlinx.serialization.json.JsonObject)
                                                                ?.get("id")
                                                                ?.jsonPrimitive
                                                                ?.content
                                                                ?: return@runCatching Result.Error(
                                                                AppException.Unknown(
                                                                        "Not authenticated"
                                                                )
                                                        )
                                        val now = Clock.System.now().toString()

                                        // Resolve threadRootId and Depth if replying
                                        var threadRootId: String? = null
                                        var currentDepth = 0

                                        if (replyToMessageId != null) {
                                                try {
                                                        // Fetch the message we are replying to
                                                        val replyTo =
                                                                pocketBase
                                                                        .collection(
                                                                                DatabaseCollections
                                                                                        .M_MESSAGES
                                                                        )
                                                                        .getOne(replyToMessageId)
                                                        val replyToObj = replyTo.jsonObject
                                                        val replyToId =
                                                                replyToObj["id"]
                                                                        ?.jsonPrimitive
                                                                        ?.content
                                                                        ?: replyToMessageId
                                                        val existingRoot =
                                                                replyToObj["thread_root_id"]
                                                                        ?.jsonPrimitive
                                                                        ?.content

                                                        // Logic for Thread Depth
                                                        val parentDepth =
                                                                replyToObj["thread_depth"]
                                                                        ?.jsonPrimitive?.content
                                                                        ?.toIntOrNull()
                                                                        ?: 0
                                                        currentDepth = parentDepth + 1

                                                        // Logic for Thread Root
                                                        threadRootId =
                                                                existingRoot.takeIf {
                                                                        !it.isNullOrEmpty()
                                                                }
                                                                        ?: replyToId

                                                        // Note: thread_reply_count removed from
                                                        // schema
                                                } catch (e: Exception) {
                                                        println(
                                                                "Warning: Reply parent $replyToMessageId not found: $e"
                                                        )
                                                }
                                        }

                                        val body =
                                                CreateMessageRequest(
                                                        conversationId = conversationId,
                                                        senderId = userId as String,
                                                        content = content,
                                                        type = "text",
                                                        sentAt = now,
                                                        replyToMessageId = replyToMessageId,
                                                        threadRootId = threadRootId,
                                                        threadDepth = currentDepth
                                                )

                                        // Convert data class to Map for SDK create(body: Map, ...)
                                        // method
                                        // We use the @SerialName values manually here since we are
                                        // bypassing
                                        // the serializer for multipart
                                        val bodyMap: MutableMap<String, Any?> =
                                                mutableMapOf(
                                                        "conversation_id" to body.conversationId,
                                                        "sender_id" to body.senderId,
                                                        "content" to body.content,
                                                        "type" to body.type, // "text"
                                                        "sent_at" to body.sentAt,
                                                        "thread_depth" to body.threadDepth
                                                )
                                        if (body.replyToMessageId != null)
                                                bodyMap["reply_to_message_id"] =
                                                        body.replyToMessageId
                                        if (body.threadRootId != null)
                                                bodyMap["thread_root_id"] = body.threadRootId

                                        // Build Multipart Request manually or use SDK convenience
                                        // if available
                                        // The PocketBase KMP SDK `create` overload supporting files
                                        // handles
                                        // this.
                                        // However, the current generic `send` above (lines 328-332)
                                        // uses JSON.
                                        // We must switch to using the collection method which
                                        // supports
                                        // Multipart.

                                        // Since we are using commonMain, we might not have
                                        // `java.io.File`.
                                        // The `AttachmentData` has `ByteArray`.

                                        // IF we have attachments, we must use the
                                        // collection().create() with
                                        // multipart.
                                        // IF NOT, we can stick to JSON or use collection().create()
                                        // without
                                        // files.

                                        val createdRecord =
                                                if (attachments.isNullOrEmpty()) {
                                                        // Use standard JSON create if no
                                                        // attachments
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .M_MESSAGES
                                                                )
                                                                .create(body)
                                                } else {
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .M_MESSAGES
                                                                )
                                                                .create(
                                                                        body = bodyMap,
                                                                        files =
                                                                                attachments.map {
                                                                                        attachment
                                                                                        ->
                                                                                        io.pocketbase
                                                                                                .models
                                                                                                .FileField(
                                                                                                        fieldName =
                                                                                                                "attachments", // Confirm this matches schema name? Usually "attachments" or "file"
                                                                                                        fileName =
                                                                                                                attachment
                                                                                                                        .fileName,
                                                                                                        data =
                                                                                                                attachment
                                                                                                                        .data
                                                                                                )
                                                                                }
                                                                )
                                                }

                                        val createdId =
                                                createdRecord["id"]?.jsonPrimitive?.content
                                                        ?: throw AppException.Unknown(
                                                                "Failed to get message ID"
                                                        )
                                        /*
                                                    val created = pocketBase.send<kotlinx.serialization.json.JsonObject>(
                                                        path = "/api/collections/${DatabaseCollections.M_MESSAGES}/records",
                                                        method = "POST",
                                                        body = body
                                                    )
                                                    val createdId = created["id"]?.jsonPrimitive?.content ?: throw AppException.Unknown("Failed to get message ID")
                                        */

                                        // update conversation last message fields
                                        val updateBody =
                                                mapOf(
                                                        "last_message_text" to content.take(100),
                                                        "last_message_at" to now
                                                )
                                        pocketBase
                                                .collection(DatabaseCollections.M_CONVERSATIONS)
                                                .update(conversationId, updateBody)

                                        // fetch full message record and map
                                        val msgRecord =
                                                pocketBase
                                                        .collection(DatabaseCollections.M_MESSAGES)
                                                        .getOne(createdId)
                                        val message = mapRecordToMessage(msgRecord)

                                        // Add to cache
                                        offlineCache?.addMessageToCache(conversationId, message)

                                        message
                                }
                                .fold(
                                        onSuccess = { Result.Success(it as Message) },
                                        onFailure = {
                                                Result.Error(
                                                        mapPocketBaseError(
                                                                "send message",
                                                                it as Exception
                                                        )
                                                )
                                        }
                                )
                }

        override suspend fun deleteMessage(messageId: String): Result<Unit> =
                writeMutex.withLock {
                        rateLimiter.acquireToken()
                        runCatching {
                                        // TODO: Restore implementation
                                        Unit
                                }
                                .fold(
                                        onSuccess = { Result.Success(Unit) },
                                        onFailure = {
                                                Result.Error(
                                                        mapPocketBaseError(
                                                                "delete message",
                                                                it as Exception
                                                        )
                                                )
                                        }
                                )
                }

        // ============================= Threading =============================
        override suspend fun getReplies(messageId: String): Result<List<Message>> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "reply_to_message_id='$messageId' && deleted_at=''",
                                                        sort = "sent_at"
                                                )
                                        )
                        }
                        .fold(
                                onSuccess = {
                                        Result.Success(
                                                it.items.map { mapRecordToMessage(it) }.filter {
                                                        it.deletedAt == null
                                                }
                                        )
                                },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError("fetch replies", it as Exception)
                                        )
                                }
                        )

        override suspend fun getThreadRoot(messageId: String): Result<Message> =
                runCatching {
                                rateLimiter.acquireToken()
                                var current =
                                        mapRecordToMessage(
                                                pocketBase
                                                        .collection(DatabaseCollections.M_MESSAGES)
                                                        .getOne(messageId)
                                        )
                                if (!current.threadRootId.isNullOrEmpty()) {
                                        return@runCatching mapRecordToMessage(
                                                pocketBase
                                                        .collection(DatabaseCollections.M_MESSAGES)
                                                        .getOne(current.threadRootId!!)
                                        )
                                }
                                while (current.replyToMessageId != null) {
                                        current =
                                                mapRecordToMessage(
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .M_MESSAGES
                                                                )
                                                                .getOne(current.replyToMessageId!!)
                                                )
                                }
                                current
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "get thread root",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun getFullThread(rootMessageId: String): Result<List<Message>> =
                runCatching {
                                rateLimiter.acquireToken()
                                val records =
                                        pocketBase
                                                .collection(DatabaseCollections.M_MESSAGES)
                                                .getList(
                                                        QueryOptions(
                                                                filter =
                                                                        "(id='$rootMessageId' || thread_root_id='$rootMessageId') && deleted_at=''",
                                                                sort = "sent_at",
                                                                perPage = 500
                                                        )
                                                )
                                records.items.map { mapRecordToMessage(it) }.filter {
                                        it.deletedAt == null
                                }
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch full thread",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun countReplies(messageId: String): Result<Int> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "reply_to_message_id='$messageId' && deleted_at=''",
                                                        perPage = 1
                                                )
                                        )
                                        .totalItems
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError("count replies", it as Exception)
                                        )
                                }
                        )

        // ============================= Advanced Queries =============================
        override suspend fun searchMessages(
                query: String,
                conversationId: String
        ): Result<List<Message>> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "conversation_id='$conversationId' && content~'$query' && deleted_at=''",
                                                        sort = "-sent_at"
                                                )
                                        )
                        }
                        .fold(
                                onSuccess = {
                                        // Filter client-side
                                        val items =
                                                it.items.map { record ->
                                                        mapRecordToMessage(record)
                                                }
                                        val filtered =
                                                items.filter { msg ->
                                                        msg.content.contains(
                                                                query,
                                                                ignoreCase = true
                                                        ) && msg.deletedAt == null
                                                }
                                        Result.Success(filtered)
                                },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "search messages",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun getMessagesAfter(
                conversationId: String,
                timestamp: Instant,
                limit: Int
        ): Result<List<Message>> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "conversation_id='$conversationId' && sent_at>'$timestamp' && deleted_at=''",
                                                        sort = "sent_at",
                                                        perPage = limit
                                                )
                                        )
                        }
                        .fold(
                                onSuccess = {
                                        Result.Success(
                                                it.items.map { mapRecordToMessage(it) }.filter {
                                                        it.deletedAt == null
                                                }
                                        )
                                },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch messages after",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun getMessagesBefore(
                conversationId: String,
                timestamp: Instant,
                limit: Int
        ): Result<List<Message>> =
                runCatching {
                                rateLimiter.acquireToken()
                                pocketBase
                                        .collection(DatabaseCollections.M_MESSAGES)
                                        .getList(
                                                QueryOptions(
                                                        filter =
                                                                "conversation_id='$conversationId' && sent_at<'$timestamp' && deleted_at=''",
                                                        sort = "-sent_at",
                                                        perPage = limit
                                                )
                                        )
                        }
                        .fold(
                                onSuccess = {
                                        Result.Success(
                                                it.items.map { mapRecordToMessage(it) }.filter {
                                                        it.deletedAt == null
                                                }
                                        )
                                },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch messages before",
                                                        it as Exception
                                                )
                                        )
                                }
                        )

        override suspend fun createGroupConversation(
                name: String,
                participantIds: List<String>
        ): Result<Conversation> {
                return runCatching {
                                rateLimiter.acquireToken()
                                val convBody =
                                        CreateConversationRequest(
                                                type = "group",
                                                isArchived = false,
                                                name = name
                                        )
                                val convJson =
                                        pocketBase.send<kotlinx.serialization.json.JsonObject>(
                                                path =
                                                        "/api/collections/${DatabaseCollections.M_CONVERSATIONS}/records",
                                                method = "POST",
                                                body = convBody
                                        )
                                val convId =
                                        convJson["id"]?.jsonPrimitive?.content
                                                ?: throw AppException.Unknown(
                                                        "Failed to parse conversation ID"
                                                )

                                val now = Clock.System.now().toString()
                                participantIds.distinct().forEach { userId ->
                                        val partBody =
                                                ParticipantCreateRequest(
                                                        conversationId = convId,
                                                        userId = userId,
                                                        role = "member",
                                                        unreadCount = 0,
                                                        joinedAt = now,
                                                        isMuted = false,
                                                        isPinned = false
                                                )
                                        pocketBase.send<io.pocketbase.models.RecordModel>(
                                                path =
                                                        "/api/collections/${DatabaseCollections.M_CONVERSATION_PARTICIPANTS}/records",
                                                method = "POST",
                                                body = partBody
                                        )
                                }
                                mapRecordToConversation(convJson)
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "create group conversation",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        override suspend fun removeParticipant(
                conversationId: String,
                userId: String
        ): Result<Unit> =
                writeMutex.withLock {
                        rateLimiter.acquireToken()
                        runCatching {
                                        // TODO: Restore implementation
                                        Unit
                                }
                                .fold(
                                        onSuccess = { Result.Success(Unit) },
                                        onFailure = {
                                                Result.Error(
                                                        mapPocketBaseError(
                                                                "remove participant",
                                                                it as Exception
                                                        )
                                                )
                                        }
                                )
                }

        override suspend fun updateParticipantSettings(
                conversationId: String,
                isMuted: Boolean?,
                isPinned: Boolean?
        ): Result<Unit> =
                writeMutex.withLock {
                        rateLimiter.acquireToken()
                        runCatching {
                                        // TODO: Restore implementation
                                        Unit
                                }
                                .fold(
                                        onSuccess = { Result.Success(Unit) },
                                        onFailure = {
                                                Result.Error(
                                                        mapPocketBaseError(
                                                                "update participant settings",
                                                                it as Exception
                                                        )
                                                )
                                        }
                                )
                }

        @Serializable
        private data class MarkAsReadRequest(
            @SerialName("message_id") val messageId: String,
            @SerialName("user_id") val userId: String,
            @SerialName("read_at") val readAt: String
        )

        override suspend fun markAsRead(messageId: String): Result<Unit> =
            writeMutex.withLock {
                rateLimiter.acquireToken()
                runCatching {
                    val userId = pocketBase.authStore.model?.get("id")?.jsonPrimitive?.content
                        ?: return@runCatching Unit

                    val request = MarkAsReadRequest(
                        messageId = messageId,
                        userId = userId,
                        readAt = Clock.System.now().toString()
                    )

                    // This assumes a 'm_read_receipts' collection exists.
                    // A unique index on (message_id, user_id) on the backend will prevent duplicates.
                    pocketBase.collection("m_read_receipts").create(request)
                    Unit
                }.fold(
                    onSuccess = { Result.Success(Unit) },
                    onFailure = {
                        // Don't treat "already exists" as a failure.
                        if (it.message?.contains("unique constraint") == true) {
                            Result.Success(Unit)
                        } else {
                            Result.Error(mapPocketBaseError("mark as read", it as Exception))
                        }
                    }
                )
            }

        // ============================= Real‑time placeholders =============================
        override fun subscribeToConversation(conversationId: String): Flow<Message> =
                realtimeService.subscribeToConversation(conversationId)
        override fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> =
                realtimeService.subscribeToTypingIndicators(conversationId)
        override suspend fun setTypingStatus(
                conversationId: String,
                isTyping: Boolean
        ): Result<Unit> =
                writeMutex.withLock {
                        rateLimiter.acquireToken()
                        try {
                                realtimeService.setTypingStatus(conversationId, isTyping)
                                Result.Success(Unit)
                        } catch (e: Exception) {
                                Result.Error(
                                        AppException.Unknown(
                                                e.message ?: "Failed to set typing status"
                                        )
                                )
                        }
                }

        // ============================= Proust Questionnaire =============================
        override suspend fun getQuestionnaire(): Result<List<ProustQuestionnaire>> {
                logDebug("Fetching questionnaire")
                return try {
                        val result =
                                pocketBase
                                        .collection(
                                                DatabaseCollections.T_PROUST_QUESTION
                                        ) // Correct collection name
                                        .getListTyped<SdkQuestionnaire>(
                                                QueryOptions(
                                                        sort = "question"
                                                ) // sort? schema has no 'order', maybe text default
                                        )

                        result.fold(
                                ifLeft = {
                                        Result.Error(
                                                AppException.Network.ServerError(
                                                        it.statusCode,
                                                        it.message
                                                                ?: "Failed to fetch questionnaire"
                                                )
                                        )
                                },
                                ifRight = { list ->
                                        Result.Success(list.items.map { it.toDomain() })
                                }
                        )
                } catch (e: Exception) {
                        Result.Error(
                                AppException.Unknown(e.message ?: "Failed to fetch questionnaire")
                        )
                }
        }

        override suspend fun getUserAnswers(): Result<List<UserAnswer>> {
                return runCatching {
                                val model = pocketBase.authStore.model
                                val userId =
                                        (model as? RecordModel)?.id
                                                ?: (model as? kotlinx.serialization.json.JsonObject)
                                                        ?.get("id")
                                                        ?.jsonPrimitive
                                                        ?.content
                                                        ?: throw AppException.Unknown(
                                                        "Not authenticated"
                                                )

                                val result =
                                        pocketBase
                                                .collection(
                                                        DatabaseCollections
                                                                .T_USER_QUESTIONNAIRE_RESPONSES
                                                )
                                                .getListTyped<SdkUserAnswer>(
                                                        QueryOptions(
                                                                filter = "user_id='$userId'",
                                                                sort = "-updated"
                                                        )
                                                )

                                result.fold(
                                        ifLeft = {
                                                throw AppException.Network.ServerError(
                                                        it.statusCode,
                                                        it.message ?: "Failed to fetch answers"
                                                )
                                        },
                                        ifRight = { list ->
                                                list.items.map { fetched ->
                                                        UserAnswer(
                                                                id = fetched.id,
                                                                created =
                                                                        kotlinx.datetime.Instant
                                                                                .parse(
                                                                                        fetched.created
                                                                                ),
                                                                updated =
                                                                        kotlinx.datetime.Instant
                                                                                .parse(
                                                                                        fetched.updated
                                                                                ),
                                                                userId = fetched.userId,
                                                                questionId = fetched.questionId,
                                                                answerText = fetched.answerText
                                                        )
                                                }
                                        }
                                )
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "fetch user answers",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        override suspend fun submitQuestionnaireResponse(
                questionId: String,
                answer: String
        ): Result<UserAnswer> {
                return runCatching {
                                // TODO: Restore implementation
                                throw AppException.Unknown("Not implemented (restoring)")
                        }
                        .fold(
                                onSuccess = { Result.Success(it) },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError(
                                                        "submit response",
                                                        it as Exception
                                                )
                                        )
                                }
                        )
        }

        override suspend fun getMatches(): Result<List<Match>> =
                runCatching {
                                val model = pocketBase.authStore.model
                                val currentUserId =
                                        (model as? RecordModel)?.id
                                                ?: (model as? kotlinx.serialization.json.JsonObject)
                                                        ?.get("id")
                                                        ?.jsonPrimitive
                                                        ?.content
                                                        ?: throw AppException.Unknown(
                                                        "Not authenticated"
                                                )

                                val matchesResult =
                                        pocketBase
                                                .collection(DatabaseCollections.M_MATCHES)
                                                .getList(
                                                        QueryOptions(
                                                                filter =
                                                                        "user_id='$currentUserId' || matched_user_id='$currentUserId'",
                                                                sort = "-match_score"
                                                        )
                                                )

                                fun getString(obj: JsonObject, key: String): String =
                                        obj[key]?.jsonPrimitive?.content ?: ""

                                val matches =
                                        matchesResult.items.map { item ->
                                                val json = item.jsonObject

                                                val uid = getString(json, "user_id")
                                                val mid = getString(json, "matched_user_id")
                                                val otherUserId =
                                                        if (uid == currentUserId) mid else uid

                                                var match =
                                                        Match(
                                                                id = getString(json, "id"),
                                                                userId = uid,
                                                                matchedUserId = mid,
                                                                matchScore =
                                                                        json["match_score"]
                                                                                ?.jsonPrimitive
                                                                                ?.content
                                                                                ?.toIntOrNull()
                                                                                ?: 0,
                                                                status =
                                                                        when (getString(
                                                                                        json,
                                                                                        "status"
                                                                                )
                                                                        ) {
                                                                                "accepted" ->
                                                                                        MatchStatus
                                                                                                .ACCEPTED
                                                                                "rejected" ->
                                                                                        MatchStatus
                                                                                                .REJECTED
                                                                                else ->
                                                                                        MatchStatus
                                                                                                .PENDING
                                                                        }
                                                        )

                                                match to otherUserId
                                        }

                                if (matches.isEmpty()) {
                                        Result.Success(emptyList())
                                } else {
                                        val otherUserIds = matches.map { it.second }.distinct()
                                        val profileFilter =
                                                otherUserIds.joinToString(" || ") { "userId='$it'" }

                                        val profilesResult =
                                                pocketBase
                                                        .collection(DatabaseCollections.S_PROFILES)
                                                        .getList(
                                                                QueryOptions(
                                                                        filter = profileFilter,
                                                                        perPage =
                                                                                otherUserIds.size +
                                                                                        5
                                                                )
                                                        )

                                        val profileMap =
                                                profilesResult.items.associate { item ->
                                                        val json = item.jsonObject
                                                        val pUserId = getString(json, "userId")

                                                        val rawBirthDate =
                                                                getString(json, "birthDate")
                                                        val birthDateStr =
                                                                if (rawBirthDate.length >= 10)
                                                                        rawBirthDate.substring(
                                                                                0,
                                                                                10
                                                                        )
                                                                else "2000-01-01"

                                                        val profile =
                                                                Profile(
                                                                        id = getString(json, "id"),
                                                                        created =
                                                                                (getString(
                                                                                                json,
                                                                                                "created"
                                                                                        ))
                                                                                        .parsePocketBaseInstantOr(),
                                                                        updated =
                                                                                (getString(
                                                                                                json,
                                                                                                "updated"
                                                                                        ))
                                                                                        .parsePocketBaseInstantOr(),
                                                                        userId = pUserId,
                                                                        firstName =
                                                                                getString(
                                                                                        json,
                                                                                        "firstName"
                                                                                ),
                                                                        lastName =
                                                                                getString(
                                                                                        json,
                                                                                        "lastName"
                                                                                ),
                                                                        birthDate =
                                                                                kotlinx.datetime
                                                                                        .LocalDate
                                                                                        .parse(
                                                                                                birthDateStr
                                                                                        ),
                                                                        bio =
                                                                                getString(
                                                                                        json,
                                                                                        "bio"
                                                                                ),
                                                                        location =
                                                                                getString(
                                                                                        json,
                                                                                        "location"
                                                                                ),
                                                                        seeking =
                                                                                when (getString(
                                                                                                json,
                                                                                                "seeking"
                                                                                        )
                                                                                ) {
                                                                                        "Friendship" ->
                                                                                                SeekingStatus
                                                                                                        .FRIENDSHIP
                                                                                        "Relationship" ->
                                                                                                SeekingStatus
                                                                                                        .RELATIONSHIP
                                                                                        else ->
                                                                                                SeekingStatus
                                                                                                        .BOTH
                                                                                },
                                                                        profilePicture =
                                                                                getString(
                                                                                        json,
                                                                                        "profilePicture"
                                                                                ),
                                                                        photos =
                                                                                json["photos"]
                                                                                        ?.jsonArray
                                                                                        ?.map {
                                                                                                it.jsonPrimitive
                                                                                                        .content
                                                                                        }
                                                                                        ?: emptyList(),
                                                                        aboutMe =
                                                                                getString(
                                                                                        json,
                                                                                        "aboutMe"
                                                                                ),
                                                                        height =
                                                                                json["height"]
                                                                                        ?.jsonPrimitive
                                                                                        ?.content
                                                                                        ?.toDoubleOrNull(),
                                                                        occupation =
                                                                                getString(
                                                                                        json,
                                                                                        "occupation"
                                                                                ),
                                                                        education =
                                                                                getString(
                                                                                        json,
                                                                                        "education"
                                                                                ),
                                                                        interests =
                                                                                json["interests"]
                                                                                        ?.jsonArray
                                                                                        ?.map {
                                                                                                it.jsonPrimitive
                                                                                                        .content
                                                                                        }
                                                                                        ?: emptyList()
                                                                )

                                                        pUserId to profile
                                                }

                                        val finalMatches =
                                                matches.map { (match, otherId) ->
                                                        match.copy(
                                                                expand =
                                                                        MatchExpand(
                                                                                matchedUserProfile =
                                                                                        profileMap[
                                                                                                otherId]
                                                                        )
                                                        )
                                                }

                                        Result.Success(finalMatches)
                                }
                        }
                        .fold(
                                onSuccess = { it },
                                onFailure = {
                                        Result.Error(
                                                mapPocketBaseError("fetch matches", it as Exception)
                                        )
                                }
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
        fun mapRecordToConversation(
                recordElement: kotlinx.serialization.json.JsonElement
        ): Conversation {
                val record = recordElement.jsonObject

                fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
                fun getBoolean(key: String): Boolean =
                        record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

                return Conversation(
                        id = getString("id"),
                        conversationType =
                                when (getString("type")) {
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
                        isArchived = getBoolean("is_archived"),
                        created = getString("created").parsePocketBaseInstantOr(),
                        updated = getString("updated").parsePocketBaseInstantOr()
                )
        }

        fun mapRecordToParticipant(
                recordElement: kotlinx.serialization.json.JsonElement
        ): ConversationParticipant {
                val record = recordElement.jsonObject

                fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
                fun getInt(key: String): Int =
                        record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                fun getBoolean(key: String): Boolean =
                        record[key]?.jsonPrimitive?.content?.toBoolean() ?: false

                return ConversationParticipant(
                        id = getString("id"),
                        conversationId = getString("conversation_id"),
                        userId = getString("user_id"),
                        role =
                                when (getString("role")) {
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

        fun mapRecordToMessage(recordElement: kotlinx.serialization.json.JsonElement, readMessageIds: Set<String> = emptySet()): Message {
                val record = recordElement.jsonObject

                fun getString(key: String): String = record[key]?.jsonPrimitive?.content ?: ""
                fun getInt(key: String): Int =
                        record[key]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
                fun getList(key: String): List<String> =
                        record[key]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

                val expand = record["expand"]?.jsonObject
                val replyToMessageJson = expand?.get("reply_to_message_id")?.jsonObject
                val replyToMessage = if (replyToMessageJson != null) {
                        mapRecordToMessage(replyToMessageJson, readMessageIds)
                } else {
                        null
                }
                
                val messageId = getString("id")

                return Message(
                        id = messageId,
                        collectionId = getString("collectionId"),
                        conversationId = getString("conversation_id"),
                        senderId = getString("sender_id"),
                        content = getString("content"),
                        messageType =
                                when (getString("type")) {
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
                        isRead = messageId in readMessageIds,
                        readAt = if (messageId in readMessageIds) {
                            // We don't have the exact readAt from the receipt here, 
                            // so we can either fetch it or just use the existence of the receipt.
                            // For now, we'll just use the existence.
                            null 
                        } else {
                            null
                        },
                        replyToMessageId =
                                getString("reply_to_message_id").takeIf { it.isNotEmpty() },
                        replyToMessage = replyToMessage,
                        threadRootId = getString("thread_root_id").takeIf { it.isNotEmpty() },
                        threadDepth = getInt("thread_depth"),
                        threadReplyCount = 0,
                        created = getString("created").parsePocketBaseInstantOr(),
                        updated = getString("updated").parsePocketBaseInstantOr()
                )
        }

        // Cache for settings
        private var cachedSettings: MessagingSettings? = null
        private var lastSettingsFetch: kotlinx.datetime.Instant? = null
        private val SETTINGS_CACHE_TTL = 5 * 60 * 1000L // 5 minutes

        override suspend fun getGlobalSettings(): Result<MessagingSettings> =
                runCatching {
                        // Check cache
                        val now = Clock.System.now()
                        if (cachedSettings != null && lastSettingsFetch != null) {
                                val age =
                                        now.toEpochMilliseconds() -
                                                lastSettingsFetch!!.toEpochMilliseconds()
                                if (age < SETTINGS_CACHE_TTL) {
                                        return@runCatching Result.Success(cachedSettings!!)
                                }
                        }

                        rateLimiter.acquireToken()
                        val model = pocketBase.authStore.model
                        val userId =
                                (model as? RecordModel)?.id
                                        ?: (model as? kotlinx.serialization.json.JsonObject)?.get(
                                                        "id"
                                                )
                                                ?.jsonPrimitive
                                                ?.content
                                                ?: return@runCatching Result.Error(
                                                AppException.Unknown("Not authenticated")
                                        )

                        // Fetch user properties
                        val userProps =
                                pocketBase
                                        .collection(DatabaseCollections.T_USER_PROPERTY)
                                        .getList(QueryOptions(filter = "user_id='$userId'"))
                                        .items
                                        .associate {
                                                val key = it["key"]?.jsonPrimitive?.content ?: ""
                                                val value =
                                                        it["value"]?.jsonPrimitive?.content ?: ""
                                                key to value
                                        }

                        // Defaults
                        val readReceipts =
                                userProps["messaging.read_receipts_enabled"]?.toBoolean() ?: true
                        val typingStatus =
                                userProps["messaging.typing_status_enabled"]?.toBoolean() ?: true

                        val settings =
                                MessagingSettings(
                                        readReceiptsEnabled = readReceipts,
                                        typingStatusEnabled = typingStatus
                                )

                        // Update cache
                        cachedSettings = settings
                        lastSettingsFetch = now

                        Result.Success(settings)
                }
                        .getOrElse {
                                Result.Error(mapPocketBaseError("get settings", it as Exception))
                        }

        override suspend fun updateGlobalSettings(settings: MessagingSettings): Result<Unit> =
                writeMutex.withLock {
                        rateLimiter.acquireToken()
                        runCatching {
                                        val model = pocketBase.authStore.model
                                        val userId =
                                                (model as? RecordModel)?.id
                                                        ?: (model as?
                                                                        kotlinx.serialization.json.JsonObject)
                                                                ?.get("id")
                                                                ?.jsonPrimitive
                                                                ?.content
                                                                ?: return@runCatching Result.Error(
                                                                AppException.Unknown(
                                                                        "Not authenticated"
                                                                )
                                                        )

                                        // Helper to upsert
                                        suspend fun upsertProperty(key: String, value: String) {
                                                // Check if exists
                                                val existing =
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .T_USER_PROPERTY
                                                                )
                                                                .getList(
                                                                        QueryOptions(
                                                                                filter =
                                                                                        "user_id='$userId' && key='$key'",
                                                                                perPage = 1
                                                                        )
                                                                )
                                                                .items
                                                                .firstOrNull()

                                                if (existing != null) {
                                                        val existingId =
                                                                existing["id"]
                                                                        ?.jsonPrimitive
                                                                        ?.content
                                                                        ?: return
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .T_USER_PROPERTY
                                                                )
                                                                .update(
                                                                        existingId,
                                                                        mapOf("value" to value)
                                                                )
                                                } else {
                                                        pocketBase
                                                                .collection(
                                                                        DatabaseCollections
                                                                                .T_USER_PROPERTY
                                                                )
                                                                .create(
                                                                        mapOf(
                                                                                "user_id" to userId,
                                                                                "key" to key,
                                                                                "value" to value
                                                                        )
                                                                )
                                                }
                                        }

                                        upsertProperty(
                                                "messaging.read_receipts_enabled",
                                                settings.readReceiptsEnabled.toString()
                                        )
                                        upsertProperty(
                                                "messaging.typing_status_enabled",
                                                settings.typingStatusEnabled.toString()
                                        )

                                        Unit
                                }
                                .fold(
                                        onSuccess = { Result.Success(Unit) },
                                        onFailure = {
                                                Result.Error(
                                                        mapPocketBaseError(
                                                                "update settings",
                                                                it as Exception
                                                        )
                                                )
                                        }
                                )
                }
}
