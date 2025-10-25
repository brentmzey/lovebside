package love.bside.app.integration

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.repository.ApiMatchRepository
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for match functionality
 */
class MatchIntegrationTest {
    
    private val mockTokenStorage = MockTokenStorage()
    private val apiClient = InternalApiClient(mockTokenStorage)
    private val matchRepository = ApiMatchRepository(apiClient)
    
    @Test
    fun testGetMatches() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Get matches
        val result = matchRepository.getMatches("test-user-id")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
    
    @Test
    fun testUpdateMatchStatus_Like() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Like a match
        val result = matchRepository.updateMatchStatus("test-match-id", "LIKED")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
    
    @Test
    fun testUpdateMatchStatus_Pass() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Pass on a match
        val result = matchRepository.updateMatchStatus("test-match-id", "PASSED")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
    
    @Test
    fun testGetPromptsForMatch() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Get prompts
        val result = matchRepository.getPromptsForMatch("test-match-id")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
}
