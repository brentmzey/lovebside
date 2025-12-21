package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.repository.MessagingRepository
import org.junit.Test
import kotlin.test.assertTrue

/**
 * UTILITY SCRIPT (Not a real test)
 * Run this to SEED data into the Production Database for manual verification.
 * 
 * Usage: ./gradlew :shared:jvmTest --tests "love.bside.app.integration.SeedConversation"
 */
class SeedConversation {

    @Test
    fun seed() = runTest {
        println("🌱 SEEDING DATA FOR MANUAL VERIFICATION...")
        
        val pocketBase = PocketBase("https://bside.pockethost.io/")
        val repository = PocketBaseMessagingRepository(pocketBase)
        
        // 1. Get Users
        val user1Email = "test@example.com"
        val user2Email = "test2@example.com"
        val pass = "test12345"

        val u1 = getUserId(pocketBase, user1Email, pass)
        val u2 = getUserId(pocketBase, user2Email, pass)
        
        println("User 1: $u1 ($user1Email)")
        println("User 2: $u2 ($user2Email)")
        
        // 2. Create Conversation
        // Auth as User 1 to create
        pocketBase.collection("t_user").authWithPassword(user1Email, pass)
        
        val convoResult = repository.createDirectConversation(listOf(u1, u2))
        if (convoResult is Result.Error) {
             println("⚠️ Conversation might already exist or failed: ${convoResult.exception.message}")
             // Try to fetch existing if possible, or just proceed
        } else {
             println("✅ Created/Retrieved Conversation: ${(convoResult as Result.Success).data.id}")
        }
        
        // We usually get the ID from result or if it failed (implied exist), we need to find it.
        // For simplicity, let's assume success or reuse logic if we had findByParticipants.
        // But createDirectConversation should operate on "get or create" logic usually, or error.
        // Current impl throws error if exists? No, PB returns existing potentially or we handle it?
        // Actually our createDirectConversation checks existence? No, it just tries to create.
        // If it fails, let's just search for it or fetch user's conversations.
        
        val convos = repository.getConversations(u1)
        val targetConvo = (convos as? Result.Success)?.data?.find { it.conversationType.name == "DIRECT" }
        
        if (targetConvo != null) {
            println("👉 Using Conversation: ${targetConvo.id}")
            
            // 3. Send a Welcome Message
            val time = Clock.System.now().toString()
            val msgResult = repository.sendMessage(
                targetConvo.id, 
                "👋 Hello! This is a manual test message generated at $time."
            )
            
            if (msgResult is Result.Success) {
                 println("✅ Sent Message: ${msgResult.data.id}")
                 
                 // 4. Send a Reply to test threading
                 val replyResult = repository.sendMessage(
                     targetConvo.id,
                     "🧵 And this is a threaded reply!",
                     replyToMessageId = msgResult.data.id
                 )
                 if (replyResult is Result.Success) {
                     println("✅ Sent Reply: ${replyResult.data.id}")
                 }
            }
        } else {
            println("❌ Could not find or create a conversation.")
        }
        
        println("\n🎉 SEEDING COMPLETE.")
        println("You can now log in as '$user1Email' / '$pass' on the app to see this.")
    }

    private suspend fun getUserId(pb: PocketBase, email: String, pass: String): String {
        pb.collection("t_user").authWithPassword(email, pass)
        val model = pb.authStore.model
        return (model as? io.pocketbase.models.RecordModel)?.id 
             ?: (model as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"') 
             ?: throw Exception("Could not get ID for $email")
    }
}
