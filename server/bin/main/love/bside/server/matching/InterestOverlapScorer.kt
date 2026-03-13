package love.bside.server.matching

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result

/**
 * Interest Overlap Scorer
 * 
 * Computes similarity between users based on shared interests/favorites,
 * weighted by mutual importance ratings.
 * 
 * Algorithm:
 * 1. Fetch each user's interests with category + value + importance
 * 2. Find shared (category, value) pairs
 * 3. Score = weighted Jaccard: sum of min(importance) for shared / sum of max(importance) for union
 */
class InterestOverlapScorer(
    private val pocketBase: PocketBaseClient
) : ScoreDimension {
    
    override val name = "interest_overlap"
    override val defaultWeight = 0.25
    
    override suspend fun score(user1Id: String, user2Id: String): Double {
        val interests1 = getUserInterests(user1Id)
        val interests2 = getUserInterests(user2Id)
        
        if (interests1.isEmpty() || interests2.isEmpty()) return 0.0
        
        // Create keys for comparison: "category:value"
        val map1 = interests1.associate { "${it.category}:${it.value}" to it.importance }
        val map2 = interests2.associate { "${it.category}:${it.value}" to it.importance }
        
        val allKeys = map1.keys.union(map2.keys)
        if (allKeys.isEmpty()) return 0.0
        
        // Weighted Jaccard: sum(min) / sum(max)
        var sumMin = 0.0
        var sumMax = 0.0
        for (key in allKeys) {
            val v1 = map1[key] ?: 0
            val v2 = map2[key] ?: 0
            sumMin += minOf(v1, v2).toDouble()
            sumMax += maxOf(v1, v2).toDouble()
        }
        
        return if (sumMax > 0) sumMin / sumMax else 0.0
    }
    
    private suspend fun getUserInterests(userId: String): List<UserInterest> {
        val filter = "user = '$userId'"
        return when (val result = pocketBase.getList<InterestRecord>(
            "s_user_interests", filter = filter, perPage = 100
        )) {
            is Result.Success -> result.data.items.map { 
                UserInterest(it.category, it.value, it.importance)
            }
            else -> emptyList()
        }
    }
    
    private data class UserInterest(val category: String, val value: String, val importance: Int)
}

@kotlinx.serialization.Serializable
private data class InterestRecord(
    val id: String,
    val user: String = "",
    val category: String,
    val value: String,
    val importance: Int = 5,
    val created: String = "",
    val updated: String = ""
)
