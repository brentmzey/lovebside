package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.core.logInfo
import love.bside.app.core.validation.Validators
import love.bside.app.data.DatabaseCollections
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.models.Profile
import love.bside.app.data.models.toDomain
import love.bside.app.data.storage.SessionManager
import love.bside.app.data.storage.TokenStorage
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.models.SeekingStatus as DomainSeekingStatus
import love.bside.app.data.models.SeekingStatus as DataSeekingStatus
import love.bside.app.domain.repository.AuthRepository
import love.bside.app.core.AppException
import kotlinx.serialization.Serializable

@Serializable
private data class SignUpRequest(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val seeking: DataSeekingStatus
)

class PocketBaseAuthRepository(
    private val client: PocketBaseClient,
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

        return client.authWithPassword<Profile>(
            collection = DatabaseCollections.USERS,
            identity = email,
            password = password
        ).map { authResponse ->
            tokenStorage.saveToken(authResponse.token)
            val domainProfile = authResponse.record.toDomain()
            val authDetails = AuthDetails(
                token = authResponse.token,
                profile = domainProfile
            )
            sessionManager.saveSession(authDetails.profile)
            logInfo("Login successful for user: $email")
            authDetails
        }
    }

    override suspend fun signUp(data: SignUpData): Result<AuthDetails> {
        val email = data.email
        logInfo("Attempting signup for user: $email")
        
        // Validate input
        val emailValidation = Validators.validateEmail(email)
        if (!emailValidation.isValid) {
            return Result.Error(emailValidation.getErrorOrNull()!!)
        }
        
        val passwordValidation = Validators.validatePassword(data.password)
        if (!passwordValidation.isValid) {
            return Result.Error(passwordValidation.getErrorOrNull()!!)
        }
        
        if (data.password != data.passwordConfirm) {
            return Result.Error(
                AppException.Validation.InvalidInput(
                    "Password confirmation",
                    "passwords do not match"
                )
            )
        }

        val request = SignUpRequest(
            email = data.email,
            password = data.password,
            passwordConfirm = data.passwordConfirm,
            firstName = data.firstName,
            lastName = data.lastName,
            birthDate = data.birthDate.toString(), // YYYY-MM-DD
            seeking = when (data.seeking) {
                DomainSeekingStatus.FRIENDSHIP -> DataSeekingStatus.FRIENDSHIP
                DomainSeekingStatus.RELATIONSHIP -> DataSeekingStatus.RELATIONSHIP
                DomainSeekingStatus.BOTH -> DataSeekingStatus.BOTH
            }
        )

        return client.create<SignUpRequest, Profile>(
            collection = DatabaseCollections.USERS,
            body = request
        ).flatMap {
            logInfo("Signup successful for user: $email, attempting auto-login")
            login(email, data.password)
        }
    }

    override suspend fun logout() {
        logInfo("Logging out user")
        tokenStorage.clearToken()
        sessionManager.clearSession()
        // Optionally clear specific user data from other caches here
    }
}
