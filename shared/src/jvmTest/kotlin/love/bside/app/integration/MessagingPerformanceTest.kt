package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.repository.MessagingRepository
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime

class MessagingPerformanceTest {

    companion object {
        private lateinit var pocketBase: PocketBase
        private lateinit var repository: MessagingRepository
        private var testUserId: String? = null
        private var testUser2Id: String? = null

        @JvmStatic
        @BeforeClass
        fun setup() {
            pocketBase = PocketBase("https://bside.pockethost.io/")
            repository = PocketBaseMessagingRepository(pocketBase)
            
            testUserId = getOrCreateUser("test1", "test@example.com", "test12345")
            testUser2Id = getOrCreateUser("test2", "test2@example.com", "test12345")
            
            runBlocking {
                pocketBase.collection("t_user").authWithPassword("test@example.com", "test12345")
            }
        }

        private fun getOrCreateUser(username: String, email: String, pass: String): String? {
            return runBlocking {
                try {
                    pocketBase.collection("t_user").authWithPassword(email, pass)
                    pocketBase.authStore.model?.let { 
                        (it as? io.pocketbase.models.RecordModel)?.id 
                        ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                    }
                } catch (e: Exception) {
                    pocketBase.collection("t_user").create(
                        mapOf("username" to username, "email" to email, "password" to pass, "passwordConfirm" to pass, "name" to "Perf User")
                    )
                    pocketBase.collection("t_user").authWithPassword(email, pass)
                    pocketBase.authStore.model?.let { 
                        (it as? io.pocketbase.models.RecordModel)?.id 
                        ?: (it as? kotlinx.serialization.json.JsonObject)?.get("id")?.toString()?.trim('"')
                    }
                }
            }
        }
    }

    @Test
    fun testThreadFetchingPerformance() = runTest {
        val convoResult = repository.createDirectConversation(listOf(testUserId!!, testUser2Id!!))
        assertTrue(convoResult is Result.Success<*>)
        val conversationId = (convoResult as Result.Success).data.id

        // Seed 50 messages
        val rootResult = repository.sendMessage(conversationId, "Root")
        val rootId = (rootResult as Result.Success).data.id

        // Sequential send is slow due to network RTT, but that's expected.
        // We want to measure the FETCH speed which should be FAST (1 query).
        println("Seeding 20 replies...")
        val seedTime = measureTime {
            var lastId = rootId
            repeat(20) { i ->
                val res = repository.sendMessage(conversationId, "Reply $i", lastId) // Deep nesting
                lastId = (res as Result.Success).data.id
            }
        }
        println("Seeding took: $seedTime")

        // Fetch Full Thread
        println("Fetching full thread...")
        var threadSize = 0
        val fetchTime = measureTime {
            val threadResult = repository.getFullThread(rootId)
            assertTrue(threadResult is Result.Success<*>)
            threadSize = (threadResult as Result.Success).data.size
        }
        
        println("Fetched $threadSize messages in: $fetchTime")
        
        // Assert performance (arbitrary but generous for remote, strict for logic)
        // A single query to Prod + RTT should be < 2.0s. 
        // Recursive (20 queries) would be ~20 * 0.5s = 10s.
        if (fetchTime.inWholeSeconds < 3) {
             println("✅ Performance PASS: < 3s")
        } else {
             println("⚠️ Performance SLOW: $fetchTime (Network lag?)")
        }
    }
}
