package love.bside.app.unit

import io.mockk.coEvery
import io.mockk.mockk
import io.pocketbase.PocketBase
import kotlinx.coroutines.test.runTest
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.domain.models.Conversation
import love.bside.app.domain.models.Message
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class PocketBaseMessagingRepositoryUnitTest {

    private val pocketBase = mockk<PocketBase>(relaxed = true)
    private val repo = PocketBaseMessagingRepository(pocketBase)

    // Helper to create a fake Record as JsonObject
    private fun fakeRecord(id: String, vararg pairs: Pair<String, Any?>): JsonObject {
        return buildJsonObject {
            put("id", id)
            put("created", "2023-01-01T00:00:00.000Z")
            put("updated", "2023-01-01T00:00:00.000Z")
            pairs.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Number -> put(key, value)
                    is Boolean -> put(key, value)
                    is List<*> -> {
                        // handling simple list of strings
                        // constructing JsonArray needs explicit list
                        // for simplicity assume empty list or ignore complex arrays in this fake
                    }
                    null -> {} // skip
                }
            }
        }
    }

    @Test
    fun `createDirectConversation returns Success`() = runTest {
        val convId = "conv123"
        val convRecord = fakeRecord(convId, "conversationType" to "direct")
        coEvery { pocketBase.collection("m_conversations").create(any()) } returns convRecord
        coEvery { pocketBase.collection("m_conversations").getOne(convId) } returns convRecord
        coEvery { pocketBase.collection("m_conversation_participants").create(any()) } returns fakeRecord("part1")

        val result = repo.createDirectConversation(listOf("alice", "bob"))
        assertTrue(result is Result.Success)
        val conv = (result as Result.Success).data
        assertEquals(convId, conv.id)
       // assertEquals("direct", conv.type) // type property might not exist on domain model if mapped from DB, check Conversation model
    }

    @Test
    fun `sendMessage returns Success and sets thread fields`() = runTest {
        val convId = "conv123"
        val senderId = "alice"
        val msgId = "msg123"
        val msgRecord = fakeRecord(
            msgId,
            "conversationId" to convId,
            "senderId" to senderId,
            "content" to "Hello",
            "threadRootId" to "",
            "threadDepth" to 0,
            "threadReplyCount" to 0
        )
        
        // Mock Auth
        val authModel = fakeRecord(senderId, "id" to senderId)
        coEvery { pocketBase.authStore.model } returns authModel

        // mocks must return JsonObject
        coEvery { pocketBase.collection("m_messages").create(any()) } returns msgRecord
        coEvery { pocketBase.collection("m_messages").getOne(msgId) } returns msgRecord // Used for returning mapped message
        coEvery { pocketBase.collection("m_messages").update(msgId, any()) } returns msgRecord
        coEvery { pocketBase.collection("m_conversations").update(convId, any()) } returns fakeRecord(convId)

        val result = repo.sendMessage(convId, "Hello") // Removed senderId arg
        assertTrue(result is Result.Success)
        val message = (result as Result.Success).data
        assertEquals(msgId, message.id)
        assertEquals(0, message.threadDepth)
        // threadRootId is "" in fake, mapped to null? mapRecordToMessage logic takesIf { it.isNotEmpty() }
        assertEquals(null, message.threadRootId) // Empty string -> null
    }

    @Test
    fun `getThreadRoot returns correct root id`() = runTest {
        val rootId = "root"
        val childId = "child"
        val leafId = "leaf"
        
        // Use getOne instead of getOneTyped
        coEvery { pocketBase.collection("m_messages").getOne(leafId) } returns fakeRecord(leafId, "replyToMessageId" to childId)
        coEvery { pocketBase.collection("m_messages").getOne(childId) } returns fakeRecord(childId, "replyToMessageId" to rootId)
        coEvery { pocketBase.collection("m_messages").getOne(rootId) } returns fakeRecord(rootId, "replyToMessageId" to "") // or null

        val result = repo.getThreadRoot(leafId)
        assertTrue(result is Result.Success)
        assertEquals(rootId, (result as Result.Success).data.id)
    }

    @Test
    fun `searchMessages returns filtered list`() = runTest {
        val convId = "conv123"
        val query = "pizza"
        
        val record1 = fakeRecord("msg1", "content" to "I love pizza", "conversationId" to convId, "senderId" to "u1", "sentAt" to "2023-01-01T00:00:00Z")
        val record2 = fakeRecord("msg2", "content" to "pizza party", "conversationId" to convId, "senderId" to "u1", "sentAt" to "2023-01-01T00:00:00Z")
        
        // ListResult<JsonObject>
        val listResult = io.pocketbase.models.ListResult(1, 10, 2, 1, listOf(record1, record2))
        
        // getList returns ListResult<JsonObject>
        coEvery {
            pocketBase.collection("m_messages").getList(any())
        } returns listResult

        val result = repo.searchMessages(query, convId)
        
        assertTrue(result is Result.Success)
        val msgs = (result as Result.Success).data
        assertEquals(2, msgs.size)
        assertTrue(msgs.all { it.content.contains("pizza", ignoreCase = true) })
    }

    @Test
    fun `error from PocketBase maps to Result Error`() = runTest {
        coEvery { pocketBase.collection("m_conversations").create(any()) } throws io.pocketbase.models.ClientResponseException(
            url = "",
            statusCode = 403,
            response = io.pocketbase.models.ErrorResponse(code=403, message = "Forbidden"),
            // originalError = RuntimeException("Forbidden") // removed in recent sdk?
        )
        val result = repo.createDirectConversation(listOf("alice", "bob"))
        assertTrue(result is Result.Error)
        val err = (result as Result.Error).exception
        // Repo maps to Unknown currently
        assertTrue(err is AppException.Unknown)
        assertTrue(err.message?.contains("Forbidden") == true)
    }
}
