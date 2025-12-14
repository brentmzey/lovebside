package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.api.RegisterRequest
import love.bside.app.data.mappers.toDomain
import love.bside.app.data.models.toDomain
import love.bside.app.data.models.Profile as DataProfile
import love.bside.app.domain.models.Profile as DomainProfile
import love.bside.app.data.storage.TokenStorage
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.repository.AuthRepository

/**
 * API-based implementation of AuthRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiAuthRepository(
    private val apiClient: InternalApiClient,
    private val pocketBaseClient: PocketBaseClient,
    private val tokenStorage: TokenStorage
) : AuthRepository {
    
    @kotlinx.serialization.Serializable
    data class PocketBaseUser(
        val id: String,
        val email: String,
        val created: String,
        val updated: String,
        val verified: Boolean
    )

    @kotlinx.serialization.Serializable
    data class CreateUserRequest(
        val email: String,
        val password: String,
        val passwordConfirm: String,
        val name: String
    )

    @kotlinx.serialization.Serializable
    data class CreateProfileRequest(
        val userId: String,
        val firstName: String,
        val lastName: String,
        val birthDate: String,
        val seeking: String
    )
    
    override suspend fun login(email: String, password: String): Result<AuthDetails> {
        // Use PB SDK to login
        // Use system collection ID _pb_users_auth_ instead of name "users" to be safe against renames
        return pocketBaseClient.authWithPassword<PocketBaseUser>("_pb_users_auth_", email, password).map { authResponse ->
            // Save token
            tokenStorage.saveToken(authResponse.token)
            
            // Try to fetch profile from s_profiles
            // We assume 1:1 relation, searching by userId
            val profileResult = pocketBaseClient.getFirstListItem<DataProfile>(
                "s_profiles", 
                "userId='${authResponse.record.id}'"
            )
            
            val profile = if (profileResult is Result.Success) {
                profileResult.data.toDomain()
            } else {
                createEmptyProfile(authResponse.record.id)
            }
            
            AuthDetails(
                token = authResponse.token,
                profile = profile
            )
        }
    }
    
    override suspend fun signUp(data: SignUpData): Result<AuthDetails> {
        // 1. Create User
        val createUserRequest = CreateUserRequest(
            email = data.email,
            password = data.password,
            passwordConfirm = data.passwordConfirm,
            name = "${data.firstName} ${data.lastName}"
        )
        
        // Use system collection ID _pb_users_auth_
        val createUserResult = pocketBaseClient.create<CreateUserRequest, PocketBaseUser>("_pb_users_auth_", createUserRequest)
        
        if (createUserResult is Result.Error) return Result.Error(createUserResult.exception)
        val user = (createUserResult as Result.Success).data
        
        // 2. Authenticate to get token
        val authResult = pocketBaseClient.authWithPassword<PocketBaseUser>("_pb_users_auth_", data.email, data.password)
        if (authResult is Result.Error) return Result.Error(authResult.exception)
        
        val authResponse = (authResult as Result.Success).data
        tokenStorage.saveToken(authResponse.token)
        
        // 3. Create Profile
        val createProfileRequest = CreateProfileRequest(
            userId = user.id,
            firstName = data.firstName,
            lastName = data.lastName,
            birthDate = data.birthDate.toString(),
            seeking = data.seeking.name
        )
        
        val createProfileResult = pocketBaseClient.create<CreateProfileRequest, DataProfile>("s_profiles", createProfileRequest)
        
        val profile = if (createProfileResult is Result.Success) {
            createProfileResult.data.toDomain()
        } else {
            // Fallback if profile creation failed but user created? 
            // Ideally should rollback user, but for now just return empty profile
            createEmptyProfile(user.id)
        }

        return Result.Success(
            AuthDetails(
                token = authResponse.token,
                profile = profile
            )
        )
    }
    
    override suspend fun logout() {
        apiClient.logout()
    }
    
    /**
     * Create an empty profile when user data doesn't include profile
     */
    private fun createEmptyProfile(userId: String): DomainProfile {
        val now = kotlinx.datetime.Clock.System.now()
        return DomainProfile(
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
