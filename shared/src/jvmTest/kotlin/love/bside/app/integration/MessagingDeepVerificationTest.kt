package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.models.ConversationType
import love.bside.app.domain.repository.MessagingRepository
import org.junit.After
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.measureTime

/**
 * Deep Verification Test: Deletion, Thread Integrity, Performance
 */
class MessagingDeepVerificationTest {

    companion object {
        private lateinit var pocketBase: PocketBase
        private lateinit var repository: MessagingRepository
        private var testUserId: String? = null

        @JvmStatic
        @BeforeClass
        fun setup() {
            pocketBase = PocketBase("https://bside.pockethost.io/") 
            repository = PocketBaseMessagingRepository(pocketBase)
            
            // Use known test user to avoid registration overhead/noise
            runBlocking {
                try {
                    pocketBase.collection("t_user").authWithPassword("test@example.com", "test12345")
                    testUserId = pocketBase.authStore.model?.let { 
                        (it as? io.pocketbase.models.RecordModel)?.id 
                            ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                    }
                    println("✓ Authenticated as test@example.com ($testUserId)")
                } catch (e: Exception) {
                    throw RuntimeException("Auth failed. Ensure test@example.com exists.", e)
                }
            }
        }
    }

    private var createdConversationIds = mutableListOf<String>()

    @After
    fun cleanup() = runTest {
        createdConversationIds.forEach { convId ->
            try {
                // Best effort cleanup
                val parts = pocketBase.collection("m_conversation_participants")
                    .getList(io.pocketbase.models.QueryOptions(filter = "conversationId='$convId'"))
                parts.items.forEach { 
                     val item = it as? io.pocketbase.models.RecordModel
                     val id = item?.id ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                     if (id != null) pocketBase.collection("m_conversation_participants").delete(id)
                }
                pocketBase.collection("m_conversations").delete(convId)
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
        createdConversationIds.clear()
    }

    @Test
    fun testMessageSoftDeletion() = runTest {
        println("=== Testing Message Deletion (Soft Delete) ===")
        val convResult = repository.createDirectConversation(listOf(testUserId!!))
        val conv = (convResult as Result.Success).data
        createdConversationIds.add(conv.id)

        // 1. Send Message
        val msgResult = repository.sendMessage(conv.id, "To be deleted")
        assertTrue(msgResult is Result.Success)
        val msgId = msgResult.data.id

        // 2. Verify it exists
        val listBefore = repository.getMessages(conv.id)
        assertTrue((listBefore as Result.Success).data.any { it.id == msgId }, "Message should exist before delete")

        // 3. Delete Message
        val delResult = repository.deleteMessage(msgId)
        assertTrue(delResult is Result.Success, "Delete operation should succeed")

        // 4. Verify it is GONE from getMessages (filtered out)
        val listAfter = repository.getMessages(conv.id)
        val exists = (listAfter as Result.Success).data.any { it.id == msgId }
        val count = listAfter.data.size
        assertEquals(false, exists, "Message should NOT appear in list after deletion")
        
        println("✅ Soft Deletion Verified. Message $msgId is hidden.")
    }

    @Test
    fun testPerformanceAndThreads() = runTest {
        println("=== Testing Performance & Threading Depth ===")
        val convResult = repository.createDirectConversation(listOf(testUserId!!))
        val conv = (convResult as Result.Success).data
        createdConversationIds.add(conv.id)

        val rootMsgResult = repository.sendMessage(conv.id, "Root of Thread")
        val rootId = (rootMsgResult as Result.Success).data.id

        val threadDepth = 5
        var parentId = rootId
        
        // Measure time to create a thread chain
        val timeTaken = measureTime {
            repeat(threadDepth) { i ->
                val res = repository.sendMessage(conv.id, "Reply $i", parentId)
                assertTrue(res is Result.Success)
                parentId = res.data.id
            }
        }
        
        println("⏱️ Created chain of $threadDepth replies in $timeTaken")
        assertTrue(timeTaken.inWholeSeconds < 10, "Should be reasonably fast (<10s for 5 serial API calls)")

        // Verify Thread Retrieval
        val fullThreadResult = repository.getFullThread(rootId)
        assertTrue(fullThreadResult is Result.Success)
        val threadSize = fullThreadResult.data.size
        println("🧵 Thread Size: $threadSize (Expected: ${threadDepth + 1})")
        assertEquals(threadDepth + 1, threadSize)
        
        // Test Deleting the Root
        println("🗑️ Deleting Root Message of Thread...")
        repository.deleteMessage(rootId)
        
        // Fetch Thread Again - Root should be gone, but replies might stay (Orphaned? or just root missing?)
        // The implementation queries by `(id=root OR threadRootId=root) AND deletedAt=null`
        // If root is deleted, asking for getFullThread(rootId) might return empty or just replies.
        // Let's see what happens.
        val threadAfterRootDelete = repository.getFullThread(rootId)
        assertTrue(threadAfterRootDelete is Result.Success)
        val remaining = threadAfterRootDelete.data
        println("🧵 Thread Size After Root Delete: ${remaining.size}")
        
        // Verify Root is gone
        assertEquals(false, remaining.any { it.id == rootId }, "Root should be gone")
        // Verify Children persist (unless we implemented cascading delete, which we didn't)
        assertTrue(remaining.isNotEmpty(), "Children should persist (Soft Delete of root shouldn't kill orphaned children query immediately)")
        
        println("✅ Performance & Thread Integrity Verified")
    }
}
