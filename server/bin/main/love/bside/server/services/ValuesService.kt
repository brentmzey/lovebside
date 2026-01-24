package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.models.api.KeyValueDTO
import love.bside.server.models.api.UserValueDTO
import love.bside.server.models.api.SaveUserValuesRequest
import love.bside.server.repositories.ValuesRepository
import love.bside.server.utils.toDTO

/**
 * Service for values operations
 */
class ValuesService(
    private val valuesRepository: ValuesRepository
) {
    
    /**
     * Get all available key values
     */
    suspend fun getAllKeyValues(category: String? = null): List<KeyValueDTO> {
        val result = if (category != null) {
            valuesRepository.getKeyValuesByCategory(category)
        } else {
            valuesRepository.getAllKeyValues()
        }
        
        return when (result) {
            is Result.Success -> result.data.map { it.toDTO() }
            is Result.Error -> throw Exception("Failed to get key values: ${result.exception.message}")
            is Result.Loading -> emptyList() // Should not happen in direct repository calls
        }
    }
    
    /**
     * Get user's selected values
     */
    suspend fun getUserValues(userId: String): List<UserValueDTO> {
        return when (val result = valuesRepository.getUserValues(userId)) {
            is Result.Success -> result.data.map { it.toDTO() }
            is Result.Error -> throw Exception("Failed to get user values: ${result.exception.message}")
            is Result.Loading -> emptyList() // Should not happen in direct repository calls
        }
    }
    
    /**
     * Save user's values
     */
    suspend fun saveUserValues(userId: String, request: SaveUserValuesRequest): List<UserValueDTO> {
        val savedValues = mutableListOf<UserValueDTO>()
        
        for (valueInput in request.values) {
            val result = valuesRepository.saveUserValue(
                userId = userId,
                keyValueId = valueInput.keyValueId,
                importance = valueInput.importance
            )
            
            when (result) {
                is Result.Success -> savedValues.add(result.data.toDTO())
                is Result.Error -> throw Exception("Failed to save value: ${result.exception.message}")
                is Result.Loading -> {} // Should not happen in direct repository calls
            }
        }
        
        return savedValues
    }
}
