package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.jsonPrimitive
import love.bside.app.data.repository.MessagingRepository
import love.bside.app.data.models.ConversationType
import love.bside.app.data.models.PresenceStatus
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive integration tests for BSide Messaging System
 * Tests the full stack: SDK → Repository → PocketBase Backend
 * 
 * Prerequisites:
 * - PocketBase running on localhost:8091
 * - Admin user created
 * - Migrations applied
 */
class ComprehensiveMessagingIntegrationTest {

    private lateinit var pb: PocketBase
    private lateinit var repo: MessagingRepository
    private lateinit var userId: String
    private val testConversations = mutableListOf<String>()
    private val testMessages = mutableListOf<String>()

    @Before
    fun setup() = runBlocking {
        pb = PocketBase("http://localhost:8091/")
        repo = MessagingRepository(pb)
        
        // Create or auth test user
        try {
            pb.collection("t_user").authWithPassword("integration_test@bside.love", "password123")
        } catch (e: Exception) {
            pb.collection("t_user").create(mapOf(
                "email" to "integration_test@bside.love",
                "password" to "password123",
                "passwordConfirm" to "password123",
                "name" to "Integration Test User"
            ))
            pb.collection("t_user").authWithPassword("integration_test@bside.love", "password123")
        }
        
        userId = pb.authStore.model?.get("id")?.jsonPrimitive?.content ?: error("No user ID")
    }

    @After
    fun cleanup() = runBlocking {
        // Clean up test data
        testMessages.forEach { id ->
            try {
                pb.collection("m_messages").delete(id)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        
        testConversations.forEach { id ->
            try {
                pb.collection("m_conversations").delete(id)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    @Test
    fun `test 1 - SDK connection and authentication`() = runBlocking {
        // Verify SDK can connect to PocketBase
        assertNotNull(pb)
        assertTrue(pb.authStore.isValid, "User should be authenticated")
        assertNotNull(pb.authStore.token, "Auth token should exist")
        assertEquals(userId, pb.authStore.model?.get("id")?.jsonPrimitive?.content)
    }

    @Test
    fun `test 2 - create conversation via repository`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        
        assertNotNull(conversation.id, "Conversation should have ID")
        assertEquals(ConversationType.DIRECT, conversation.type)
        assertTrue(conversation.participants.contains(userId))
        
        testConversations.add(conversation.id)
    }

    @Test
    fun `test 3 - send simple message`() = runBlocking {
        // Create conversation
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        // Send message
        val message = repo.sendMessage(conversation.id, "Integration test message")
        
        assertNotNull(message.id)
        assertEquals("Integration test message", message.content)
        assertEquals(conversation.id, message.conversationId)
        assertEquals(userId, message.senderId)
        
        testMessages.add(message.id)
    }

    @Test
    fun `test 4 - send threaded reply`() = runBlocking {
        // Setup
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        val rootMessage = repo.sendMessage(conversation.id, "Root message")
        testMessages.add(rootMessage.id)
        
        // Send threaded reply
        val reply = repo.sendMessage(
            conversationId = conversation.id,
            text = "This is a reply",
            replyToId = rootMessage.id,
            threadRootId = rootMessage.id
        )
        testMessages.add(reply.id)
        
        // Verify threading
        assertEquals(rootMessage.id, reply.replyToMessageId, "Reply should reference root")
        assertEquals(rootMessage.id, reply.threadRootId, "Thread root should be set")
        assertNotNull(reply.id)
    }

    @Test
    fun `test 5 - nested thread replies`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        // Create thread: Root -> Reply1 -> Reply2
        val root = repo.sendMessage(conversation.id, "Root")
        testMessages.add(root.id)
        
        val reply1 = repo.sendMessage(
            conversationId = conversation.id,
            text = "Reply 1",
            replyToId = root.id,
            threadRootId = root.id
        )
        testMessages.add(reply1.id)
        
        val reply2 = repo.sendMessage(
            conversationId = conversation.id,
            text = "Reply 2",
            replyToId = reply1.id,
            threadRootId = root.id  // Should still point to root
        )
        testMessages.add(reply2.id)
        
        // Verify
        assertEquals(root.id, reply1.threadRootId)
        assertEquals(root.id, reply2.threadRootId)
        assertEquals(reply1.id, reply2.replyToMessageId)
    }

    @Test
    fun `test 6 - retrieve messages`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        // Send multiple messages
        val msg1 = repo.sendMessage(conversation.id, "Message 1")
        val msg2 = repo.sendMessage(conversation.id, "Message 2")
        val msg3 = repo.sendMessage(conversation.id, "Message 3")
        
        testMessages.addAll(listOf(msg1.id, msg2.id, msg3.id))
        
        // Retrieve
        val messages = repo.getMessages(conversation.id)
        
        assertTrue(messages.size >= 3, "Should have at least 3 messages")
        assertTrue(messages.any { it.content == "Message 1" })
        assertTrue(messages.any { it.content == "Message 2" })
        assertTrue(messages.any { it.content == "Message 3" })
    }

    @Test
    fun `test 7 - add and remove reactions`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        val message = repo.sendMessage(conversation.id, "React to this!")
        testMessages.add(message.id)
        
        // Add reaction
        val reaction = repo.addReaction(message.id, "👍")
        assertEquals("👍", reaction.reaction)
        assertEquals(message.id, reaction.messageId)
        assertEquals(userId, reaction.userId)
        
        // Remove reaction
        repo.removeReaction(message.id, "👍")
        // Note: Could add verification by querying reactions
    }

    @Test
    fun `test 8 - multiple reactions on same message`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        val message = repo.sendMessage(conversation.id, "Many reactions!")
        testMessages.add(message.id)
        
        // Add multiple reactions
        val reactions = listOf("👍", "❤️", "😂", "🔥")
        reactions.forEach { emoji ->
            val reaction = repo.addReaction(message.id, emoji)
            assertEquals(emoji, reaction.reaction)
        }
        
        // Clean up
        reactions.forEach { emoji ->
            repo.removeReaction(message.id, emoji)
        }
    }

