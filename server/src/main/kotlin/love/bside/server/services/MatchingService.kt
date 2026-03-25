package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.models.api.MatchDTO
import love.bside.server.models.api.UserDTO
import love.bside.server.models.api.ProfileDTO
import love.bside.server.models.api.DiscoverMatchesResponse
import love.bside.server.repositories.MatchRepository
import love.bside.server.repositories.UserRepository
import love.bside.server.repositories.ProfileRepository
import love.bside.server.matching.MatchDiscoveryService
import love.bside.server.utils.toDTO

/**
 * Service for matching operations
 */
class MatchingService(
    private val matchRepository: MatchRepository,
    private val userRepository: UserRepository,
    private val profileRepository: ProfileRepository,
    private val discoveryService: MatchDiscoveryService
) {
    
    /**
     * Get user's matches
     */
    suspend fun getMatches(userId: String): List<MatchDTO> {
        val matches = when (val result = matchRepository.getMatchesForUser(userId)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to get matches: ${result.exception.message}")
            is Result.Loading -> throw Exception("Matches lookup is still loading")
        }
        
        // Transform to DTOs with expanded user data
        return matches.map { match ->
            val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
                is Result.Success -> userResult.data
                is Result.Error -> null
                is Result.Loading -> null
            }
            
            val matchedProfile = matchedUser?.let { user ->
                when (val profileResult = profileRepository.getProfileByUserId(user.id)) {
                    is Result.Success -> profileResult.data
                    is Result.Error -> null
                    is Result.Loading -> null
                }
            }
            
            if (matchedUser != null) {
                match.toDTO(matchedUser, matchedProfile, emptyList())
            } else {
                null
            }
        }.filterNotNull()
    }
    
    /**
     * Discover new matches using the algorithmic matching engine
     */
    suspend fun discoverMatches(userId: String, limit: Int = 10): DiscoverMatchesResponse {
        val candidates = discoveryService.discoverCandidates(userId, limit)
        
        val matchDTOs = candidates.mapNotNull { score ->
            val matchedUser = when (val userResult = userRepository.getUserById(score.user2Id)) {
                is Result.Success -> userResult.data
                is Result.Error -> null
                is Result.Loading -> null
            } ?: return@mapNotNull null
            
            val matchedProfile = when (val profileResult = profileRepository.getProfileByUserId(score.user2Id)) {
                is Result.Success -> profileResult.data
                is Result.Error -> null
                is Result.Loading -> null
            }
            
            val userDTO = UserDTO(
                id = matchedUser.id,
                email = matchedUser.email,
                profile = matchedProfile?.let { p ->
                    ProfileDTO(
                        firstName = p.firstName,
                        lastName = p.lastName,
                        age = p.age,
                        bio = p.bio,
                        location = p.location,
                        seeking = love.bside.server.models.api.SeekingTypeDTO.fromString(p.seeking.name)
                    )
                }
            )
            
            MatchDTO(
                id = "",
                user = userDTO,
                compatibilityScore = score.compositeScore,
                sharedValues = emptyList(),
                status = love.bside.server.models.api.MatchStatusDTO.DISCOVERED,
                createdAt = kotlinx.datetime.Clock.System.now().toString()
            )
        }
        
        return DiscoverMatchesResponse(
            matches = matchDTOs,
            hasMore = candidates.size >= limit
        )
    }
    
    /**
     * Like a match
     */
    suspend fun likeMatch(matchId: String): MatchDTO {
        val match = when (val result = matchRepository.updateMatchStatus(matchId, "LIKED")) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to like match: ${result.exception.message}")
            is Result.Loading -> throw Exception("Match update is still loading")
        }
        
        val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
            is Result.Success -> userResult.data
            is Result.Error -> throw Exception("User not found")
            is Result.Loading -> throw Exception("User lookup is still loading")
        }
        
        val matchedProfile = when (val profileResult = profileRepository.getProfileByUserId(matchedUser.id)) {
            is Result.Success -> profileResult.data
            is Result.Error -> null
            is Result.Loading -> null
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
            is Result.Loading -> throw Exception("Match update is still loading")
        }
        
        val matchedUser = when (val userResult = userRepository.getUserById(match.matchedUserId)) {
            is Result.Success -> userResult.data
            is Result.Error -> throw Exception("User not found")
            is Result.Loading -> throw Exception("User lookup is still loading")
        }
        
        val matchedProfile = when (val profileResult = profileRepository.getProfileByUserId(matchedUser.id)) {
            is Result.Success -> profileResult.data
            is Result.Error -> null
            is Result.Loading -> null
        }
        
        return match.toDTO(matchedUser, matchedProfile, emptyList())
    }
}

