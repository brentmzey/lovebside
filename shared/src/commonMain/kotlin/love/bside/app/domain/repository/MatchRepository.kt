package love.bside.app.domain.repository

import love.bside.app.domain.models.Match
import love.bside.app.domain.models.Prompt

interface MatchRepository {
    suspend fun getMatches(userId: String): Result<List<Match>>
    suspend fun updateMatchStatus(matchId: String, status: String): Result<Unit>
    suspend fun getPromptsForMatch(matchId: String): Result<List<Prompt>>
}
