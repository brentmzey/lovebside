package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.api.UserValueInput
import love.bside.app.data.mappers.toDomain
import love.bside.app.domain.models.KeyValue
import love.bside.app.domain.models.UserValue
import love.bside.app.domain.repository.ValuesRepository

/**
 * API-based implementation of ValuesRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiValuesRepository(
    private val apiClient: InternalApiClient
) : ValuesRepository {
    
    override suspend fun getAllKeyValues(): Result<List<KeyValue>> {
        return apiClient.getAllKeyValues().map { keyValueDTOs ->
            keyValueDTOs.map { it.toDomain() }
        }
    }
    
    override suspend fun getKeyValuesByCategory(category: String): Result<List<KeyValue>> {
        return apiClient.getAllKeyValues(category = category).map { keyValueDTOs ->
            keyValueDTOs.map { it.toDomain() }
        }
    }
    
    override suspend fun addUserValue(userValue: UserValue): Result<Unit> {
        // Get current user values
        val currentValuesResult = apiClient.getUserValues()
        return when (currentValuesResult) {
            is Result.Success -> {
                // Add the new value to the list
                val allValues = currentValuesResult.data.map {
                    UserValueInput(
                        keyValueId = it.keyValue.id,
                        importance = 5 // Default importance
                    )
                } + UserValueInput(
                    keyValueId = userValue.valueId,
                    importance = 5
                )
                
                // Save the updated list
                apiClient.saveUserValues(allValues).map { Unit }
            }
            is Result.Error -> Result.Error(currentValuesResult.exception)
            is Result.Loading -> Result.Loading
        }
    }
    
    override suspend fun removeUserValue(userValue: UserValue): Result<Unit> {
        // Get current user values
        val currentValuesResult = apiClient.getUserValues()
        return when (currentValuesResult) {
            is Result.Success -> {
                // Remove the specified value
                val remainingValues = currentValuesResult.data
                    .filter { it.keyValue.id != userValue.valueId }
                    .map {
                        UserValueInput(
                            keyValueId = it.keyValue.id,
                            importance = 5 // Default importance
                        )
                    }
                
                // Save the updated list
                apiClient.saveUserValues(remainingValues).map { Unit }
            }
            is Result.Error -> Result.Error(currentValuesResult.exception)
            is Result.Loading -> Result.Loading
        }
    }
    
    override suspend fun getUserValues(userId: String): Result<List<UserValue>> {
        return apiClient.getUserValues().map { userValueDTOs ->
            userValueDTOs.map { it.toDomain(userId) }
        }
    }
}
