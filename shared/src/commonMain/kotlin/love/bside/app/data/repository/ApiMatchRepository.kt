package love.bside.app.data.repository

import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.mappers.toDomain
import love.bside.app.domain.models.Match
import love.bside.app.domain.models.Prompt
import love.bside.app.domain.repository.MatchRepository

/**
 * API-based implementation of MatchRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiMatchRepository(
    private val apiClient: InternalApiClient
) : MatchRepository {
    
    override suspend fun getMatches(userId: String): Result<List<Match>> {
        return apiClient.getMatches().map { matchDTOs ->
            matchDTOs.map { it.toDomain(userId) }
        }
    }
    
    override suspend fun updateMatchStatus(matchId: String, status: String): Result<Unit> {
        // The API uses separate endpoints for like/pass
        return when (status.uppercase()) {
            "LIKED" -> apiClient.likeMatch(matchId).map { Unit }
            "PASSED" -> apiClient.passMatch(matchId).map { Unit }
            else -> Result.Error(
                AppException.Business.OperationNotAllowed(
                    "update match",
                    "Invalid status: $status"
                )
            )
        }
    }
    
    override suspend fun getPromptsForMatch(matchId: String): Result<List<Prompt>> {
        // The API returns all prompts, not per-match
        // We'll get all prompts and associate them with the match
        return apiClient.getAllPrompts().map { promptDTOs ->
            promptDTOs.map { it.toDomain(matchId = matchId) }
        }
    }
}
