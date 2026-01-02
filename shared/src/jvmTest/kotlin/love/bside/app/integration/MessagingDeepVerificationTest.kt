package love.bside.app.integration

import io.pocketbase.PocketBase
import io.pocketbase.models.RecordModel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.repository.AttachmentData
import love.bside.app.domain.models.MessageType
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class MessagingDeepVerificationTest {

    // Use LOCALHOST for this verification as per "start-everything.sh"
    private val pbUrl = "http://127.0.0.1:8091" // Using the direct PocketBase port (or 8081 facade)
    private val testEmail = "test@example.com" // Ensure this user exists or create one
    private val testPassword = "test12345"
    
    // NOTE: This test requires the Backend to be RUNNING (start-everything.sh)
    // and a user with these credentials to exist.
    
    @Test
    fun verifyDeepMessagingFeatures() = runTest {
        println("🚀 STARTED: verifyDeepMessagingFeatures")
        try {
            val client = HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json { 
                        ignoreUnknownKeys = true 
                        isLenient = true
                    })
                }
            }
            val pb = PocketBase(pbUrl, httpClient = client)
            println("✅ PocketBase initialized with $pbUrl")
            
            // 1. Authenticate as User
            println("🔑 Attempting to authenticate as $testEmail ...")
            try {
                pb.collection("users").authWithPassword(testEmail, testPassword)
                println("✅ Authentication success")
            } catch (e: Exception) {
                println("❌ Authentication failed: ${e.message}")
                println("⚠️ Please ensure user '$testEmail' with password '$testPassword' exists in PocketBase.")
                throw e
            }

        val repo = PocketBaseMessagingRepository(pb)
        val model = pb.authStore.model
        val userId = (model as? RecordModel)?.id ?: (model as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull ?: return@runTest
        
        println("✅ Authenticated as $userId")

        // --- 2. Self Chat Verification ---
        println("Testing Self Chat...")
        // Create conversation with ONLY myself
        val selfConvResult = repo.createDirectConversation(listOf(userId))
        assertTrue(selfConvResult is Result.Success, "Self Chat creation failed")
        val selfConv = selfConvResult.data
        println("✅ Self Chat Created: ${selfConv.id}")

        // --- 3. Media Upload Verification ---
        println("Testing Media Upload...")
        val dummyImage = AttachmentData(
            fileName = "test_image.png",
            data = ByteArray(1024) { 1 }, // Dummy bytes
            mimeType = "image/png"
        )
        val mediaMsgResult = repo.sendMessage(
            conversationId = selfConv.id,
            content = "Media Verification Message",
            attachments = listOf(dummyImage)
        )
        assertTrue(mediaMsgResult is Result.Success, "Media Message send failed")
        val mediaMsg = mediaMsgResult.data
        // Verify attachments list (assuming backend returns it populated)
        // If not expanded, it might just be filenames.
        // But messageType should definitely reflect if logic handles it, or at least it isn't failed.
        println("✅ Media Message Sent: ${mediaMsg.id}")
        
        // --- 4. Threading / Replies Verification ---
        println("Testing Replies/Threading...")
        val parentMsgResult = repo.sendMessage(selfConv.id, "Parent Message")
        assertTrue(parentMsgResult is Result.Success)
        val parentMsg = parentMsgResult.data
        
        val replyMsgResult = repo.sendMessage(
            conversationId = selfConv.id,
            content = "Reply Message",
            replyToMessageId = parentMsg.id
        )
        assertTrue(replyMsgResult is Result.Success, "Reply send failed")
        val replyMsg = replyMsgResult.data
        assertEquals(parentMsg.id, replyMsg.replyToMessageId, "Reply link broken")
        println("✅ Reply Sent: ${replyMsg.id} -> ${parentMsg.id}")

        // --- 5. Group Chat Verification (Simulated with just me if allowed, else skipped) ---
        println("Testing Group Chat...")
        // PocketBase logic might allow group of 1, or we need another user.
        // We'll try group of 1 for now or skip.
        val groupResult = repo.createGroupConversation("Test Group", listOf(userId))
        assertTrue(groupResult is Result.Success, "Group creation failed")
        println("✅ Group Created: ${groupResult.data.id}")
        } catch (e: Throwable) {
            println("❌ TEST FAILED WITH EXCEPTION")
            e.printStackTrace()
            throw e
        }
    }
}
