package love.bside.server.matching

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject

/**
 * Match Discovery Service
 * 
 * Orchestrates the multi-dimensional scoring pipeline to discover
 * and rank potential matches for a user.
 * 
 * Pipeline:
 * 1. Fetch all candidate users (excluding already swiped/matched)
 * 2. Run each ScoreDimension against every candidate
 * 3. Compute composite weighted score
 * 4. Return ranked results
 */
class MatchDiscoveryService(
    private val pocketBase: PocketBaseClient,
    private val scorers: List<ScoreDimension>
) {
    
    /**
     * Discover top match candidates for a user.
     * Excludes users already swiped on or matched with.
     */
    suspend fun discoverCandidates(userId: String, limit: Int = 20): List<CompositeMatchScore> {
        val candidates = getCandidateUserIds(userId)
        if (candidates.isEmpty()) return emptyList()
        
        val scored = candidates.map { candidateId ->
            scoreCandidate(userId, candidateId)
        }
        
        return scored
            .filter { it.compositeScore > 0.0 }
            .sortedByDescending { it.compositeScore }
            .take(limit)
    }
    
    /**
     * Score a single candidate against all dimensions
     */
    suspend fun scoreCandidate(userId: String, candidateId: String): CompositeMatchScore {
        val weightedScores = scorers.map { scorer ->
            try {
                val score = scorer.score(userId, candidateId)
                WeightedScore(
                    dimension = scorer.name,
                    rawScore = score.coerceIn(0.0, 1.0),
                    weight = scorer.defaultWeight
                )
            } catch (e: Exception) {
                // Individual scorer failure shouldn't derail the whole pipeline
                WeightedScore(dimension = scorer.name, rawScore = 0.0, weight = scorer.defaultWeight)
            }
        }
        
        return CompositeMatchScore.compute(userId, candidateId, weightedScores)
    }
    
    /**
     * Get candidate user IDs, excluding:
     * - The user themselves
     * - Users already swiped on
     * - Users already matched with
     */
    private suspend fun getCandidateUserIds(userId: String): List<String> {
        // Use s_profiles instead of users — auth collections are restricted by default
        val allUsers = when (val result = pocketBase.getList<ProfileUserRecord>("s_profiles", perPage = 200)) {
            is Result.Success -> result.data.items.map { it.user }
            else -> return emptyList()
        }
        
        // Get already swiped user IDs
        val swipedFilter = "swiper = '$userId'"
        val alreadySwiped = when (val result = pocketBase.getList<SwipeRecord>(
            "m_swipes", filter = swipedFilter, perPage = 500
        )) {
            is Result.Success -> result.data.items.map { it.swiped }.toSet()
            else -> emptySet()
        }
        
        // Get already matched user IDs
        val matchFilter = "user1 = '$userId' || user2 = '$userId'"
        val alreadyMatched = when (val result = pocketBase.getList<MatchRecord>(
            "m_matches", filter = matchFilter, perPage = 500
        )) {
            is Result.Success -> result.data.items.flatMap { 
                listOf(it.user1, it.user2) 
            }.filter { it != userId }.toSet()
            else -> emptySet()
        }
        
        val excluded = alreadySwiped + alreadyMatched + userId
        return allUsers.filter { it !in excluded }
    }
    
    /**
     * Persist a computed match score to the m_match_scores collection
     */
    suspend fun persistScore(score: CompositeMatchScore): Result<JsonObject> {
        val body = buildJsonObject {
            put("user1", score.user1Id)
            put("user2", score.user2Id)
            put("compositeScore", score.compositeScore)
            // Individual dimension scores
            score.scores.find { it.dimension == "proust_affinity" }?.let { put("proustScore", it.rawScore) }
            score.scores.find { it.dimension == "geolocation" }?.let { put("geoScore", it.rawScore) }
            score.scores.find { it.dimension == "interest_overlap" }?.let { put("interestScore", it.rawScore) }
            score.scores.find { it.dimension == "seeking_compatibility" }?.let { put("seekingScore", it.rawScore) }
        }
        return pocketBase.create("m_match_scores", body)
    }
}

@kotlinx.serialization.Serializable
private data class ProfileUserRecord(
    val id: String,
    val user: String,
    val created: String = "",
    val updated: String = ""
)

@kotlinx.serialization.Serializable
private data class SwipeRecord(
    val id: String,
    val swiper: String,
    val swiped: String,
    val direction: String,
    val created: String = "",
    val updated: String = ""
)

@kotlinx.serialization.Serializable
private data class MatchRecord(
    val id: String,
    val user1: String,
    val user2: String,
    val status: String = "",
    val created: String = "",
    val updated: String = ""
)
