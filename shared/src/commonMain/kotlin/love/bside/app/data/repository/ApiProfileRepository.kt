package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.api.UpdateProfileRequest
import love.bside.app.data.mappers.toDomain
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.domain.models.Profile
import love.bside.app.domain.repository.ProfileRepository

/**
 * API-based implementation of ProfileRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiProfileRepository(
    private val apiClient: InternalApiClient
) : ProfileRepository {
    
    override suspend fun getProfile(userId: String): Result<Profile> {
        return apiClient.getCurrentUser().map { userDTO ->
            userDTO.profile?.toDomain(
                userId = userDTO.id,
                profileId = userDTO.id
            ) ?: throw IllegalStateException("Profile not found for user")
        }
    }
    
    override suspend fun createProfile(profile: Profile): Result<Unit> {
        // Profile creation happens during registration in the API
        // This method is mainly for compatibility
        val request = UpdateProfileRequest(
            firstName = profile.firstName,
            lastName = profile.lastName,
            bio = profile.bio,
            location = profile.location,
            seeking = profile.seeking.name
        )
        
        return apiClient.updateProfile(request).map { 
            Unit
        }
    }
    
    override suspend fun updateProfile(
        userId: String,
        request: ProfileUpdateRequest
    ): Result<Profile> {
        val apiRequest = UpdateProfileRequest(
            firstName = request.firstName,
            lastName = request.lastName,
            bio = request.bio,
            location = request.location,
            seeking = request.seeking?.name
        )
        
        return apiClient.updateProfile(apiRequest).map { userDTO ->
            userDTO.profile?.toDomain(
                userId = userDTO.id,
                profileId = userDTO.id
            ) ?: throw IllegalStateException("Profile not found after update")
        }
    }
}
