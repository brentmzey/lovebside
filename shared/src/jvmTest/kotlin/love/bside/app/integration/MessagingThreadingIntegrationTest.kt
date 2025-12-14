package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.AfterClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for messaging with threading support
 * Tests against live PocketHost instance: https://bside.pockethost.io/
 */
class MessagingThreadingIntegrationTest {

    // Companion object for static setup
    companion object {
        private lateinit var pocketBase: PocketBase
        private lateinit var repository: PocketBaseMessagingRepository
        private var testUserId: String? = null

        @JvmStatic
        @BeforeClass
        fun setup() {
            // Use local for development, or switch to "https://bside.pockethost.io/" for production testing
            pocketBase = PocketBase("http://127.0.0.1:8090") 
            repository = PocketBaseMessagingRepository(pocketBase)

            // Authenticate with test user (Alice)
            runBlocking {
                try {
                    val authResult = pocketBase.collection("users").authWithPassword(
                        "alice@bside.love",
                        "password123"
                    )
                    // Depending on SDK, model access might differ.
                    // AuthResponse has model as JsonObject? Or we check authStore.
                    // Let's assume authStore.model is correct type or JsonObject
                    val model = pocketBase.authStore.model
                    testUserId = (model as? io.pocketbase.models.RecordModel)?.id 
                        ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                    
                    println("✓ Authenticated as user: $testUserId")
                } catch (e: Exception) {
                    println("⚠ Authentication failed: ${e.message}")
                    println("  Note: Make sure 'alice@bside.love' user exists with password 'password123'")
                    // If auth fails, subsequent tests will fail
                }
            }
        }
    }

    private var testConversationId: String? = null
    private val testMessageIds = mutableListOf<String>()

    @Before
    fun setupTest() = runTest {
        testMessageIds.clear()
        testConversationId = null
    }

    @After
    fun cleanupTest() = runTest {
        // Clean up test messages and conversation
        try {
            testMessageIds.forEach { messageId ->
                runCatching {
                    pocketBase.collection("m_messages").delete(messageId)
                }
            }
            
            testConversationId?.let { convId ->
                runCatching {
                    // Delete participants first
                    val result = pocketBase.collection("m_conversation_participants")
                        .getList(io.pocketbase.models.QueryOptions(
                            filter = "conversationId='$convId'"
                        ))
                    
                    // Result is ListResult<JsonObject> directly
                    result.items.forEach { participantRecord ->
                        // participantRecord is JsonObject
                        val id = participantRecord["id"]?.toString()?.trim('"') ?: return@forEach
                        pocketBase.collection("m_conversation_participants").delete(id)
                    }
                    
                    // Then delete conversation
                    pocketBase.collection("m_conversations").delete(convId)
                }
            }
        } catch (e: Exception) {
            println("  Cleanup warning: ${e.message}")
        }
    }

    @Test
    fun testGetRepliesWithSimpleThread() = runTest {
         // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, "RelayHost")) // Use existing user or same user? 
        // Test uses same user twice in original code: listOf(testUserId!!, testUserId!!) 
        // We will stick to original logic if possible, but creating direct convo with oneself might fail if schema forbids.
        // Assuming it works for test.
        val convoResult = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(convoResult is Result.Success, "Failed to create conversation")
        testConversationId = (convoResult as Result.Success).data.id

        // Send root message
        val rootMessage = repository.sendMessage(
            conversationId = testConversationId!!,
            content = "Root message"
        )
        assertTrue(rootMessage is Result.Success)
        val rootId = (rootMessage as Result.Success).data.id
        testMessageIds.add(rootId)

        // Send 3 replies
        for (i in 1..3) {
            val reply = repository.sendMessage(
                conversationId = testConversationId!!,
                content = "Reply $i",
                replyToMessageId = rootId
            )
            assertTrue(reply is Result.Success, "Failed to send reply $i")
            testMessageIds.add((reply as Result.Success).data.id)
        }


        // Test getReplies
        val replies = repository.getReplies(rootId)
        assertTrue(replies is Result.Success, "Failed to get replies")
        assertEquals(3, (replies as Result.Success).data.size, "Should have 3 replies")
        
        // Verify all replies reference the root
        replies.data.forEach { reply ->
            assertEquals(rootId, reply.replyToMessageId, "Reply should reference root message")
        }
        
        println("✓ getReplies test passed: Found ${replies.data.size} replies")
    }

    @Test
    fun `test getThreadRoot with nested replies`() = runTest {
        // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(conversation is Result.Success)
        testConversationId = (conversation as Result.Success).data.id

        // Create nested thread: Root -> Reply1 -> Reply2 -> Reply3
        val rootMessage = repository.sendMessage(testConversationId!!, "Root")
        assertTrue(rootMessage is Result.Success)
        val rootId = (rootMessage as Result.Success).data.id
        testMessageIds.add(rootId)

        var previousId = rootId
        for (i in 1..3) {
            val reply = repository.sendMessage(
                conversationId = testConversationId!!,
                content = "Nested reply $i",
                replyToMessageId = previousId
            )
            assertTrue(reply is Result.Success)
            previousId = (reply as Result.Success).data.id
            testMessageIds.add(previousId)
        }

        // Get root from the deepest reply
        val threadRoot = repository.getThreadRoot(previousId)
        assertTrue(threadRoot is Result.Success, "Failed to get thread root")
        assertEquals(rootId, (threadRoot as Result.Success).data.id, "Should find the root message")
        assertEquals("Root", threadRoot.data.content, "Root message content should match")
        
        println("✓ getThreadRoot test passed: Found root from depth 3")
    }

