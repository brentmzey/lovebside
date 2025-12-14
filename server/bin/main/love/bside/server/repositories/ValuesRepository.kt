package love.bside.server.repositories

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.domain.KeyValue
import love.bside.server.models.domain.UserValue
import love.bside.server.models.db.PBKeyValue
import love.bside.server.models.db.PBUserValue
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject

/**
 * Repository interface for values operations
 */
interface ValuesRepository {
    suspend fun getAllKeyValues(): Result<List<KeyValue>>
    suspend fun getKeyValuesByCategory(category: String): Result<List<KeyValue>>
    suspend fun getUserValues(userId: String): Result<List<UserValue>>
    suspend fun saveUserValue(userId: String, keyValueId: String, importance: Int): Result<UserValue>
    suspend fun deleteUserValue(id: String): Result<Unit>
}

/**
 * Implementation using PocketBase
 */
class ValuesRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : ValuesRepository {
    
    private val keyValuesCollection = "s_key_values"
    private val userValuesCollection = "s_user_values"
    
    override suspend fun getAllKeyValues(): Result<List<KeyValue>> {
        return when (val result = pocketBase.getList<PBKeyValue>(keyValuesCollection, perPage = 500)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getKeyValuesByCategory(category: String): Result<List<KeyValue>> {
        val filter = "category = '$category'"
        return when (val result = pocketBase.getList<PBKeyValue>(keyValuesCollection, filter = filter, perPage = 500)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getUserValues(userId: String): Result<List<UserValue>> {
        val filter = "userId = '$userId'"
        val expand = "keyValueId"
        
        return when (val result = pocketBase.getList<PBUserValue>(userValuesCollection, filter = filter, expand = expand, perPage = 500)) {
            is Result.Success -> Result.Success(result.data.items.map { it.toDomain() })
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun saveUserValue(userId: String, keyValueId: String, importance: Int): Result<UserValue> {
        val body = buildJsonObject {
            put("userId", userId)
            put("keyValueId", keyValueId)
            put("importance", importance)
        }
        
        return when (val result = pocketBase.create<JsonObject, PBUserValue>(userValuesCollection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun deleteUserValue(id: String): Result<Unit> {
        return pocketBase.delete(userValuesCollection, id)
    }
}
