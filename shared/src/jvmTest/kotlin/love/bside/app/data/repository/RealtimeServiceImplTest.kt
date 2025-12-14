package love.bside.app.data.repository

import io.mockk.*
import io.pocketbase.PocketBase
import io.pocketbase.models.RealtimeAction
import io.pocketbase.models.RecordModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import love.bside.app.core.Result
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.TypingStatus
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RealtimeServiceImplTest {
    private lateinit var pocketBase: PocketBase
    private lateinit var repo: PocketBaseMessagingRepository
    private lateinit var service: RealtimeServiceImpl

    @Before
    fun setUp() {
        pocketBase = mockk(relaxed = true)
        repo = mockk(relaxed = true)
        service = RealtimeServiceImpl(pocketBase, repo)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `subscribeToConversation emits messages from event`() = runBlocking {
        // Prepare a fake Record as JsonObject
        val conversationId = "conv123"
        val fakeRecord = buildJsonObject {
            put("id", "msg1")
            put("conversationId", conversationId)
        }
        
        // Mock repository mapping
        val domainMessage = Message(
            id = "msg1",
            conversationId = conversationId,
            senderId = "userA",
            content = "Hello",
            messageType = love.bside.app.domain.models.MessageType.TEXT,
            attachments = emptyList(),
            sentAt = kotlinx.datetime.Instant.parse("2025-01-01T00:00:00Z"),
            editedAt = null,
            deletedAt = null,
            readByCount = 0,
            replyToMessageId = null,
            threadRootId = null,
            threadDepth = null,
            threadReplyCount = null,
            created = kotlinx.datetime.Instant.parse("2025-01-01T00:00:00Z"),
            updated = kotlinx.datetime.Instant.parse("2025-01-01T00:00:00Z")
        )
        
        every { repo.mapRecordToMessage(any()) } returns domainMessage

        // Mock pocketBase.realtime.subscribe
        val slotCallback = slot<(io.pocketbase.models.RealtimeEvent) -> Unit>()
        
        // Note: subscribe returns a suspending function type: suspend () -> Unit
        coEvery { 
            pocketBase.realtime.subscribe(eq("m_messages"), capture(slotCallback), any()) 
        } returns { /* unsubscribe */ }

        // Collect
        val messages = mutableListOf<Message>()
        val job = launch { 
            service.subscribeToConversation(conversationId).take(1).toList(messages) 
        }
        
        // Simulate event
        // We need a RealtimeEvent with JsonElement record
        val event = io.pocketbase.models.RealtimeEvent(
            action = RealtimeAction.create,
            record = fakeRecord
        )
        
        // Wait slightly for subscription to register
        delay(50)
        
        if (slotCallback.isCaptured) {
            slotCallback.captured.invoke(event)
        }
        
        job.join()

        assertEquals(1, messages.size)
        assertEquals(domainMessage, messages.first())
    }
}
