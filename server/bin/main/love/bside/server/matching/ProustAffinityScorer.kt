package love.bside.server.matching

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result

/**
 * Proust Questionnaire Affinity Scorer
 * 
 * Compares free-text responses to Proust parlor questions between two users.
 * Uses tokenized Jaccard similarity on answers to shared questions.
 */
class ProustAffinityScorer(
    private val pocketBase: PocketBaseClient
) : ScoreDimension {
    
    override val name = "proust_affinity"
    override val defaultWeight = 0.30
    
    override suspend fun score(user1Id: String, user2Id: String): Double {
        val responses1 = getUserResponses(user1Id)
        val responses2 = getUserResponses(user2Id)
        
        if (responses1.isEmpty() || responses2.isEmpty()) return 0.0
        
        // Find questions both users have answered
        val sharedQuestions = responses1.keys.intersect(responses2.keys)
        if (sharedQuestions.isEmpty()) return 0.0
        
        // Compute Jaccard similarity on tokenized answers for each shared question
        val similarities = sharedQuestions.map { questionId ->
            val tokens1 = tokenize(responses1[questionId] ?: "")
            val tokens2 = tokenize(responses2[questionId] ?: "")
            jaccardSimilarity(tokens1, tokens2)
        }
        
        return similarities.average()
    }
    
    private suspend fun getUserResponses(userId: String): Map<String, String> {
        val filter = "user = '$userId'"
        return when (val result = pocketBase.getList<ResponseRecord>(
            "t_user_questionnaire_responses", filter = filter, perPage = 100
        )) {
            is Result.Success -> result.data.items.associate { it.question to it.response }
            else -> emptyMap()
        }
    }
    
    private fun tokenize(text: String): Set<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 } // Skip very short words
            .toSet()
    }
    
    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        val intersection = a.intersect(b).size.toDouble()
        val union = a.union(b).size.toDouble()
        return if (union > 0) intersection / union else 0.0
    }
}

@kotlinx.serialization.Serializable
private data class ResponseRecord(
    val id: String,
    val question: String,
    val response: String,
    val user: String = "",
    val created: String = "",
    val updated: String = ""
)
