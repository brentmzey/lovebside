package love.bside.server.matching

/**
 * A single dimension of match compatibility scoring.
 * Each scorer produces a normalized 0.0–1.0 score.
 */
interface ScoreDimension {
    /** Human-readable name of this scoring dimension */
    val name: String
    
    /** Default weight for composite scoring (0.0–1.0) */
    val defaultWeight: Double
    
    /** Compute compatibility score between two users (0.0 = incompatible, 1.0 = perfect) */
    suspend fun score(user1Id: String, user2Id: String): Double
}

/**
 * A weighted score result from a single dimension
 */
data class WeightedScore(
    val dimension: String,
    val rawScore: Double,
    val weight: Double
) {
    val weightedScore: Double get() = rawScore * weight
}

/**
 * Complete match score result combining all dimensions
 */
data class CompositeMatchScore(
    val user1Id: String,
    val user2Id: String,
    val scores: List<WeightedScore>,
    val compositeScore: Double
) {
    companion object {
        fun compute(user1Id: String, user2Id: String, scores: List<WeightedScore>): CompositeMatchScore {
            val totalWeight = scores.sumOf { it.weight }
            val composite = if (totalWeight > 0) {
                scores.sumOf { it.weightedScore } / totalWeight
            } else 0.0
            return CompositeMatchScore(user1Id, user2Id, scores, composite)
        }
    }
}
