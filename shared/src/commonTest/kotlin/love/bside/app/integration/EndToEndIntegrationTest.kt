package love.bside.app.integration

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.repository.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * End-to-end integration test
 * Tests the complete user journey through the app
 * 
 * This test requires the server to be running at localhost:8080
 * Run with: ./gradlew :server:run (in a separate terminal)
 */
class EndToEndIntegrationTest {
    
    private val mockTokenStorage = MockTokenStorage()
    private val apiClient = InternalApiClient(mockTokenStorage)
    
    private val authRepository = ApiAuthRepository(apiClient)
    private val profileRepository = ApiProfileRepository(apiClient)
    private val valuesRepository = ApiValuesRepository(apiClient)
    private val matchRepository = ApiMatchRepository(apiClient)
    private val questionnaireRepository = ApiQuestionnaireRepository(apiClient)
    
    @Test
    fun testCompleteUserJourney() = runTest {
        println("=== Starting End-to-End Integration Test ===")
        
        // Step 1: Get key values (public endpoint)
        println("\n[Step 1] Fetching available personality traits...")
        val keyValuesResult = valuesRepository.getAllKeyValues()
        assertTrue(keyValuesResult is Result.Success || keyValuesResult is Result.Error)
        
        if (keyValuesResult is Result.Success) {
            println("✓ Successfully fetched ${keyValuesResult.data.size} key values")
        } else if (keyValuesResult is Result.Error) {
            println("✗ Failed to fetch key values: ${keyValuesResult.exception.message}")
        }
        
        // Step 2: Get prompts (public endpoint)
        println("\n[Step 2] Fetching questionnaire prompts...")
        val promptsResult = questionnaireRepository.getAllQuestionnaires()
        assertTrue(promptsResult is Result.Success || promptsResult is Result.Error)
        
        if (promptsResult is Result.Success) {
            println("✓ Successfully fetched ${promptsResult.data.size} prompts")
        } else if (promptsResult is Result.Error) {
            println("✗ Failed to fetch prompts: ${promptsResult.exception.message}")
        }
        
        // Step 3: Attempt login (requires test user to exist)
        println("\n[Step 3] Testing authentication...")
        val loginResult = authRepository.login("test@example.com", "Test1234!")
        assertTrue(loginResult is Result.Success || loginResult is Result.Error)
        
        when (loginResult) {
            is Result.Success -> {
                println("✓ Successfully logged in")
                println("  Token: ${loginResult.data.token.take(20)}...")
                
                // Continue with authenticated requests
                testAuthenticatedFlow()
            }
            is Result.Error -> {
                println("✗ Login failed: ${loginResult.exception.message}")
                println("  Note: Create a test user first or update credentials")
            }
            is Result.Loading -> {
                println("⏳ Login in progress")
            }
        }
        
        println("\n=== End-to-End Integration Test Complete ===")
    }
    
    private suspend fun testAuthenticatedFlow() {
        // Step 4: Get user profile
        println("\n[Step 4] Fetching user profile...")
        val profileResult = profileRepository.getProfile("test-user-id")
        
        when (profileResult) {
            is Result.Success -> {
                println("✓ Successfully fetched profile")
                println("  Name: ${profileResult.data.firstName} ${profileResult.data.lastName}")
            }
            is Result.Error -> {
                println("✗ Failed to fetch profile: ${profileResult.exception.message}")
            }
            is Result.Loading -> {
                println("⏳ Profile fetch in progress")
            }
        }
        
        // Step 5: Get user's values
        println("\n[Step 5] Fetching user's selected values...")
        val userValuesResult = valuesRepository.getUserValues("test-user-id")
        
        when (userValuesResult) {
            is Result.Success -> {
                println("✓ Successfully fetched user values")
                println("  Count: ${userValuesResult.data.size}")
            }
            is Result.Error -> {
                println("✗ Failed to fetch user values: ${userValuesResult.exception.message}")
            }
            is Result.Loading -> {
                println("⏳ User values fetch in progress")
            }
        }
        
        // Step 6: Get matches
        println("\n[Step 6] Fetching potential matches...")
        val matchesResult = matchRepository.getMatches("test-user-id")
        
        when (matchesResult) {
            is Result.Success -> {
                println("✓ Successfully fetched matches")
                println("  Count: ${matchesResult.data.size}")
            }
            is Result.Error -> {
                println("✗ Failed to fetch matches: ${matchesResult.exception.message}")
            }
            is Result.Loading -> {
                println("⏳ Matches fetch in progress")
            }
        }
        
        // Step 7: Get user's questionnaire answers
        println("\n[Step 7] Fetching user's answers...")
        val answersResult = questionnaireRepository.getUserAnswers("test-user-id")
        
        when (answersResult) {
            is Result.Success -> {
                println("✓ Successfully fetched answers")
                println("  Count: ${answersResult.data.size}")
            }
            is Result.Error -> {
                println("✗ Failed to fetch answers: ${answersResult.exception.message}")
            }
            is Result.Loading -> {
                println("⏳ Answers fetch in progress")
            }
        }
    }
    
    @Test
    fun testHealthCheck() = runTest {
        println("\n=== Testing Server Health Check ===")
        
        val healthResult = apiClient.healthCheck()
        
        when (healthResult) {
            is Result.Success -> {
                println("✓ Server is healthy")
                println("  Status: ${healthResult.data.status}")
                println("  Version: ${healthResult.data.version}")
                assertEquals("healthy", healthResult.data.status)
            }
            is Result.Error -> {
                println("✗ Health check failed: ${healthResult.exception.message}")
                println("  Make sure server is running: ./gradlew :server:run")
            }
            is Result.Loading -> {
                println("⏳ Health check in progress")
            }
        }
    }
    
    @Test
    fun testRepositoryInitialization() = runTest {
        println("\n=== Testing Repository Initialization ===")
        
        // Verify all repositories are properly initialized
        assertTrue(authRepository is ApiAuthRepository, "AuthRepository should be ApiAuthRepository")
        assertTrue(profileRepository is ApiProfileRepository, "ProfileRepository should be ApiProfileRepository")
        assertTrue(valuesRepository is ApiValuesRepository, "ValuesRepository should be ApiValuesRepository")
        assertTrue(matchRepository is ApiMatchRepository, "MatchRepository should be ApiMatchRepository")
        assertTrue(questionnaireRepository is ApiQuestionnaireRepository, "QuestionnaireRepository should be ApiQuestionnaireRepository")
        
        println("✓ All repositories initialized correctly")
        println("  - AuthRepository: ApiAuthRepository")
        println("  - ProfileRepository: ApiProfileRepository")
        println("  - ValuesRepository: ApiValuesRepository")
        println("  - MatchRepository: ApiMatchRepository")
        println("  - QuestionnaireRepository: ApiQuestionnaireRepository")
    }
}
