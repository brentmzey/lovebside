package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.models.ConversationType
import love.bside.app.domain.repository.MessagingRepository
import org.junit.After
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Integration tests for Advanced Messaging features (Self Chat, Group Chat)
 * Tests against live PocketHost instance: https://bside.pockethost.io/
 */
class MessagingGroupIntegrationTest {

    companion object {
        private lateinit var pocketBase: PocketBase
        private lateinit var repository: MessagingRepository
        private var testUserId: String? = null
        private var testUser2Id: String? = null
        private var testUser3Id: String? = null

        @JvmStatic
        @BeforeClass
        fun setup() {
            pocketBase = PocketBase("http://localhost:8091") 
            repository = PocketBaseMessagingRepository(pocketBase)

            // Try to use the known "test" user first to debug logic
            // This bypasses potential registration issues/rate limits
            testUserId = getOrCreateUser("test", "test@example.com", "test12345")
            testUser2Id = getOrCreateUser("test2", "test2@example.com", "test12345")
            // testUser3Id = getOrCreateUser("g_user3_$now", "group3_$now@example.com", "test12345")
            // testUser3Id = getOrCreateUser("g_user3_$now", "group3_$now@example.com", "test12345")
            
            // Re-auth as primary user
            runBlocking {
                pocketBase.collection("users").authWithPassword("test@example.com", "test12345")
            }
        }

        private fun getOrCreateUser(username: String, email: String, pass: String): String? {
            return try {
                runBlocking {
                    try {
                        // 1. Try to Auth
                        pocketBase.collection("users").authWithPassword(email, pass)
                        val id = pocketBase.authStore.model?.let { 
                             (it as? io.pocketbase.models.RecordModel)?.id 
                             ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                        }
                        println("✓ Authenticated as $email")
                        id
                    } catch (e: Exception) {
                        println("ℹ Auth failed, creating user $email...")
                        // 2. Create if Auth failed
                        try {
                            pocketBase.collection("users").create(
                                mapOf(
                                    "username" to username,
                                    "email" to email,
                                    "password" to pass,
                                    "passwordConfirm" to pass,
                                    "name" to "Group Test User"
                                )
                            )
                        } catch (createEx: Exception) {
                             // Ignore creation errors if it's "user already exists" (400)
                             println("Creation might have failed (ignored): $createEx")
                        }
                        
                        // 3. Auth again
                        pocketBase.collection("users").authWithPassword(email, pass)
                         val id = pocketBase.authStore.model?.let { 
                             (it as? io.pocketbase.models.RecordModel)?.id 
                             ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                        }
                        println("✓ Created/Authenticated as $email")
                        id
                    }
                }
            } catch (e: Exception) {
                 println("❌ Failed to set up user $email: $e")
                 if (e is io.pocketbase.models.ClientResponseException) {
                     println("Response: ${e.response}")
                 }
                 throw RuntimeException("Failed to Get or Create user $email", e)
            }
        }
    }

    private var createdConversationIds = mutableListOf<String>()

    @After
    fun cleanup() = runTest {
        createdConversationIds.forEach { convId ->
            try {
                // Delete participants first
                val parts = pocketBase.collection("m_conversation_participants")
                    .getList(io.pocketbase.models.QueryOptions(filter = "conversationId='$convId'"))
                parts.items.forEach { 
                     val item = it as? io.pocketbase.models.RecordModel
                     val json = it as? kotlinx.serialization.json.JsonObject
                     val id = item?.id ?: json?.get("id")?.toString()?.trim('"')
                     if (id != null) {
                        pocketBase.collection("m_conversation_participants").delete(id)
                     }
                }
                // Delete conversation
                pocketBase.collection("m_conversations").delete(convId)
            } catch (e: Exception) {
                println("Cleanup warning for $convId: ${e.message}")
            }
        }
        createdConversationIds.clear()
    }

    @Test
    fun testSelfChat() = runTest {
        println("=== Testing Self Chat (Note to Self) ===")
        
        // 1. Create Direct Chat with ONLY VALID USER ID (myself)
        val result = repository.createDirectConversation(listOf(testUserId!!))
        
        assertTrue(result is Result.Success, "Should allow creating 1-person direct chat")
        val conversation = result.data
        createdConversationIds.add(conversation.id)
        
        assertEquals(ConversationType.DIRECT, conversation.conversationType)
        
        // 2. Verify Participants
        val participantsResult = repository.getParticipants(conversation.id)
        assertTrue(participantsResult is Result.Success)
        val participants = participantsResult.data
        
        assertEquals(1, participants.size, "Should have exactly 1 participant")
        assertEquals(testUserId, participants.first().userId)
        
        // 3. Send Message
        val msgResult = repository.sendMessage(conversation.id, "Note to self")
        assertTrue(msgResult is Result.Success)
        
        println("✅ Self Chat Verified: ID ${conversation.id}")
    }

    @Test
    fun testGroupChat() = runTest {
        if (testUser2Id == null) {
            println("Skipping Group Chat test (Users not set up)")
            return@runTest
        }
        println("=== Testing Group Chat (2 Users - Testing Logic) ===")
        
        // We test with 2 users, but verify it creates a GROUP type conversation
        val userIds = listOf(testUserId!!, testUser2Id!!)
        val groupName = "Avengers Assemble"
        
        // 1. Create Group
        val result = repository.createGroupConversation(groupName, userIds)
        
        assertTrue(result is Result.Success, "Should create group chat")
        val conversation = result.data
        createdConversationIds.add(conversation.id)
        
        assertEquals(ConversationType.GROUP, conversation.conversationType)
        assertEquals(groupName, conversation.conversationName)
        
        // 2. Verify Participants
        val participantsResult = repository.getParticipants(conversation.id)
        assertTrue(participantsResult is Result.Success)
        val participants = participantsResult.data
        
        assertEquals(2, participants.size, "Should have 2 participants")
        
        // 3. Send Message
        val msgResult = repository.sendMessage(conversation.id, "Hello Team!")
        assertTrue(msgResult is Result.Success)
        
        println("✅ Group Chat Verified: ID ${conversation.id}")
    }
}
