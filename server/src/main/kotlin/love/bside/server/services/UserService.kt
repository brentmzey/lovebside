package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.models.api.UserDTO
import love.bside.server.models.api.ProfileDTO
import love.bside.server.models.api.UpdateProfileRequest
import love.bside.server.plugins.NotFoundException
import love.bside.server.repositories.UserRepository
import love.bside.server.repositories.ProfileRepository
import love.bside.server.utils.toDTO

/**
 * Service for user operations
 */
class UserService(
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) {
    
    /**
     * Get current user details
     */
    suspend fun getCurrentUser(userId: String): UserDTO {
        val user = when (val result = userRepository.getUserById(userId)) {
            is Result.Success -> result.data
            is Result.Error -> throw NotFoundException("User not found")
            is Result.Loading -> throw NotFoundException("User lookup is still loading")
        }
        
        val profile = when (val result = profileRepository.getProfileByUserId(userId)) {
            is Result.Success -> result.data
            is Result.Error -> null
            is Result.Loading -> null
        }
        
        return user.toDTO(profile)
    }
    
    /**
     * Update user profile
     */
    suspend fun updateProfile(userId: String, request: UpdateProfileRequest): ProfileDTO {
        // Build updates map
        val updates = mutableMapOf<String, Any>()
        request.firstName?.let { updates["firstName"] = it }
        request.lastName?.let { updates["lastName"] = it }
        request.bio?.let { updates["bio"] = it }
        request.location?.let { updates["location"] = it }
        request.seeking?.let { updates["seeking"] = it }
        
        val profile = when (val result = profileRepository.updateProfile(userId, updates)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to update profile: ${result.exception.message}")
            is Result.Loading -> throw Exception("Profile update is still loading")
        }
        
        return profile.toDTO()
    }
    
    /**
     * Delete user account
     */
    suspend fun deleteUser(userId: String) {
        // Delete profile first
        profileRepository.deleteProfile(userId)
        
        // Then delete user
        when (userRepository.deleteUser(userId)) {
            is Result.Success -> {}
            is Result.Error -> throw Exception("Failed to delete user")
            is Result.Loading -> throw Exception("User deletion is still loading")
        }
    }
}
