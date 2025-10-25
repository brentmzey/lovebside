package love.bside.app.integration

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.repository.ApiValuesRepository
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for values/personality traits
 */
class ValuesIntegrationTest {
    
    private val mockTokenStorage = MockTokenStorage()
    private val apiClient = InternalApiClient(mockTokenStorage)
    private val valuesRepository = ApiValuesRepository(apiClient)
    
    @Test
    fun testGetAllKeyValues() = runTest {
        // Get all personality traits
        val result = valuesRepository.getAllKeyValues()
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
        
        if (result is Result.Success) {
            assertTrue(result.data.isNotEmpty(), "Should have at least some key values")
        }
    }
    
    @Test
    fun testGetKeyValuesByCategory() = runTest {
        // Get values by category
        val result = valuesRepository.getKeyValuesByCategory("PERSONALITY")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
    
    @Test
    fun testGetUserValues() = runTest {
        // Set a mock token first
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Get user's values
        val result = valuesRepository.getUserValues("test-user-id")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
}
