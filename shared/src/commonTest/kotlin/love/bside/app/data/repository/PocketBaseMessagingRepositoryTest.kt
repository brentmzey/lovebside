package love.bside.app.data.repository

import io.pocketbase.PocketBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import love.bside.app.core.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull


class PocketBaseMessagingRepositoryTest {

    // Assuming a local PocketBase instance is running for integration tests
    // Using a known test user credentials if possible, or creating one.
    // For this test, we might need manual setup or assume "test@example.com" / "Test1234!" exists via migration.
    
    private val pbUrl = "http://127.0.0.1:8090"
    private val testEmail = "test@example.com"
    private val testPassword = "Test1234!"
    
    // You might want to skip this test if server is not reachable
    
    @Test
    fun testMessagingFlow() = runTest {
        val pb = PocketBase(pbUrl)
        
        // 1. Authenticate
        try {
            pb.collection("users").authWithPassword(testEmail, testPassword)
        } catch (e: Exception) {
            println("Skipping test: Could not authenticate with local PocketBase: ${e.message}")
            return@runTest
        }
        
        val repo = PocketBaseMessagingRepository(pb)
        val model = pb.authStore.model
        val userId = (model as? io.pocketbase.models.RecordModel)?.id 
            ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.jsonPrimitive?.content
            ?: fail("User ID not found")
        
        // 2. Create or find a conversation (Simulated)
        // Since we need 2 participants, we might need another user. 
        // For simplicity, we might try to find an existing conversation or just testing message listing if we can't create.
        // Or we use a known setup.
        
        // Let's list conversations first
        val conversationsResult = repo.getConversations(userId)
        assertTrue(conversationsResult is Result.Success, "Should fetch conversations")
        
        val conversations = conversationsResult.data
        var conversationId = conversations.firstOrNull()?.id
        
        if (conversationId == null) {
            println("No conversation found. Attempting to create one if another user exists is complex in this scope.")
            println("Please ensure the test user has at least one conversation for full testing.")
            // Ideally we create a second user and a conversation here.
            // For now, we assert that if we have no conversation, we can't test messaging fully.
            return@runTest
        }
        
        // 3. Send Message
        val content = "Integration Test Message ${kotlinx.datetime.Clock.System.now()}"
        val sendResult = repo.sendMessage(conversationId, content)
        assertTrue(sendResult is Result.Success, "Should send message")
        val sentMessage = sendResult.data
        assertEquals(content, sentMessage.content)
        
        // 4. Get Messages
        val messagesResult = repo.getMessages(conversationId, perPage = 10)
        assertTrue(messagesResult is Result.Success)
        assertTrue(messagesResult.data.any { it.id == sentMessage.id }, "Fetched messages should include sent message")
        
        // 5. Realtime Subscription (Test if we receive what we send)
        // We need to launch the collector first
        val receivedMessages = mutableListOf<love.bside.app.domain.models.Message>()
        
        val job = launch {
            repo.subscribeToConversation(conversationId)
                .take(1)
                .collect { receivedMessages.add(it) }
        }
        
        // Send another message to trigger realtime event
        val rtContent = "Realtime Test ${kotlinx.datetime.Clock.System.now()}"
        repo.sendMessage(conversationId, rtContent)
        
        // Wait for it
        withTimeoutOrNull(5.seconds) {
            job.join()
        }
        
        assertTrue(receivedMessages.isNotEmpty(), "Should receive realtime message")
        assertEquals(rtContent, receivedMessages.first().content)
        
        // 6. Typing Status
        val setTypingResult = repo.setTypingStatus(conversationId, true)
        assertTrue(setTypingResult is Result.Success)
        
        // Subscribe to typing (harder to test self-event without 2 clients, but we verify no crash)
        val typingJob = launch {
            repo.subscribeToTypingIndicators(conversationId)
                .take(1) // might timeout if we don't get our own events (usually we don't)
                .collect { println("Typing event: $it") }
        }
        
        // We typically don't receive our own typing status, so we just let it timeout or cancel
        typingJob.cancel()
    }
}
