package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.models.api.MatchDTO
import love.bside.server.models.api.DiscoverMatchesResponse
import love.bside.server.repositories.MatchRepository
import love.bside.server.repositories.UserRepository
import love.bside.server.repositories.ProfileRepository
import love.bside.server.utils.toDTO

/**
 * Service for matching operations
 */
class MatchingService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository
) {
    
    /**
     * Get user's matches
     */
    suspend fun getMatches(userId: String): List<MatchDTO> {
        val matches = when (val result = matchRepository.getMatchesForUser(userId)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to get matches: ${result.exception.message}")
        }
        
        // Transform to DTOs with expanded user data
        return matches.map { match ->
            val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
                is Result.Success -> userResult.data
                is Result.Error -> null
            }
            
            val matchedProfile = matchedUser?.let { user ->
                when (val profileResult = profileRepository.getProfileByUserId(user.id)) {
                    is Result.Success -> profileResult.data
                    is Result.Error -> null
                }
            }
            
            if (matchedUser != null) {
                match.toDTO(matchedUser, matchedProfile, emptyList()) // TODO: Add shared values
            } else {
                null
            }
        }.filterNotNull()
    }
    
    /**
     * Discover new matches
     */
    suspend fun discoverMatches(userId: String, limit: Int = 10): DiscoverMatchesResponse {
        // TODO: Implement matching algorithm
        // For now, return empty list
        return DiscoverMatchesResponse(
            matches = emptyList(),
            hasMore = false
        )
    }
    
    /**
     * Like a match
     */
    suspend fun likeMatch(matchId: String): MatchDTO {
        val match = when (val result = matchRepository.updateMatchStatus(matchId, "LIKED")) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to like match: ${result.exception.message}")
        }
        
        // Get matched user details
        val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
            is Result.Success -> userResult.data
            is Result.Error -> throw Exception("User not found")
        }
        
        val matchedProfile = when (val profileResult = profileRepository.getProfileByUserId(matchedUser.id)) {
            is Result.Success -> profileResult.data
            is Result.Error -> null
        }
        
        return match.toDTO(matchedUser, matchedProfile, emptyList())
    }
    
    /**
     * Pass on a match
     */
    suspend fun passMatch(matchId: String): MatchDTO {
        val match = when (val result = matchRepository.updateMatchStatus(matchId, "PASSED")) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to pass match: ${result.exception.message}")
        }
        
        // Get matched user details
        val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
            is Result.Success -> userResult.data
            is Result.Error -> throw Exception("User not found")
        }
        
        val matchedProfile = when (val profileResult = profileRepository.getProfileByUserId(matchedUser.id)) {
            is Result.Success -> profileResult.data
            is Result.Error -> null
        }
        
        return match.toDTO(matchedUser, matchedProfile, emptyList())
    }
}