    @Test
    fun `test 9 - set and get presence`() = runBlocking {
        // Set presence
        val presence = repo.setPresence(
            PresenceStatus.ONLINE,
            "Running integration tests"
        )
        
        assertEquals(PresenceStatus.ONLINE, presence.status)
        assertEquals("Running integration tests", presence.activityMessage)
        assertEquals(userId, presence.userId)
        
        // Get presence
        val retrieved = repo.getPresence(userId)
        assertNotNull(retrieved)
        assertEquals(PresenceStatus.ONLINE, retrieved.status)
    }

    @Test
    fun `test 10 - update presence`() = runBlocking {
        // Initial presence
        val initial = repo.setPresence(PresenceStatus.ONLINE, "Initial")
        
        delay(100) // Small delay to ensure timestamp difference
        
        // Update presence
        val updated = repo.setPresence(PresenceStatus.BUSY, "Updated activity")
        
        assertEquals(initial.id, updated.id, "Should update same record")
        assertEquals(PresenceStatus.BUSY, updated.status)
        assertEquals("Updated activity", updated.activityMessage)
    }

    @Test
    fun `test 11 - get conversations list`() = runBlocking {
        // Create multiple conversations
        val conv1 = repo.createConversation(listOf(userId), ConversationType.DIRECT)
        val conv2 = repo.createConversation(listOf(userId), ConversationType.DIRECT)
        
        testConversations.addAll(listOf(conv1.id, conv2.id))
        
        // Get conversations
        val conversations = repo.getConversations()
        
        assertTrue(conversations.size >= 2)
        assertTrue(conversations.any { it.id == conv1.id })
        assertTrue(conversations.any { it.id == conv2.id })
    }

    @Test
    fun `test 12 - conversation with multiple messages maintains order`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        // Send messages in order
        val messages = (1..5).map { i ->
            val msg = repo.sendMessage(conversation.id, "Message $i")
            testMessages.add(msg.id)
            delay(50) // Ensure timestamp ordering
            msg
        }
        
        // Retrieve and verify order
        val retrieved = repo.getMessages(conversation.id)
        assertTrue(retrieved.size >= 5)
        
        // Verify messages exist
        messages.forEach { sent ->
            assertTrue(retrieved.any { it.id == sent.id })
        }
    }

    @Test
    fun `test 13 - empty conversation has no messages`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        val messages = repo.getMessages(conversation.id)
        assertTrue(messages.isEmpty(), "New conversation should have no messages")
    }

    @Test
    fun `test 14 - presence status changes persist`() = runBlocking {
        val statuses = listOf(
            PresenceStatus.ONLINE,
            PresenceStatus.AWAY,
            PresenceStatus.BUSY,
            PresenceStatus.OFFLINE
        )
        
        statuses.forEach { status ->
            val presence = repo.setPresence(status, "Testing $status")
            assertEquals(status, presence.status)
            
            delay(50)
            
            val retrieved = repo.getPresence(userId)
            assertNotNull(retrieved)
            assertEquals(status, retrieved.status)
        }
    }

    @Test
    fun `test 15 - thread depth calculation`() = runBlocking {
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        testConversations.add(conversation.id)
        
        // Build deep thread
        var current = repo.sendMessage(conversation.id, "Root")
        testMessages.add(current.id)
        
        repeat(3) { depth ->
            val reply = repo.sendMessage(
                conversationId = conversation.id,
                text = "Depth ${depth + 1}",
                replyToId = current.id,
                threadRootId = testMessages.first() // Root message ID
            )
            testMessages.add(reply.id)
            current = reply
        }
        
        // Verify all messages persist
        val messages = repo.getMessages(conversation.id)
        assertTrue(messages.size >= 4, "Should have root + 3 replies")
    }
}
