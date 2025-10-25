package love.bside.app.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.appConfig
import love.bside.app.core.logDebug
import love.bside.app.core.logError
import love.bside.app.data.storage.TokenStorage

/**
 * Client for communicating with the internal B-Side API
 * This is the ONLY client that should be used by the app - 
 * clients should never talk directly to PocketBase
 */
class InternalApiClient(
    private val tokenStorage: TokenStorage
) {
    private val config = appConfig()
    
    // Base URL for the internal API - configurable per environment
    private val baseUrl = when (config.environment) {
        love.bside.app.core.Environment.DEVELOPMENT -> "http://localhost:8080/api/v1"
        love.bside.app.core.Environment.STAGING -> "https://staging.bside.love/api/v1"
        love.bside.app.core.Environment.PRODUCTION -> "https://www.bside.love/api/v1"
    }
    
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = config.apiTimeout
            connectTimeoutMillis = config.apiTimeout / 2
            socketTimeoutMillis = config.apiTimeout
        }
        
        if (config.enableLogging) {
            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        this.logDebug(message)
                    }
                }
                level = when (config.environment) {
                    love.bside.app.core.Environment.DEVELOPMENT -> LogLevel.ALL
                    love.bside.app.core.Environment.STAGING -> LogLevel.HEADERS
                    love.bside.app.core.Environment.PRODUCTION -> LogLevel.NONE
                }
            }
        }
        
        defaultRequest {
            url(baseUrl)
        }
    }
    
    // ===== Authentication =====
    
    suspend fun register(request: RegisterRequest): Result<AuthResponse> = safeApiCall {
        val response = client.post("/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<AuthResponse>>()
        
        if (response.success && response.data != null) {
            // Store tokens
            tokenStorage.saveToken(response.data.token)
            tokenStorage.saveRefreshToken(response.data.refreshToken)
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("register", response.error?.message ?: "Registration failed"))
        }
    }
    
    suspend fun login(email: String, password: String): Result<AuthResponse> = safeApiCall {
        val response = client.post("/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(LoginRequest(email, password))
        }.body<ApiResponse<AuthResponse>>()
        
        if (response.success && response.data != null) {
            // Store tokens
            tokenStorage.saveToken(response.data.token)
            tokenStorage.saveRefreshToken(response.data.refreshToken)
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Auth.InvalidCredentials())
        }
    }
    
    suspend fun refreshToken(): Result<AuthResponse> = safeApiCall {
        val refreshToken = tokenStorage.getRefreshToken()
            ?: return@safeApiCall Result.Error(AppException.Auth.SessionExpired())
        
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshTokenRequest(refreshToken))
        }.body<ApiResponse<AuthResponse>>()
        
        if (response.success && response.data != null) {
            // Update tokens
            tokenStorage.saveToken(response.data.token)
            tokenStorage.saveRefreshToken(response.data.refreshToken)
            Result.Success(response.data)
        } else {
            // Refresh failed - clear tokens
            tokenStorage.clearTokens()
            Result.Error(AppException.Auth.SessionExpired())
        }
    }
    
    suspend fun logout(): Result<Unit> {
        tokenStorage.clearTokens()
        return Result.Success(Unit)
    }
    
    // ===== User =====
    
    suspend fun getCurrentUser(): Result<UserDTO> = safeApiCall {
        val response = client.get("/users/me") {
            bearerAuth(getToken())
        }.body<ApiResponse<UserDTO>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("User"))
        }
    }
    
    suspend fun updateProfile(request: UpdateProfileRequest): Result<UserDTO> = safeApiCall {
        val response = client.put("/users/me") {
            bearerAuth(getToken())
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body<ApiResponse<UserDTO>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("profile update", response.error?.message))
        }
    }
    
    suspend fun deleteAccount(): Result<Unit> = safeApiCall {
        client.delete("/users/me") {
            bearerAuth(getToken())
        }
        tokenStorage.clearTokens()
        Result.Success(Unit)
    }
    
    // ===== Values =====
    
    suspend fun getAllKeyValues(category: String? = null): Result<List<KeyValueDTO>> = safeApiCall {
        val response = client.get("/values") {
            category?.let { parameter("category", it) }
        }.body<ApiResponse<List<KeyValueDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("Values"))
        }
    }
    
    suspend fun getUserValues(): Result<List<UserValueDTO>> = safeApiCall {
        val response = client.get("/users/me/values") {
            bearerAuth(getToken())
        }.body<ApiResponse<List<UserValueDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("User values"))
        }
    }
    
    suspend fun saveUserValues(values: List<UserValueInput>): Result<List<UserValueDTO>> = safeApiCall {
        val response = client.post("/users/me/values") {
            bearerAuth(getToken())
            contentType(ContentType.Application.Json)
            setBody(SaveUserValuesRequest(values))
        }.body<ApiResponse<List<UserValueDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("save values", response.error?.message))
        }
    }
    
    // ===== Matches =====
    
    suspend fun getMatches(): Result<List<MatchDTO>> = safeApiCall {
        val response = client.get("/matches") {
            bearerAuth(getToken())
        }.body<ApiResponse<List<MatchDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("Matches"))
        }
    }
    
    suspend fun discoverMatches(limit: Int = 10): Result<DiscoverMatchesResponse> = safeApiCall {
        val response = client.get("/matches/discover") {
            bearerAuth(getToken())
            parameter("limit", limit)
        }.body<ApiResponse<DiscoverMatchesResponse>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("discover matches", response.error?.message))
        }
    }
    
    suspend fun likeMatch(matchId: String): Result<MatchDTO> = safeApiCall {
        val response = client.post("/matches/$matchId/like") {
            bearerAuth(getToken())
        }.body<ApiResponse<MatchDTO>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("like match", response.error?.message))
        }
    }
    
    suspend fun passMatch(matchId: String): Result<MatchDTO> = safeApiCall {
        val response = client.post("/matches/$matchId/pass") {
            bearerAuth(getToken())
        }.body<ApiResponse<MatchDTO>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("pass match", response.error?.message))
        }
    }
    
    // ===== Prompts =====
    
    suspend fun getAllPrompts(): Result<List<PromptDTO>> = safeApiCall {
        val response = client.get("/prompts")
            .body<ApiResponse<List<PromptDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("Prompts"))
        }
    }
    
    suspend fun getUserAnswers(): Result<List<PromptAnswerDTO>> = safeApiCall {
        val response = client.get("/users/me/answers") {
            bearerAuth(getToken())
        }.body<ApiResponse<List<PromptAnswerDTO>>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.ResourceNotFound("Answers"))
        }
    }
    
    suspend fun submitAnswer(promptId: String, answer: String): Result<PromptAnswerDTO> = safeApiCall {
        val response = client.post("/users/me/answers") {
            bearerAuth(getToken())
            contentType(ContentType.Application.Json)
            setBody(SubmitAnswerRequest(promptId, answer))
        }.body<ApiResponse<PromptAnswerDTO>>()
        
        if (response.success && response.data != null) {
            Result.Success(response.data)
        } else {
            Result.Error(AppException.Business.OperationNotAllowed("submit answer", response.error?.message))
        }
    }
    
    // ===== Health Check =====
    
    suspend fun healthCheck(): Result<HealthResponse> = safeApiCall {
        val response = client.get("/health").body<HealthResponse>()
        Result.Success(response)
    }
    
    // ===== Helper Methods =====
    
    private suspend fun getToken(): String {
        val token = tokenStorage.getToken()
        if (token.isNullOrBlank()) {
            throw AppException.Auth.Unauthorized()
        }
        return token
    }
    
    private suspend fun <T> safeApiCall(block: suspend () -> Result<T>): Result<T> {
        return try {
            block()
        } catch (e: AppException) {
            logError("API call failed: ${e.message}")
            Result.Error(e)
        } catch (e: HttpRequestTimeoutException) {
            logError("API timeout: ${e.message}")
            Result.Error(AppException.Network.Timeout())
        } catch (e: Exception) {
            logError("API call error: ${e.message}")
            Result.Error(AppException.Network.ServerError(0, e.message ?: "Network error"))
        }
    }
}
