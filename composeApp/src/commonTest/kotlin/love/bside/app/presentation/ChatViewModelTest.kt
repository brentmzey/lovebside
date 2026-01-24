package love.bside.app.presentation

import kotlin.test.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.domain.models.*
import love.bside.app.domain.repository.AttachmentData
import love.bside.app.domain.repository.MessagingRepository

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private lateinit var viewModel: ChatViewModel
    private lateinit var fakeRepository: FakeMessagingRepository
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeMessagingRepository()
        viewModel = ChatViewModel(fakeRepository, "me")
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadConversation loads history and subscribes`() =
            runTest(testDispatcher) {
                val convId = "conv1"

                // 1. Load
                viewModel.loadConversation(convId)
                testScheduler.advanceUntilIdle()

                // Verify loaded messages
                assertEquals(3, viewModel.messages.value.size)
                assertEquals(
                        "msg1",
                        viewModel.messages.value.first().id
                ) // Oldest first (reversed history)

                // 2. Subscribe and Receive Update
                // Emit a new message via flow
                val newMessage =
                        createMessage("msg4", convId, "them", "New Message", Clock.System.now())
                fakeRepository.emitMessage(convId, newMessage)
                testScheduler.advanceUntilIdle()

                assertEquals(4, viewModel.messages.value.size)
                assertEquals("msg4", viewModel.messages.value.last().id)

                // 3. Verify Read Receipt Triggered
                // msg4 is from "them", so it should trigger markAsRead
                assertTrue(fakeRepository.markAsReadCalled.contains(convId))
            }

    @Test
    fun `realtime UPDATE updates existing message and triggers read receipt`() =
            runTest(testDispatcher) {
                val convId = "conv1"
                viewModel.loadConversation(convId)
                testScheduler.advanceUntilIdle()

                // Initial state
                assertEquals(3, viewModel.messages.value.size)
                val originalMsg = viewModel.messages.value.find { it.id == "msg2" }
                assertNotNull(originalMsg)
                assertNull(originalMsg.readAt)

                // Emit UPDATE for msg2 (e.g. it was read)
                val updatedMsg = originalMsg.copy(readAt = Clock.System.now())
                fakeRepository.emitMessage(convId, updatedMsg)
                testScheduler.advanceUntilIdle()

                // Verify update in list
                val msgCheck = viewModel.messages.value.find { it.id == "msg2" }
                assertNotNull(msgCheck)
                assertNotNull(msgCheck.readAt)

                // Verify markAsRead was called again (since it was unread before)
                fakeRepository.markAsReadCalled.clear()

                // Receive another update that IS unread
                val unreadUpdate =
                        createMessage("msgNew", convId, "them", "Unread", Clock.System.now())
                fakeRepository.emitMessage(convId, unreadUpdate)
                testScheduler.advanceUntilIdle()

                assertTrue(fakeRepository.markAsReadCalled.contains(convId))
            }

    @Test
    fun `toggleReaction calls repository`() =
            runTest(testDispatcher) {
                val convId = "conv1"
                viewModel.loadConversation(convId)
                testScheduler.advanceUntilIdle()

                val msgId = "msg2" // "them" sent this
                
                // Toggle ON
                viewModel.toggleReaction(msgId, "👍")
                testScheduler.advanceUntilIdle()
                
                assertTrue(fakeRepository.addReactionCalled.contains(msgId to "👍"))
                
                // Simulate update coming back with reaction
                val originalMsg = viewModel.messages.value.find { it.id == msgId }!!
                val updatedMsg = originalMsg.copy(
                    reactions = mapOf("👍" to listOf("me"))
                )
                fakeRepository.emitMessage(convId, updatedMsg)
                testScheduler.advanceUntilIdle()
                
                // Verify UI state
                val msgCheck = viewModel.messages.value.find { it.id == msgId }!!
                assertTrue(msgCheck.reactions["👍"]?.contains("me") == true)

                // Toggle OFF
                viewModel.toggleReaction(msgId, "👍")
                testScheduler.advanceUntilIdle()
                assertTrue(fakeRepository.removeReactionCalled.contains(msgId to "👍"))
            }
}

// Minimal Fake Implementation
class FakeMessagingRepository : MessagingRepository {

    val markAsReadCalled = mutableListOf<String>()
    val addReactionCalled = mutableListOf<Pair<String, String>>()
    val removeReactionCalled = mutableListOf<Pair<String, String>>()

    private val messageFlow = MutableSharedFlow<Message>()
    private val typingFlow = MutableSharedFlow<TypingStatus>()

    suspend fun emitMessage(convId: String, msg: Message) {
        messageFlow.emit(msg)
    }

    suspend fun emitTyping(convId: String, status: TypingStatus) {
        typingFlow.emit(status)
    }

    override suspend fun getMessages(
            conversationId: String,
            page: Int,
            perPage: Int
    ): Result<List<Message>> {
        // Return 3 dummy messages
        val list =
                listOf(
                        createMessage("msg3", conversationId, "me", "Hi", Clock.System.now()),
                        createMessage("msg2", conversationId, "them", "Hello", Clock.System.now()),
                        createMessage("msg1", conversationId, "me", "Yo", Clock.System.now())
                ) // Sorted by sentAt desc usually in repo
        return Result.Success(list)
    }

    // Helper for not impl error
    private fun notImpl(): Result.Error = Result.Error(AppException.Unknown("Not impl"))

    override fun subscribeToConversation(conversationId: String): Flow<Message> = messageFlow
    override fun subscribeToTypingIndicators(conversationId: String): Flow<TypingStatus> =
            typingFlow

    override suspend fun markAsRead(conversationId: String): Result<Unit> {
        markAsReadCalled.add(conversationId)
        return Result.Success(Unit)
    }

    override suspend fun setTypingStatus(conversationId: String, isTyping: Boolean): Result<Unit> {
        return Result.Success(Unit)
    }
    
    override suspend fun addReaction(messageId: String, reaction: String): Result<Unit> {
        addReactionCalled.add(messageId to reaction)
        return Result.Success(Unit)
    }

    override suspend fun removeReaction(messageId: String, reaction: String): Result<Unit> {
        removeReactionCalled.add(messageId to reaction)
        return Result.Success(Unit)
    }

    // --- Unused methods stubbed ---
    override suspend fun getConversations(userId: String) =
            Result.Success(emptyList<Conversation>())
    override suspend fun getConversation(conversationId: String) = notImpl()
    override suspend fun createDirectConversation(participantIds: List<String>) = notImpl()
    override suspend fun createGroupConversation(name: String, participantIds: List<String>) =
            notImpl()
    override suspend fun getParticipants(conversationId: String) =
            Result.Success(emptyList<ConversationParticipant>())
    override suspend fun addParticipants(conversationId: String, userIds: List<String>) =
            Result.Success(Unit)
    override suspend fun removeParticipant(conversationId: String, userId: String) =
            Result.Success(Unit)
    override suspend fun updateParticipantSettings(
            conversationId: String,
            isMuted: Boolean?,
            isPinned: Boolean?
    ) = Result.Success(Unit)
    override suspend fun sendMessage(
            conversationId: String,
            content: String,
            replyToMessageId: String?,
            attachments: List<AttachmentData>?
    ) = notImpl()
    override suspend fun deleteMessage(messageId: String) = Result.Success(Unit)
    override suspend fun getReplies(messageId: String) = Result.Success(emptyList<Message>())
    override suspend fun getThreadRoot(messageId: String) = notImpl()
    override suspend fun getFullThread(rootMessageId: String) = Result.Success(emptyList<Message>())
    override suspend fun countReplies(messageId: String) = Result.Success(0)
    override suspend fun searchMessages(query: String, conversationId: String) =
            Result.Success(emptyList<Message>())
    override suspend fun getMessagesAfter(conversationId: String, timestamp: Instant, limit: Int) =
            Result.Success(emptyList<Message>())
    override suspend fun getMessagesBefore(conversationId: String, timestamp: Instant, limit: Int) =
            Result.Success(emptyList<Message>())
    override suspend fun getQuestionnaire() = Result.Success(emptyList<ProustQuestionnaire>())
    override suspend fun getUserAnswers() = Result.Success(emptyList<UserAnswer>())
    override suspend fun submitQuestionnaireResponse(questionId: String, answer: String) = notImpl()
    override suspend fun getMatches() = Result.Success(emptyList<Match>())
    override suspend fun getGlobalSettings() =
            Result.Success(
                    MessagingSettings(readReceiptsEnabled = true, typingStatusEnabled = true)
            )
    override suspend fun updateGlobalSettings(settings: MessagingSettings) = Result.Success(Unit)
}

fun createMessage(id: String, convId: String, senderId: String, content: String, sentAt: Instant) =
        Message(
                id = id,
                collectionId = "col_messages",
                conversationId = convId,
                senderId = senderId,
                content = content,
                messageType = MessageType.TEXT,
                attachments = emptyList(),
                sentAt = sentAt,
                editedAt = null,
                deletedAt = null,
                readByCount = 0,
                isRead = false,
                readAt = null,
                replyToMessageId = null,
                replyToMessage = null,
                threadRootId = null,
                threadDepth = 0,
                threadReplyCount = 0,
                reactions = emptyMap(),
                created = sentAt,
                updated = sentAt
        )
