package love.bside.app.manual

import io.pocketbase.PocketBase
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import love.bside.app.data.repository.MessagingRepository
import love.bside.app.data.models.ConversationType
import love.bside.app.data.models.PresenceStatus

/**
 * Manual verification test - Run this to see actual data flow
 * 
 * Prerequisites:
 * 1. Start PocketBase: ./pocketbase/pocketbase serve --http=127.0.0.1:8091
 * 2. Create admin: ./pocketbase/pocketbase superuser create admin@test.com password123
 * 3. Run this file
 */
fun main() = runBlocking {
    println("╔════════════════════════════════════════════════════════════╗")
    println("║  BSide Messaging - Manual Verification                   ║")
    println("╚════════════════════════════════════════════════════════════╝")
    println()
    
    val pb = PocketBase("http://localhost:8091/")
    val repo = MessagingRepository(pb)
    
    try {
        // 1. Create/Auth User
        println("🔐 Authenticating...")
        try {
            pb.collection("t_user").authWithPassword("tester@bside.love", "password123")
            println("✓ Logged in as existing user")
        } catch (e: Exception) {
            println("ℹ Creating new user...")
            pb.collection("t_user").create(mapOf(
                "email" to "tester@bside.love",
                "password" to "password123",
                "passwordConfirm" to "password123",
                "name" to "Test User"
            ))
            pb.collection("t_user").authWithPassword("tester@bside.love", "password123")
            println("✓ New user created and logged in")
        }
        
        val userId = pb.authStore.model?.get("id")?.toString()?.trim('"') ?: error("No user ID")
        println("  User ID: $userId")
        println()
        
        // 2. Create Conversation
        println("💬 Creating conversation...")
        val conversation = repo.createConversation(
            participants = listOf(userId),
            type = ConversationType.DIRECT
        )
        println("✓ Conversation created")
        println("  ID: ${conversation.id}")
        println("  Type: ${conversation.type}")
        println("  Participants: ${conversation.participants.size}")
        println()
        
        // 3. Send Messages
        println("📨 Sending messages...")
        val msg1 = repo.sendMessage(conversation.id, "Hello! This is a test message.")
        println("✓ Message 1 sent: ${msg1.content}")
        delay(100)
        
        val msg2 = repo.sendMessage(conversation.id, "This is a second message.")
        println("✓ Message 2 sent: ${msg2.content}")
        delay(100)
        
        // 4. Send Threaded Reply
        println("\n🧵 Creating threaded reply...")
        val reply = repo.sendMessage(
            conversationId = conversation.id,
            text = "This is a reply to the first message!",
            replyToId = msg1.id,
            threadRootId = msg1.id
        )
        println("✓ Reply sent: ${reply.content}")
        println("  Reply To: ${reply.replyToMessageId}")
        println("  Thread Root: ${reply.threadRootId}")
        println()
        
        // 5. Add Reactions
        println("👍 Adding reactions...")
        val reaction1 = repo.addReaction(msg1.id, "👍")
        println("✓ Added 👍 to first message")
        
        val reaction2 = repo.addReaction(msg2.id, "❤️")
        println("✓ Added ❤️ to second message")
        println()
        
        // 6. Set Presence
        println("👀 Setting presence...")
        val presence = repo.setPresence(PresenceStatus.ONLINE, "Testing the messaging system")
        println("✓ Presence set")
        println("  Status: ${presence.status}")
        println("  Message: ${presence.activityMessage}")
        println()
        
        // 7. Fetch All Messages
        println("📥 Fetching all messages...")
        val messages = repo.getMessages(conversation.id)
        println("✓ Retrieved ${messages.size} messages:")
        messages.forEachIndexed { index, msg ->
            val prefix = if (msg.replyToMessageId != null) "  ↳" else " "
            println("  $prefix ${index + 1}. ${msg.content}")
            if (msg.replyToMessageId != null) {
                println("      (Reply to: ${msg.replyToMessageId})")
            }
        }
        println()
        
        // 8. Summary
        println("╔════════════════════════════════════════════════════════════╗")
        println("║  ✅ VERIFICATION COMPLETE                                 ║")
        println("╚════════════════════════════════════════════════════════════╝")
        println()
        println("📊 Summary:")
        println("  • 1 conversation created")
        println("  • 3 messages sent (2 regular + 1 threaded reply)")
        println("  • 2 reactions added")
        println("  • 1 presence status set")
        println()
        println("🔍 View in PocketBase Admin:")
        println("  URL: http://localhost:8091/_/")
        println("  Collection: m_messages")
        println()
        println("✨ All messaging features are working correctly!")
        
    } catch (e: Exception) {
        println()
        println("❌ Error: ${e.message}")
        e.printStackTrace()
    }
}
