package love.bside.server.repositories

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import love.bside.app.core.Result
import love.bside.app.data.api.PocketBaseClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserRepositoryTest {

    @Test
    fun authenticateUsesTUserCollection() = runBlocking {
        val paths = mutableListOf<String>()
        val hosts = mutableListOf<String>()
        val repository = repository(hosts, paths, AUTH_RESPONSE_JSON)

        val result = repository.authenticate("test@example.com", "password123")

        assertTrue(result is Result.Success)
        assertEquals(listOf("bside.pockethost.io"), hosts)
        assertEquals(listOf("/api/collections/users/auth-with-password"), paths)
    }

    @Test
    fun createUserUsesTUserCollection() = runBlocking {
        val paths = mutableListOf<String>()
        val hosts = mutableListOf<String>()
        val repository = repository(hosts, paths, USER_RECORD_JSON)

        val result = repository.createUser("test@example.com", "hash")

        assertTrue(result is Result.Success)
        assertEquals(listOf("bside.pockethost.io"), hosts)
        assertEquals(listOf("/api/collections/users/records"), paths)
    }

    private fun repository(
        hosts: MutableList<String>,
        paths: MutableList<String>,
        responseBody: String
    ): UserRepository {
        val engine = MockEngine { request ->
            hosts += request.url.host
            paths += request.url.fullPath
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        val pocketBaseClient = PocketBaseClient(httpClient, baseUrl = POCKETBASE_URL)
        return UserRepositoryImpl(pocketBaseClient)
    }

    companion object {
        private const val POCKETBASE_URL = "https://bside.pockethost.io/api/"
        private const val USER_RECORD_JSON = """
            {
              "id": "user1",
              "collectionId": "_pb_users_auth_",
              "collectionName": "users",
              "email": "test@example.com",
              "emailVisibility": false,
              "verified": true,
              "created": "2024-01-01T00:00:00Z",
              "updated": "2024-01-01T00:00:00Z"
            }
        """

        private const val AUTH_RESPONSE_JSON = """
            {
              "token": "pb-token",
              "record": $USER_RECORD_JSON
            }
        """
    }
}
