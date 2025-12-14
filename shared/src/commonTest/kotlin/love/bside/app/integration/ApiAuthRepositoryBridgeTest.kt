package love.bside.app.integration

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.repository.ApiAuthRepository
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.models.SignUpData

class ApiAuthRepositoryBridgeTest {

    @Test
    fun loginRoutesThroughBackendApi() = runTest {
        val capturedPaths = mutableListOf<String>()
        val capturedHosts = mutableListOf<String>()
        val repository = repository { request ->
            capturedHosts += request.url.host
            capturedPaths += request.url.fullPath
        }

        // We expect this to fail parsing result in mock because we don't return full PB auth response structure
        // But we only care about the request being made to the right URL
        try {
            repository.login("test@example.com", "Sup3rSecret!")
        } catch (e: Exception) {
            // Ignore serialization errors in this bridge test
        }

        assertEquals(listOf("api.client.test"), capturedHosts)
        // Check for PocketBase Auth URL - using System ID
        assertEquals(listOf("/api/collections/_pb_users_auth_/auth-with-password"), capturedPaths)
    }

    @Test
    fun signUpRoutesThroughBackendApi() = runTest {
        val capturedPaths = mutableListOf<String>()
        val capturedHosts = mutableListOf<String>()
        val repository = repository { request ->
            capturedHosts += request.url.host
            capturedPaths += request.url.fullPath
        }

        try {
            repository.signUp(
                SignUpData(
                    email = "new@example.com",
                    password = "Sup3rSecret!",
                    passwordConfirm = "Sup3rSecret!",
                    firstName = "New",
                    lastName = "User",
                    birthDate = kotlinx.datetime.LocalDate(2000, 1, 1),
                    seeking = SeekingStatus.BOTH
                )
            )
        } catch (e: Exception) {
            // Ignore serialization errors
        }

        assertEquals(listOf("api.client.test"), capturedHosts)
        // Should see 3 calls: Create User -> Auth -> Create Profile
        // We check if the path contains the system ID now
        assertTrue(capturedPaths.first().contains("/api/collections/_pb_users_auth_/records"))
    }

    private fun repository(
        onRequest: (HttpRequestData) -> Unit
    ): ApiAuthRepository {
        val engine = MockEngine { request ->
            onRequest(request)
            respondAuthEnvelope()
        }
        val tokenStorage = MockTokenStorage()
        val apiClient = InternalApiClient(
            tokenStorage = tokenStorage,
            engine = engine,
            baseUrlOverride = TEST_BASE_URL
        )
        // PB Client with same mock engine
        val pbClient = love.bside.app.data.api.PocketBaseClient(
             client = io.ktor.client.HttpClient(engine),
             baseUrl = "https://api.client.test/api/"
        )
        
        return ApiAuthRepository(apiClient, pbClient, tokenStorage)
    }

    private fun MockRequestHandleScope.respondAuthEnvelope() = respond(
        content = SUCCESSFUL_AUTH_JSON,
        status = HttpStatusCode.OK,
        headers = Headers.build { append(HttpHeaders.ContentType, "application/json") }
    )

    companion object {
        private const val TEST_BASE_URL = "https://api.client.test/api/v42"
        private const val SUCCESSFUL_AUTH_JSON = """
            {
              "token": "access-token",
              "record": {
                 "id": "u1",
                 "email": "test@example.com"
              },
              "items": []
            }
        """
    }
}
