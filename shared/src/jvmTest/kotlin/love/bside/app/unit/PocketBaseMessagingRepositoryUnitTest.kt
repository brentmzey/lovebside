package love.bside.app.unit

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.mockk.mockk
import io.pocketbase.PocketBase
import kotlinx.coroutines.test.runTest
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.data.repository.RealtimeService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PocketBaseMessagingRepositoryUnitTest {

    private val realtimeService = mockk<RealtimeService>(relaxed = true)

    private fun createRepository(
        handler: suspend (io.ktor.client.request.HttpRequestData) -> io.ktor.client.request.HttpResponseData
    ): PocketBaseMessagingRepository {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    handler(request)
                }
            }
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
        
        val pb = PocketBase("http://test", httpClient = client)
        return PocketBaseMessagingRepository(pb, realtimeService)
    }

    // Simplified Mock Response Helper
    private fun jsonResponse(content: String, status: HttpStatusCode = HttpStatusCode.OK) = io.ktor.client.request.HttpResponseData(
        statusCode = status,
        headers = headersOf(HttpHeaders.ContentType, "application/json"),
        body = ByteReadChannel(content.encodeToByteArray()), // Use ByteReadChannel for Ktor 3 compatibility
        requestTime = io.ktor.util.date.GMTDate.START,
        version = io.ktor.http.HttpProtocolVersion.HTTP_1_1,
        callContext = kotlin.coroutines.EmptyCoroutineContext + kotlinx.coroutines.Job()
    )

    @Test
    fun `createDirectConversation returns Success`() = runTest {
        val repo = createRepository { request ->
            val url = request.url.toString()
            if (url.endsWith("/api/collections/m_conversations/records") && request.method.value == "POST") {
                jsonResponse("""
                    {"id": "conv123", "conversationType": "direct", "conversationName": ""}
                """.trimIndent())
            } else if (url.endsWith("/api/collections/m_conversation_participants/records") && request.method.value == "POST") {
                jsonResponse("""{"id": "part1"}""")
            } else {
                jsonResponse("{}", HttpStatusCode.NotFound)
            }
        }

        val result = repo.createDirectConversation(listOf("alice", "bob"))
        assertTrue(result is Result.Success)
        val conv = (result as Result.Success).data
        assertEquals("conv123", conv.id)
    }

    @Test
    fun `sendMessage returns Success`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    println("Mock Request: ${request.method.value} $url")
                    when {
                        url.endsWith("/api/collections/m_messages/records") && request.method.value == "POST" -> 
                             jsonResponse("""{"id": "msg123"}""")
                             
                        url.contains("/api/collections/m_messages/records/msg123") ->
                             jsonResponse("""
                               {
                                   "id": "msg123",
                                   "conversation_id": "conv123", 
                                   "sender_id": "alice",
                                   "content": "Hello",
                                   "sent_at": "2023-01-01 12:00:00"
                               }
                             """.trimIndent())
                             
                        url.contains("/api/collections/m_conversations/records/conv123") -> 
                             jsonResponse("""{"id":"conv123"}""")
                             
                        else -> {
                            println("Mock Request NOT FOUND: $url")
                            jsonResponse("{}", HttpStatusCode.NotFound)
                        }
                    }
                }
            }
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        
        val pb = PocketBase("http://test", httpClient = client)
        // Set Auth
        pb.authStore.save("token", kotlinx.serialization.json.JsonObject(mapOf("id" to kotlinx.serialization.json.JsonPrimitive("alice"))))
        
        // Mock AuthStore
        val mockAuthStore = mockk<io.pocketbase.stores.AuthStore>(relaxed = true)
        val mockUser = kotlinx.serialization.json.JsonObject(mapOf("id" to kotlinx.serialization.json.JsonPrimitive("alice")))
        io.mockk.every { mockAuthStore.model } returns mockUser
        
        val pbWithAuth = PocketBase("http://test", authStore = mockAuthStore, httpClient = client)
        val repo = PocketBaseMessagingRepository(pbWithAuth, realtimeService)

        val result = repo.sendMessage("conv123", "Hello")
        
        if (result is Result.Error) {
             println("sendMessage failed: ${(result.exception as? AppException.Unknown)?.message}")
             (result.exception as? AppException.Unknown)?.cause?.printStackTrace()
        }
        assertTrue(result is Result.Success, "Expected Success but got $result")
        assertEquals("msg123", result.data.id)
    }

    @Test
    fun `getThreadRoot returns correct root id`() = runTest {
         val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    println("Mock Request: ${request.method.value} $url")
                    if (url.contains("/api/collections/m_messages/records/leaf")) {
                        jsonResponse("""{"id":"leaf", "reply_to_message_id":"child", "conversation_id": "c"}""")
                    } else if (url.contains("/api/collections/m_messages/records/child")) {
                        jsonResponse("""{"id":"child", "reply_to_message_id":"root", "conversation_id": "c"}""")
                    } else if (url.contains("/api/collections/m_messages/records/root")) {
                        jsonResponse("""{"id":"root", "reply_to_message_id":"", "conversation_id": "c"}""")
                    } else {
                         println("Mock Request NOT FOUND: $url")
                        jsonResponse("{}", HttpStatusCode.NotFound)
                    }
                }
            }
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val pb = PocketBase("http://test", httpClient = client)
        val repo = PocketBaseMessagingRepository(pb, realtimeService)
        
        val result = repo.getThreadRoot("leaf")
        if (result is Result.Error) {
             println("getThreadRoot failed: ${(result.exception as? AppException.Unknown)?.message}")
        }
        assertTrue(result is Result.Success, "Expected Success")
        assertEquals("root", result.data.id)
    }
    
    @Test
    fun `error from PocketBase maps to Result Error`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { 
                    jsonResponse("""{"code":403, "message":"Forbidden"}""", HttpStatusCode.Forbidden)
                }
            }
             install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        val pb = PocketBase("http://test", httpClient = client)
        val repo = PocketBaseMessagingRepository(pb, realtimeService)
        
        val result = repo.createDirectConversation(listOf("alice", "bob"))
        assertTrue(result is Result.Error)
        val err = (result as Result.Error).exception
        assertTrue(err is AppException.Unknown)
    }

    @Test
    fun `searchMessages returns filtered list`() = runTest {
        val convId = "conv123"
        val query = "pizza"
        
        val repo = createRepository { request ->
            val url = request.url.toString()
            if (url.contains("/api/collections/m_messages/records") && request.method.value == "GET") {
                // Return ListResult
                jsonResponse("""
                    {
                        "page": 1,
                        "perPage": 30,
                        "totalItems": 2,
                        "totalPages": 1,
                        "items": [
                            {"id": "msg1", "content": "I love pizza", "conversationId": "conv123", "senderId": "u1", "sentAt": "2023-01-01 10:00:00"},
                            {"id": "msg2", "content": "pizza party", "conversationId": "conv123", "senderId": "u1", "sentAt": "2023-01-01 11:00:00"}
                        ]
                    }
                """.trimIndent())
            } else {
                jsonResponse("{}", HttpStatusCode.NotFound)
            }
        }
        
        val result = repo.searchMessages(query, convId)
        
        assertTrue(result is Result.Success)
        val msgs = (result as Result.Success).data
        assertEquals(2, msgs.size)
        assertTrue(msgs.all { it.content.fold({ false }, { text -> text.contains("pizza", ignoreCase = true) }) })
    }

    @Test
    fun `getGlobalSettings returns settings`() = runTest {
        val client = HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    val url = request.url.toString()
                    when {
                        url.contains("/api/collections/t_user_property/records") -> 
                            jsonResponse("""
                                {
                                    "page": 1,
                                    "perPage": 30,
                                    "totalItems": 2,
                                    "totalPages": 1,
                                    "items": [
                                        {"id": "p1", "key": "messaging.read_receipts_enabled", "value": "true", "user_id": "alice"},
                                        {"id": "p2", "key": "messaging.typing_status_enabled", "value": "false", "user_id": "alice"}
                                    ]
                                }
                            """.trimIndent())
                        else -> jsonResponse("{}", HttpStatusCode.NotFound)
                    }
                }
            }
            install(ContentNegotiation) {
                json(kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true })
            }
        }
        
        val mockAuthStore = mockk<io.pocketbase.stores.AuthStore>(relaxed = true)
        val mockUser = kotlinx.serialization.json.JsonObject(mapOf("id" to kotlinx.serialization.json.JsonPrimitive("alice")))
        io.mockk.every { mockAuthStore.model } returns mockUser
        
        val pbWithAuth = PocketBase("http://test", authStore = mockAuthStore, httpClient = client)
        val repo = PocketBaseMessagingRepository(pbWithAuth, realtimeService)
        
        val result = repo.getGlobalSettings()
        println("DEBUG: Result type: ${result::class.simpleName}")
        if (result is Result.Error) {
             println("DEBUG: getGlobalSettings failed: ${(result.exception as? AppException.Unknown)?.message}")
             (result.exception as? AppException.Unknown)?.cause?.printStackTrace()
        } else if (result is Result.Success) {
            println("DEBUG: Success data: ${result.data}")
        }
        assertTrue(result is Result.Success)
        val settings = (result as Result.Success).data
        assertTrue(settings.readReceiptsEnabled)
        assertTrue(!settings.typingStatusEnabled)
    }
}
