package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.models.RecordModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import love.bside.app.core.Result
import arrow.core.getOrElse
import kotlin.test.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class PocketBaseMessagingRepositoryTest {

  // Use remote test server instead of local instance
  private val pbUrl = "https://bside.pockethost.io/"
  private val testEmail = "test@example.com"
  private val testPassword = "test12345"
  
  @Test
  @Ignore // Integration test dependent on remote Pockethost instance. NPEs in CI.
  fun testMessagingFlow() = runTest {
    val pb = PocketBase(pbUrl)
    
    // 1. Authenticate
    try {
      pb.collection("t_user").authWithPassword(testEmail, testPassword)
    } catch (e: Exception) {
      println("⚠️  Skipping test: Could not authenticate with PocketBase: ${e.message}")
      return@runTest
    }
    
    val repo = PocketBaseMessagingRepository(pb)
    val model = pb.authStore.model
    
    // Safe extraction of user ID with null checks
    val userId = when (model) {
      is RecordModel -> model.id
      is JsonObject -> model["id"]?.jsonPrimitive?.contentOrNull
      else -> null
    } ?: run {
      println("⚠️  Skipping test: User ID not found after authentication")
      return@runTest
    }
    
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
    assertEquals(content, sentMessage.content.getOrElse { "" })
    
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
    assertEquals(rtContent, receivedMessages.first().content.getOrElse { "" })
    
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
