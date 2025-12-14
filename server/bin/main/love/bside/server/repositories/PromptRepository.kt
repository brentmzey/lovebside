package love.bside.server.repositories

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.domain.Prompt
import love.bside.server.models.domain.UserAnswer
import love.bside.server.models.db.PBPrompt
import love.bside.server.models.db.PBUserAnswer
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject

/**
 * Repository interface for prompt operations
 */
interface PromptRepository {
    suspend fun getAllPrompts(): Result<List<Prompt>>
    suspend fun getPromptById(id: String): Result<Prompt>
    suspend fun getUserAnswers(userId: String): Result<List<UserAnswer>>
    suspend fun saveUserAnswer(userId: String, promptId: String, answer: String): Result<UserAnswer>
}

/**
 * Implementation using PocketBase
 */
class PromptRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : PromptRepository {
    
    private val promptsCollection = "s_prompts"
    private val answersCollection = "s_user_answers"
    
    override suspend fun getAllPrompts(): Result<List<Prompt>> {
        return when (val result = pocketBase.getList<PBPrompt>(promptsCollection, perPage = 500)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getPromptById(id: String): Result<Prompt> {
        return when (val result = pocketBase.getOne<PBPrompt>(promptsCollection, id)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getUserAnswers(userId: String): Result<List<UserAnswer>> {
        val filter = "userId = '$userId'"
        val expand = "promptId"
        
        return when (val result = pocketBase.getList<PBUserAnswer>(answersCollection, filter = filter, expand = expand, perPage = 500)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun saveUserAnswer(userId: String, promptId: String, answer: String): Result<UserAnswer> {
        val body = buildJsonObject {
            put("userId", userId)
            put("promptId", promptId)
            put("answer", answer)
        }
        
        return when (val result = pocketBase.create<JsonObject, PBUserAnswer>(answersCollection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
}
