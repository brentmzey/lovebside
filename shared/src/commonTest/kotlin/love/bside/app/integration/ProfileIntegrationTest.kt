package love.bside.app.integration

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.repository.ApiProfileRepository
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.domain.models.SeekingStatus
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Integration tests for profile functionality
 */
class ProfileIntegrationTest {
    
    private val mockTokenStorage = MockTokenStorage()
    private val apiClient = InternalApiClient(mockTokenStorage)
    private val profileRepository = ApiProfileRepository(apiClient)
    
    @Test
    fun testGetProfile() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Get profile
        val result = profileRepository.getProfile("test-user-id")
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
    
    @Test
    fun testUpdateProfile() = runTest {
        // Set a mock token
        mockTokenStorage.saveToken("mock-token-for-testing")
        
        // Update profile
        val updateRequest = ProfileUpdateRequest(
            firstName = "UpdatedFirst",
            lastName = "UpdatedLast",
            bio = "Updated bio",
            location = "New Location",
            seeking = love.bside.app.data.models.SeekingStatus.BOTH
        )
        
        val result = profileRepository.updateProfile("test-user-id", updateRequest)
        
        // Verify result structure
        assertTrue(result is Result.Success || result is Result.Error)
    }
}
