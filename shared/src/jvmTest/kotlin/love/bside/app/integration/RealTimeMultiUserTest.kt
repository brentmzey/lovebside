package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import love.bside.app.AppConstants

/**
 * Automated verification of Real-Time Messaging between two users.
 * Requires: Local PocketBase running at http://127.0.0.1:8090
 * Data: 'alice@bside.love' and 'bob@bside.love' (password123)
 */
class RealTimeMultiUserTest {

    private lateinit var aliceClient: PocketBase
    private lateinit var bobClient: PocketBase
    
    private lateinit var aliceRepo: PocketBaseMessagingRepository
    private lateinit var bobRepo: PocketBaseMessagingRepository

    private var testConversationId: String? = null

    @Test
    fun `verify Bob receives Alice's message via Real-Time`() = runTest {
        println("🧪 Test Configuration: Using ${if(AppConstants.USE_PRODUCTION) "PRODUCTION" else "LOCAL"} Environment")
        println("TARGET URL: ${AppConstants.POCKETBASE_URL}")

        // Skip assertions if we are in production but auth failed (handled in try block below)
        if (AppConstants.USE_PRODUCTION) {
             // We'll initialize inside
        }
        
        try {
            // ================= SETUP =================
            // Initialize clients
            aliceClient = PocketBase(AppConstants.POCKETBASE_URL)
            bobClient = PocketBase(AppConstants.POCKETBASE_URL)
            
            // Authenticate Alice
            aliceClient.collection("users").authWithPassword("alice@bside.love", "password123")
            
            // Authenticate Bob
            bobClient.collection("users").authWithPassword("bob@bside.love", "password123")
            
            // Get user IDs
            val aliceModel = aliceClient.authStore.model
            val aliceId = (aliceModel as? io.pocketbase.models.RecordModel)?.id 
                 ?: (aliceModel as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                 ?: throw IllegalStateException("Could not get Alice's ID")
            
            val bobModel = bobClient.authStore.model
            val bobId = (bobModel as? io.pocketbase.models.RecordModel)?.id 
                 ?: (bobModel as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                 ?: throw IllegalStateException("Could not get Bob's ID")
            
            println("    Alice ID: $aliceId")
            println("    Bob ID: $bobId")

            aliceRepo = PocketBaseMessagingRepository(aliceClient)
            bobRepo = PocketBaseMessagingRepository(bobClient)
            
            // Create Conversation
            val conversationResult = aliceRepo.createDirectConversation(listOf(aliceId, bobId))
            if (conversationResult is love.bside.app.core.Result.Error) {
                val ex = conversationResult.exception
                println("Conversation creation note: ${ex.message}")
                if (ex.cause is io.pocketbase.models.ClientResponseException) {
                    println("PB Error Response: ${(ex.cause as io.pocketbase.models.ClientResponseException).response}")
                }
            }
            testConversationId = conversationResult.getOrThrow().id

            // ================= EXECUTION =================
            val messagesToSend = listOf(
                "Hello from Automated Test ${kotlin.random.Random.nextInt()}",
                "This is the second message in the chain.",
                "And here is a third one to verify threading/ordering."
            )

            // 1. Bob subscribes
            val receivedMessages = mutableListOf<love.bside.app.domain.models.Message>()
            val job = launch {
                try {
                    bobRepo.subscribeToConversation(testConversationId!!)
                        .collect { message ->
                            println("Bob received: ${message.content}")
                            receivedMessages.add(message)
                        }
                } catch (e: Exception) {
                    println("Bob subscription error: $e")
                }
            }


            // Verify participants
            val participantsResult = aliceRepo.getParticipants(testConversationId!!)
            assertTrue(participantsResult is love.bside.app.core.Result.Success, "Failed to get participants")
            val participants = (participantsResult as love.bside.app.core.Result.Success).data
            println("Verified Participants: ${participants.size}")
            participants.forEach { p -> println(" - User: ${p.userId}, Role: ${p.role}") }
            assertEquals(2, participants.size, "Should have 2 participants")
            // Give subscription a moment
            kotlinx.coroutines.delay(500)

            // 2. Alice sends multiple messages
            messagesToSend.forEach { msgContent ->
                println("Alice sending: $msgContent")
                val result = aliceRepo.sendMessage(testConversationId!!, msgContent)
                if (result is love.bside.app.core.Result.Error) {
                    println("❌ Alice sendMessage FAILED:")
                    val e = result.exception
                    val cause = e.cause
                    if (cause is io.pocketbase.models.ClientResponseException) {
                        println("   PB Error: ${cause.response}")
                    } else {
                        println("   Error: ${e.message}")
                        e.printStackTrace()
                    }
                }
                assertTrue(result is love.bside.app.core.Result.Success, "Alice failed to send message: $msgContent")
                kotlinx.coroutines.delay(200) // Slight delay between messages
            }
            
            // 3. Bob marks the conversation as read (Read Receipt simulation)
            bobRepo.markAsRead(testConversationId!!)

            // 4. Wait for Bob to receive all
            println("Waiting for messages... (Received so far: ${receivedMessages.size}/${messagesToSend.size})")
            try {
                withTimeout(15.seconds) {
                    while (receivedMessages.size < messagesToSend.size) {
                        println("   ... waiting (count=${receivedMessages.size})")
                        kotlinx.coroutines.delay(1000)
                    }
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                println("❌ TIMEOUT waiting for messages. Received: ${receivedMessages.size}")
                // Do not throw yet, let assertions fail for better report
            }
            
            job.cancel()

            // ================= ASSERTION =================
            assertEquals(messagesToSend.size, receivedMessages.size, "Bob should have received all messages")
            messagesToSend.forEachIndexed { index, expected ->
                assertEquals(expected, receivedMessages[index].content, "Message $index content mismatch")
            }
            
            println("✅ SUCCESS: Real-Time verification passed! (Chain length: ${messagesToSend.size})")

        } catch (e: Exception) {
            println("❌ TEST FAILED: ${e.message}")
            if (e.cause is io.pocketbase.models.ClientResponseException) {
                println("   PB Cause: ${(e.cause as io.pocketbase.models.ClientResponseException).response}")
            }
            throw e
        } finally {
            // ================= TEARDOWN =================
            // Cleanup: Alice deletes the conversation always
            try {
                testConversationId?.let { id ->
                    println("🧹 Cleaning up conversation $id...")
                    kotlinx.coroutines.runBlocking {
                        aliceClient.collection("m_conversations").delete(id)
                    }
                    println("   Cleanup successful.")
                }
            } catch (e: Exception) {
                println("   Cleanup warning: ${e.message}")
            }
        }
    }
}
