package love.bside.app.integration

import love.bside.app.data.models.Attachment
import org.junit.Test
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import io.pocketbase.PocketBase
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

class MessagingAttachmentVerificationTest {

    @Test
    fun testSendMessageWithAttachment() = runBlocking {
        // Setup similar to ThreadTest
        println("Starting test...")
        val pb = PocketBase("http://localhost:8091")
        val adminPb = PocketBase("http://localhost:8091")
        
        println("Authenticating admin...")
        // Authenticate as Admin
         try {
               adminPb.send<JsonObject>("/api/collections/_superusers/auth-with-password", method = "POST", body = mapOf(
                "identity" to "tester_admin@bside.love",
                "password" to "password123"
            ))
            println("Admin auth success")
        } catch (e: Exception) {
             e.printStackTrace()
             throw IllegalStateException("Admin auth failed. Run ./scripts/setup_dev_env.sh first. error: $e")
        }
        val adminToken = TestUtils.extractToken(adminPb.authStore.model as? JsonObject ?: JsonObject(emptyMap())) // Auth store model might be null if not saved automatically?
        // Actually manual save
        // We will assume TestUtils logic or just re-auth simply in repository
        // But let's reuse the flow:
        
        // Create User A
        val userEmail = "attach_user_${System.currentTimeMillis()}@bside.love"
        try {
            pb.collection("users").create(mapOf(
                "email" to userEmail,
                "password" to "password123",
                "passwordConfirm" to "password123",
                "name" to "Attachment User"
            ))
        } catch(e: Exception) {
            // Ignore if exists
        }
        pb.collection("users").authWithPassword(userEmail, "password123")
        println("User A authenticated")
        
        // Create Conversation
        val userBEmail = "attach_B_${System.currentTimeMillis()}@bside.love"
        // Create user B first
         try {
            adminPb.collection("users").create(mapOf(
                "email" to userBEmail,
                "password" to "password123",
                "passwordConfirm" to "password123",
                "name" to "User B"
            ))
        } catch(e: Exception) {}
        val userB = adminPb.collection("users").getFirstListItem("email='$userBEmail'")
        val userBId = userB["id"]?.jsonPrimitive?.content ?: ""
        
        // Create Conv
        val repo = love.bside.app.data.repository.MessagingRepository(pb)
        val conv = repo.createConversation(listOf(userBId), love.bside.app.data.models.ConversationType.DIRECT)
        
        // Send Message with Attachment
        val attachment = Attachment(
            fileName = "test_image.txt",
            data = "fake_image_bytes".encodeToByteArray(),
            mimeType = "text/plain"
        )
        
        println("Sending message with attachment...")
        val msg = repo.sendMessage(
            conversationId = conv.id,
            text = "Check this out",
            attachments = listOf(attachment)
        )
        
        assertNotNull(msg)
        assertEquals(1, msg.attachments.size)
        assertTrue(msg.attachments[0].isNotEmpty()) // Should be filename or ID? PocketBase returns filename usually
        println("Attachment sent: ${msg.attachments[0]}")
        
        // Verify from server
        val storedMsg = repo.getMessages(conv.id).first()
        assertEquals(msg.id, storedMsg.id)
        assertEquals(1, storedMsg.attachments.size)
    }
}
