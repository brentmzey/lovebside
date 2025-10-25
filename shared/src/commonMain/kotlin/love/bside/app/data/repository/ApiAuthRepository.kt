package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.api.RegisterRequest
import love.bside.app.data.mappers.toDomain
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.Profile
import love.bside.app.domain.repository.AuthRepository

/**
 * API-based implementation of AuthRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiAuthRepository(
    private val apiClient: InternalApiClient
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Result<AuthDetails> {
        return apiClient.login(email, password).map { authResponse ->
            val profile = authResponse.user.profile?.toDomain(
                userId = authResponse.user.id,
                profileId = authResponse.user.id
            ) ?: createEmptyProfile(authResponse.user.id)
            
            AuthDetails(
                token = authResponse.token,
                profile = profile
            )
        }
    }
    
    override suspend fun signUp(
        email: String,
        password: String,
        passwordConfirm: String
    ): Result<AuthDetails> {
        // For signup, we need additional profile info
        // This is a simplified version - you may want to pass more parameters
        val request = RegisterRequest(
            email = email,
            password = password,
            passwordConfirm = passwordConfirm,
            firstName = "",
            lastName = "",
            birthDate = "2000-01-01",
            seeking = "BOTH"
        )
        
        return apiClient.register(request).map { authResponse ->
            val profile = authResponse.user.profile?.toDomain(
                userId = authResponse.user.id,
                profileId = authResponse.user.id
            ) ?: createEmptyProfile(authResponse.user.id)
            
            AuthDetails(
                token = authResponse.token,
                profile = profile
            )
        }
    }
    
    override suspend fun logout() {
        apiClient.logout()
    }
    
    /**
     * Create an empty profile when user data doesn't include profile
     */
    private fun createEmptyProfile(userId: String): Profile {
        val now = kotlinx.datetime.Clock.System.now()
        return Profile(
            id = userId,
            created = now,
            updated = now,
            userId = userId,
            firstName = "",
            lastName = "",
            birthDate = kotlinx.datetime.LocalDate(2000, 1, 1),
            bio = null,
            location = null,
            seeking = love.bside.app.domain.models.SeekingStatus.BOTH
        )
    }
}
