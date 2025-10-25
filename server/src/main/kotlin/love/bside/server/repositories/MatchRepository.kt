package love.bside.server.repositories

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.domain.Match
import love.bside.server.models.db.PBMatch
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

/**
 * Repository interface for match operations
 */
interface MatchRepository {
    suspend fun getMatchesForUser(userId: String): Result<List<Match>>
    suspend fun getMatchById(id: String): Result<Match>
    suspend fun createMatch(userId: String, matchedUserId: String, compatibilityScore: Double): Result<Match>
    suspend fun updateMatchStatus(id: String, status: String): Result<Match>
}

/**
 * Implementation using PocketBase
 */
class MatchRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : MatchRepository {
    
    private val collection = "s_matches"
    
    override suspend fun getMatchesForUser(userId: String): Result<List<Match>> {
        val filter = "userId = '$userId'"
        val expand = "matchedUserId"
        
        return when (val result = pocketBase.getList<PBMatch>(collection, filter = filter, expand = expand, perPage = 100)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getMatchById(id: String): Result<Match> {
        val expand = "matchedUserId"
        
        return when (val result = pocketBase.getOne<PBMatch>(collection, id, expand = expand)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun createMatch(userId: String, matchedUserId: String, compatibilityScore: Double): Result<Match> {
        val body = buildJsonObject {
            put("userId", userId)
            put("matchedUserId", matchedUserId)
            put("compatibilityScore", compatibilityScore)
            put("status", "PENDING")
        }
        
        return when (val result = pocketBase.create<JsonObject, PBMatch>(collection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun updateMatchStatus(id: String, status: String): Result<Match> {
        val body = buildJsonObject {
            put("status", status)
        }
        
        return when (val result = pocketBase.update<JsonObject, PBMatch>(collection, id, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
}
