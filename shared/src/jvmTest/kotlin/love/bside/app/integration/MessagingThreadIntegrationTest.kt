package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import love.bside.app.data.repository.MessagingRepository
import love.bside.app.data.models.ConversationType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.JsonElement

import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import io.pocketbase.models.RecordModel

class MessagingThreadIntegrationTest {


    @Test
    fun testThreadingFlow() = runBlocking {
        // Setup Schema (Admin)
        val adminPb = PocketBase("http://localhost:8091/")
        try {
            // Manual Admin Auth
            // Manual Admin Auth
    val adminEmail = "tester_admin@bside.love"
    val adminPassword = "password123"
            val authBody = mapOf(
                "identity" to adminEmail,
                "password" to adminPassword
            )
            
            // Admin created via CLI command manually as bootstrapping step
            
            val response = try {
                 adminPb.send<JsonObject>("/api/collections/_superusers/auth-with-password", method = "POST", body = authBody)
            } catch (e: Exception) {
                 println("First admin auth failed: $e")
                 // Retry with changeme
                   adminPb.send<JsonObject>("/api/collections/_superusers/auth-with-password", method = "POST", body = mapOf(
                    "identity" to "tester_admin@bside.love",
                    "password" to "password123"
                ))
            }
            // Response format for record auth: { token: "...", record: {...} }
            val token = TestUtils.extractToken(response)
            val model = TestUtils.extractAuthRecord(response)
            adminPb.authStore.save(token, model)
            
        } catch (e: Exception) {
             println("Admin auth failed: $e")
             throw e
        }
        // setupSchema(adminPb) // Handled by external script
        
        // User Client
        val pb = PocketBase("http://localhost:8091/")
        val messagingRepo = MessagingRepository(pb)

        // 1. Auth as tester
        println("Authenticating...")
        try {
            pb.collection("users").authWithPassword("tester@bside.love", "password123")
        } catch (e: Exception) {
             println("Auth failed, creating tester user...")
             // Create user
             try {
                val userBody = mapOf(
                    "email" to "tester@bside.love",
                    "password" to "password123",
                    "passwordConfirm" to "password123",
                    "name" to "Tester"
                )
                // Use generic map for creation to avoid Serialization confusion with JsonPrimitive
                pb.collection("users").create(userBody)
                
                // Re-auth
                pb.collection("users").authWithPassword("tester@bside.love", "password123")
             } catch (ex: Exception) {
                 println("Failed to create/auth tester: $ex")
                 throw ex
             }
        }
        
        // Cast model to RecordModel or JsonObject to access fields safely
        val authModel = pb.authStore.model
        val currentUserId = authModel?.get("id")?.jsonPrimitive?.content ?: throw IllegalStateException("No user id")

        // 2. Create Conversation
        println("Creating conversation...")
        val conversation = try {
            messagingRepo.createConversation(
                participants = listOf(currentUserId), // Self chat for testing
                type = ConversationType.DIRECT
            )
        } catch (e: Throwable) {
            println("Error creating conversation: $e")
            e.printStackTrace()
             throw e
        }

        // 3. Send Root Message
        println("Sending root message...")
        val rootMsg = messagingRepo.sendMessage(conversation.id, "Root Message")
        assertNotNull(rootMsg.id)
        
        // 4. Send Reply
        println("Sending reply...")
        val replyMsg = messagingRepo.sendMessage(
            conversationId = conversation.id, 
            text = "Reply Message", 
            replyToId = rootMsg.id,
            threadRootId = rootMsg.id
        )
        println("Root ID: ${rootMsg.id}")
        println("Reply To ID: ${replyMsg.replyToMessageId}")
        println("Thread Root ID: ${replyMsg.threadRootId}")
        
        assertEquals(rootMsg.id, replyMsg.replyToMessageId)
        assertEquals(rootMsg.id, replyMsg.threadRootId)

        // 5. Verify via Subscription
        println("Verifying subscription...")
        val receivedMessages = mutableListOf<love.bside.app.data.models.Message>()
        
        val job = this.launch {
            messagingRepo.observeMessages(conversation.id)
                .take(1)
                .toList(receivedMessages)
        }
        
        kotlinx.coroutines.delay(500)
        
        messagingRepo.sendMessage(conversation.id, "Realtime Message")
        
        withTimeout(5000) {
            job.join()
        }
        
        assertEquals(1, receivedMessages.size)
        assertEquals("Realtime Message", receivedMessages[0].content)
        
        println("Test Passed!")
    }
}
