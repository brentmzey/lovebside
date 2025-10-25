package love.bside.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import love.bside.app.core.Result
import love.bside.app.core.logError
import love.bside.app.core.logInfo
import love.bside.app.core.network.retryable
import love.bside.app.core.validation.ValidationResult
import love.bside.app.core.validation.Validators
import love.bside.app.data.models.AuthRecord
import love.bside.app.data.models.toDomain
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.TokenStorage
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.repository.AuthRepository

@Serializable
data class LoginRequest(val identity: String, val password: String)

@Serializable
data class SignUpRequest(val email: String, val password: String, val passwordConfirm: String)

class PocketBaseAuthRepository(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage,
    private val sessionManager: SessionManager
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<AuthDetails> {
        logInfo("Attempting login for user: $email")
        
        // Validate input
        val emailValidation = Validators.validateEmail(email)
        if (!emailValidation.isValid) {
            return Result.Error(emailValidation.getErrorOrNull()!!)
        }
        
        val passwordValidation = Validators.validateRequired(password, "Password")
        if (!passwordValidation.isValid) {
            return Result.Error(passwordValidation.getErrorOrNull()!!)
        }

        return retryable {
            val request = LoginRequest(email, password)
            val authRecord = client.post("collections/users/auth-with-password") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<AuthRecord>()

            tokenStorage.saveToken(authRecord.token)
            sessionManager.saveSession(authRecord.record.toDomain())
            logInfo("Login successful for user: $email")
            authRecord.toDomain()
        }
    }

    override suspend fun signUp(email: String, password: String, passwordConfirm: String): Result<AuthDetails> {
        logInfo("Attempting signup for user: $email")
        
        // Validate input
        val emailValidation = Validators.validateEmail(email)
        if (!emailValidation.isValid) {
            return Result.Error(emailValidation.getErrorOrNull()!!)
        }
        
        val passwordValidation = Validators.validatePassword(password)
        if (!passwordValidation.isValid) {
            return Result.Error(passwordValidation.getErrorOrNull()!!)
        }
        
        if (password != passwordConfirm) {
            return Result.Error(
                love.bside.app.core.AppException.Validation.InvalidInput(
                    "Password confirmation",
                    "passwords do not match"
                )
            )
        }

        return retryable {
            val request = SignUpRequest(email, password, passwordConfirm)
            client.post("collections/users/records") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<AuthRecord>()
            
            logInfo("Signup successful for user: $email, attempting auto-login")
            // After signing up, log in to get a token
            login(email, password).getOrThrow()
        }
    }

    override suspend fun logout() {
        logInfo("Logging out user")
        tokenStorage.clearToken()
        sessionManager.clearSession()
    }
}
