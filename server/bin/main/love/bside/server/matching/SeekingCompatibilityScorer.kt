package love.bside.server.matching

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result

/**
 * Seeking Compatibility Scorer
 * 
 * Checks alignment between what two users are looking for:
 * - "friendship" + "friendship" → 1.0
 * - "romantic" + "romantic" → 1.0
 * - "both" + anything → 0.8
 * - "friendship" + "romantic" → 0.0 (incompatible)
 */
class SeekingCompatibilityScorer(
    private val pocketBase: PocketBaseClient
) : ScoreDimension {
    
    override val name = "seeking_compatibility"
    override val defaultWeight = 0.20
    
    override suspend fun score(user1Id: String, user2Id: String): Double {
        val seeking1 = getUserSeeking(user1Id) ?: return 0.5 // Default if unknown
        val seeking2 = getUserSeeking(user2Id) ?: return 0.5
        
        return computeCompatibility(seeking1, seeking2)
    }
    
    private fun computeCompatibility(s1: String, s2: String): Double {
        val n1 = normalize(s1)
        val n2 = normalize(s2)
        
        return when {
            n1 == n2 -> 1.0                // Perfect alignment
            n1 == "both" || n2 == "both" -> 0.8   // One is flexible
            else -> 0.0                     // Incompatible (friendship vs romantic)
        }
    }
    
    private fun normalize(seeking: String): String = 
        seeking.lowercase().trim().let {
            when (it) {
                "relationship", "romantic" -> "romantic"
                "friendship" -> "friendship"
                "both" -> "both"
                else -> "both"
            }
        }
    
    private suspend fun getUserSeeking(userId: String): String? {
        val filter = "user = '$userId'"
        return when (val result = pocketBase.getList<ProfileSeekingRecord>(
            "s_profiles", filter = filter
        )) {
            is Result.Success -> result.data.items.firstOrNull()?.seeking
            else -> null
        }
    }
}

@kotlinx.serialization.Serializable
private data class ProfileSeekingRecord(
    val id: String,
    val user: String = "",
    val seeking: String = "both",
    val created: String = "",
    val updated: String = ""
)
