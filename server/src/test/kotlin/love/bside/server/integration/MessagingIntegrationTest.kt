package love.bside.server.integration

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Integration tests for real-time messaging functionality
 * Tests the full stack: API -> Repository -> PocketBase
 */
class MessagingIntegrationTest {

    @Test
    fun `test create conversation and send message`() = testApplication {
        application {
            // TODO: Configure test application module
        }

        runBlocking {
            // Test user authentication
            val loginResponse = client.post("/api/pb/collections/users/auth-with-password") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "identity": "test@bside.love",
                        "password": "testpassword123"
                    }
                """.trimIndent())
            }
            
            assertEquals(HttpStatusCode.OK, loginResponse.status)
            val authToken = loginResponse.bodyAsText() // Extract token from response

            // Create conversation
            val createConvResponse = client.post("/api/pb/collections/m_conversations/records") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "conversation_type": "direct",
                        "is_group": false
                    }
                """.trimIndent())
            }
            
            assertEquals(HttpStatusCode.OK, createConvResponse.status)

            // Send message
            val sendMessageResponse = client.post("/api/pb/collections/m_messages/records") {
                header("Authorization", "Bearer $authToken")
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "conversation_id": "test-conv-id",
                        "content": "Hello, World!",
                        "message_type": "text"
                    }
                """.trimIndent())
            }
            
            assertEquals(HttpStatusCode.OK, sendMessageResponse.status)
        }
    }

    @Test
    fun `test typing indicators`() = testApplication {
        runBlocking {
            val response = client.post("/api/pb/collections/m_typing_status/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "conversation_id": "test-conv",
                        "user_id": "test-user",
                        "is_typing": true
                    }
                """.trimIndent())
            }
            
            assertTrue(response.status.isSuccess())
        }
    }

    @Test
    fun `test read receipts`() = testApplication {
        runBlocking {
            val response = client.post("/api/pb/collections/m_read_receipts/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "message_id": "test-message",
                        "user_id": "test-user",
                        "read_at": "${System.currentTimeMillis()}"
                    }
                """.trimIndent())
            }
            
            assertTrue(response.status.isSuccess())
        }
    }

    @Test
    fun `test message reactions`() = testApplication {
        runBlocking {
            val response = client.post("/api/pb/collections/m_reactions/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "message_id": "test-message",
                        "user_id": "test-user",
                        "reaction": "❤️"
                    }
                """.trimIndent())
            }
            
            assertTrue(response.status.isSuccess())
        }
    }

    @Test
    fun `test presence status`() = testApplication {
        runBlocking {
            val response = client.post("/api/pb/collections/m_presence/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "user_id": "test-user",
                        "status": "online",
                        "last_active": "${System.currentTimeMillis()}"
                    }
                """.trimIndent())
            }
            
            assertTrue(response.status.isSuccess())
        }
    }
}

/**
 * Performance tests for high-load scenarios
 */
class PerformanceIntegrationTest {

    @Test
    fun `test concurrent message sending`() = testApplication {
        runBlocking {
            val jobs = (1..100).map { index ->
                kotlinx.coroutines.async {
                    client.post("/api/pb/collections/m_messages/records") {
                        contentType(ContentType.Application.Json)
                        setBody("""
                            {
                                "conversation_id": "perf-test",
                                "content": "Message $index",
                                "message_type": "text"
                            }
                        """.trimIndent())
                    }
                }
            }
            
            val responses = jobs.map { it.await() }
            val successCount = responses.count { it.status.isSuccess() }
            
            assertTrue(successCount >= 95, "At least 95% of messages should succeed")
        }
    }

    @Test
    fun `test rate limiting`() = testApplication {
        runBlocking {
            val responses = mutableListOf<HttpResponse>()
            
            // Send rapid requests to trigger rate limit
            repeat(20) {
                val response = client.get("/api/pb/collections/m_messages/records")
                responses.add(response)
            }
            
            // Should have some rate-limited responses
            val rateLimited = responses.any { it.status == HttpStatusCode.TooManyRequests }
            assertTrue(rateLimited, "Rate limiting should kick in")
        }
    }
}

/**
 * End-to-end user journey tests
 */
class UserJourneyIntegrationTest {

    @Test
    fun `test complete signup to messaging flow`() = testApplication {
        runBlocking {
            // 1. Sign up
            val signupResponse = client.post("/api/pb/collections/users/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "email": "newuser@test.com",
                        "password": "testpass123",
                        "passwordConfirm": "testpass123"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, signupResponse.status)

            // 2. Complete Proust questionnaire
            val proustResponse = client.post("/api/pb/collections/t_user_questionnaire_responses/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "user_id": "new-user-id",
                        "question_id": "q1",
                        "response": "My favorite color is blue"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, proustResponse.status)

            // 3. Create profile
            val profileResponse = client.post("/api/pb/collections/s_profiles/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "user_id": "new-user-id",
                        "display_name": "Test User",
                        "bio": "Hello Bside!"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, profileResponse.status)

            // 4. Start conversation
            val convResponse = client.post("/api/pb/collections/m_conversations/records") {
                contentType(ContentType.Application.Json)
                setBody("""{"conversation_type": "direct"}""")
            }
            assertEquals(HttpStatusCode.OK, convResponse.status)

            // 5. Send first message
            val messageResponse = client.post("/api/pb/collections/m_messages/records") {
                contentType(ContentType.Application.Json)
                setBody("""
                    {
                        "conversation_id": "conv-id",
                        "content": "Hi there!",
                        "message_type": "text"
                    }
                """.trimIndent())
            }
            assertEquals(HttpStatusCode.OK, messageResponse.status)
        }
    }
}