    @Test
    fun `test getFullThread with branching replies`() = runTest {
        // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(conversation is Result.Success)
        testConversationId = (conversation as Result.Success).data.id

        // Create branching thread:
        //      Root
        //     /    \
        //   R1      R2
        //   |       |
        //  R1.1    R2.1

        val rootMessage = repository.sendMessage(testConversationId!!, "Root")
        assertTrue(rootMessage is Result.Success)
        val rootId = (rootMessage as Result.Success).data.id
        testMessageIds.add(rootId)

        // Branch 1
        val reply1 = repository.sendMessage(testConversationId!!, "Reply 1", rootId)
        assertTrue(reply1 is Result.Success)
        val r1Id = (reply1 as Result.Success).data.id
        testMessageIds.add(r1Id)

        val reply11 = repository.sendMessage(testConversationId!!, "Reply 1.1", r1Id)
        assertTrue(reply11 is Result.Success)
        testMessageIds.add((reply11 as Result.Success).data.id)

        // Branch 2
        val reply2 = repository.sendMessage(testConversationId!!, "Reply 2", rootId)
        assertTrue(reply2 is Result.Success)
        val r2Id = (reply2 as Result.Success).data.id
        testMessageIds.add(r2Id)

        val reply21 = repository.sendMessage(testConversationId!!, "Reply 2.1", r2Id)
        assertTrue(reply21 is Result.Success)
        testMessageIds.add((reply21 as Result.Success).data.id)

        // Get full thread
        val fullThread = repository.getFullThread(rootId)
        assertTrue(fullThread is Result.Success, "Failed to get full thread")
        assertEquals(5, (fullThread as Result.Success).data.size, "Thread should have 5 messages")
        
        // Verify root is first (after sorting by sentAt)
        assertEquals(rootId, fullThread.data.first().id, "Root should be first in sorted thread")
        
        println("✓ getFullThread test passed: Found all 5 messages in branching thread")
    }

    @Test
    fun `test countReplies`() = runTest {
        // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(conversation is Result.Success)
        testConversationId = (conversation as Result.Success).data.id

        // Send root message
        val rootMessage = repository.sendMessage(testConversationId!!, "Root")
        assertTrue(rootMessage is Result.Success)
        val rootId = (rootMessage as Result.Success).data.id
        testMessageIds.add(rootId)

        // Initially no replies
        val initialCount = repository.countReplies(rootId)
        assertTrue(initialCount is Result.Success)
        assertEquals(0, (initialCount as Result.Success).data)

        // Add 5 replies
        for (i in 1..5) {
            val reply = repository.sendMessage(testConversationId!!, "Reply $i", rootId)
            assertTrue(reply is Result.Success)
            testMessageIds.add((reply as Result.Success).data.id)
        }

        // Count should be 5
        val finalCount = repository.countReplies(rootId)
        assertTrue(finalCount is Result.Success)
        assertEquals(5, (finalCount as Result.Success).data)
        
        println("✓ countReplies test passed: Correctly counted 5 replies")
    }

    @Test
    fun `test searchMessages`() = runTest {
        // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(conversation is Result.Success)
        testConversationId = (conversation as Result.Success).data.id

        // Send messages with specific keywords
        val keywords = listOf("kotlin", "multiplatform", "messaging", "threading", "kotlin")
        keywords.forEach { keyword ->
            val message = repository.sendMessage(
                conversationId = testConversationId!!,
                content = "Test message about $keyword"
            )
            assertTrue(message is Result.Success)
            testMessageIds.add((message as Result.Success).data.id)
        }

        // Search for "kotlin"
        val searchResult = repository.searchMessages("kotlin", testConversationId!!)
        assertTrue(searchResult is Result.Success, "Search failed")
        assertEquals(2, (searchResult as Result.Success).data.size, "Should find 2 messages with 'kotlin'")
        
        // Search for "messaging"
        val messagingResult = repository.searchMessages("messaging", testConversationId!!)
        assertTrue(messagingResult is Result.Success)
        assertEquals(1, (messagingResult as Result.Success).data.size)
        
        println("✓ searchMessages test passed: Found correct messages")
    }

    @Test
    fun `test getMessagesAfter and getMessagesBefore`() = runTest {
        // Create conversation
        val conversation = repository.createDirectConversation(listOf(testUserId!!, testUserId!!))
        assertTrue(conversation is Result.Success)
        testConversationId = (conversation as Result.Success).data.id

        // Send messages with delays to ensure different timestamps
        val messages = mutableListOf<String>()
        for (i in 1..5) {
            val message = repository.sendMessage(testConversationId!!, "Message $i")
            assertTrue(message is Result.Success)
            messages.add((message as Result.Success).data.id)
            testMessageIds.add(messages.last())
            kotlinx.coroutines.delay(100) // Small delay for timestamp differentiation
        }

        // Get the middle message's timestamp
        val allMessages = repository.getMessages(testConversationId!!)
        assertTrue(allMessages is Result.Success)
        val middleMessage = (allMessages as Result.Success).data[2] // 3rd message
        val middleTimestamp = middleMessage.sentAt

        // Get messages after middle timestamp
        val afterMessages = repository.getMessagesAfter(testConversationId!!, middleTimestamp)
        assertTrue(afterMessages is Result.Success)
        assertTrue((afterMessages as Result.Success).data.size >= 2, "Should have at least 2 messages after middle")

        // Get messages before middle timestamp  
        val beforeMessages = repository.getMessagesBefore(testConversationId!!, middleTimestamp)
        assertTrue(beforeMessages is Result.Success)
        assertTrue((beforeMessages as Result.Success).data.size >= 2, "Should have at least 2 messages before middle")
        
        println("✓ getMessagesAfter/Before test passed")
    }
}


