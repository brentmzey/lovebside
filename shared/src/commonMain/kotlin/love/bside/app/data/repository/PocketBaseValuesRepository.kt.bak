package love.bside.app.data.repository

import kotlinx.serialization.Serializable
import love.bside.app.core.Result
import love.bside.app.core.cache.CacheKeys
import love.bside.app.core.cache.InMemoryCache
import love.bside.app.core.logDebug
import love.bside.app.core.logInfo
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.data.models.toDomain
import love.bside.app.domain.models.KeyValue
import love.bside.app.domain.models.UserValue
import love.bside.app.domain.repository.ValuesRepository

@Serializable
data class UserValueCreateRequest(val userId: String, val keyValueId: String)

class PocketBaseValuesRepository(
    private val pocketBase: PocketBaseClient,
    private val cache: InMemoryCache<String, List<KeyValue>> = InMemoryCache()
) : ValuesRepository {
    
    override suspend fun getAllKeyValues(): Result<List<KeyValue>> {
        logDebug("Fetching all key values")
        
        // Check cache first
        cache.get("all_key_values")?.let {
            logDebug("Key values found in cache")
            return Result.Success(it)
        }

        return pocketBase.getList<love.bside.app.data.models.KeyValue>(
            collection = "s_key_values",
            perPage = 100
        ).map { listResult ->
            val domainValues = listResult.items.map { it.toDomain() }
            cache.put("all_key_values", domainValues, ttlMs = 3600_000) // Cache for 1 hour
            logInfo("Fetched ${domainValues.size} key values")
            domainValues
        }
    }

    override suspend fun getKeyValuesByCategory(category: String): Result<List<KeyValue>> {
        logDebug("Fetching key values for category: $category")
        
        return pocketBase.getList<love.bside.app.data.models.KeyValue>(
            collection = "s_key_values",
            filter = "(category='$category')",
            perPage = 100
        ).map { listResult ->
            val domainValues = listResult.items.map { it.toDomain() }
            logInfo("Fetched ${domainValues.size} key values for category $category")
            domainValues
        }
    }

    override suspend fun addUserValue(userValue: UserValue): Result<Unit> {
        logInfo("Adding user value: userId=${userValue.userId}, valueId=${userValue.valueId}")
        
        return pocketBase.create<UserValueCreateRequest, love.bside.app.data.models.UserValue>(
            collection = "s_user_values",
            body = UserValueCreateRequest(userValue.userId, userValue.valueId)
        ).map {
            cache.remove(CacheKeys.values(userValue.userId))
            logInfo("User value added successfully")
        }
    }

    override suspend fun removeUserValue(userValue: UserValue): Result<Unit> {
        logInfo("Removing user value: userId=${userValue.userId}, valueId=${userValue.valueId}")
        
        // First find the record
        return pocketBase.getFirstListItem<love.bside.app.data.models.UserValue>(
            collection = "s_user_values",
            filter = "(userId='${userValue.userId}' && keyValueId='${userValue.valueId}')"
        ).flatMap { dto ->
            pocketBase.delete(
                collection = "s_user_values",
                id = dto.id
            ).map {
                cache.remove(CacheKeys.values(userValue.userId))
                logInfo("User value removed successfully")
            }
        }
    }

    override suspend fun getUserValues(userId: String): Result<List<UserValue>> {
        logDebug("Fetching user values for userId: $userId")
        
        // Check cache first
        cache.get(CacheKeys.values(userId))?.let { cachedValues ->
            // Convert cached KeyValues back to UserValues if needed
            // For now, we'll skip cache for user values since they're different type
        }

        return pocketBase.getList<love.bside.app.data.models.UserValue>(
            collection = "s_user_values",
            filter = "(userId='$userId')",
            perPage = 100
        ).map { listResult ->
            val domainValues = listResult.items.map { it.toDomain() }
            logInfo("Fetched ${domainValues.size} user values")
            domainValues
        }
    }
}
