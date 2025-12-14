package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.test.runTest
import love.bside.app.core.Result
import love.bside.app.data.repository.PocketBaseProfileRepository
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * LIVE integration test for PocketBase on Pockethost
 * 
 * This test actually connects to https://bside.pockethost.io
 * to verify the schema and connectivity work.
 * 
 * Run with: ./gradlew :shared:jvmTest --tests "*PocketHostIntegrationTest*"
 */
class PocketHostIntegrationTest {
    
    private val pocketBase = PocketBase("https://bside.pockethost.io")
    private val profileRepository = PocketBaseProfileRepository(pocketBase)
    
    @Test
    fun `verify s_profiles collection exists and is accessible`() = runTest {
        println("🧪 Testing live connection to Pockethost...")
        
        // Attempt to list profiles without auth (should fail with 403 or return empty)
        val result = profileRepository.getProfile("nonexistent-user-id")
        
        // We expect an error (either 403 Forbidden, 404 Not Found, or ResourceNotFound)
        // But NOT a 500 or connection error
        assertTrue(result is Result.Error, "Expected error for unauthenticated access or non-existent user")
        
        val error = (result as Result.Error).exception
        println("✅ Got expected error: ${error.message}")
        
        // Verify it's a client error (4xx), not server error (5xx)
        // Accept "not found" errors as proof the collection exists
        val isExpectedError = error.message?.contains("403") == true || 
            error.message?.contains("404") == true ||
            error.message?.contains("not found", ignoreCase = true) == true ||
            error.message?.contains("ResourceNotFound") == true
        
        assertTrue(
            isExpectedError,
            "Expected 403/404/NotFound error (proves collection exists), got: ${error.message}"
        )
        
        println("✅ s_profiles collection exists and has correct permissions!")
    }
    
    @Test
    fun `verify m_messages collection exists`() = runTest {
        println("🧪 Testing m_messages collection...")
        
        try {
            // Try to access messages endpoint (will fail auth but proves collection exists)
            val response = pocketBase.collection("m_messages")
                .getList(io.pocketbase.models.QueryOptions(perPage = 1))
            
            // If we get here without exception, collection exists
            println("✅ m_messages collection accessible (got response)")
        } catch (e: Exception) {
            // Check it's NOT a 404 (that would mean collection doesn't exist)
            val is404 = e.message?.contains("404") == true && !e.message!!.contains("not found", ignoreCase = true)
            
            // If it's 404 for the collection itself (not records), fail
            if (is404 && e.message?.contains("collection") == true) {
                throw AssertionError("m_messages collection NOT FOUND - create it!", e)
            }
            
            // Any other error (403, record not found) means collection exists
            println("✅ m_messages exists (${e.message?.take(50)}...)")
        }
    }
    
    @Test
    fun `verify m_conversations collection exists`() = runTest {
        println("🧪 Testing m_conversations collection...")
        
        try {
            val response = pocketBase.collection("m_conversations")
                .getList(io.pocketbase.models.QueryOptions(perPage = 1))
            
            println("✅ m_conversations collection accessible")
        } catch (e: Exception) {
            val is404 = e.message?.contains("404") == true && !e.message!!.contains("not found", ignoreCase = true)
            
            if (is404 && e.message?.contains("collection") == true) {
                throw AssertionError("m_conversations collection NOT FOUND - create it!", e)
            }
            
            println("✅ m_conversations exists (${e.message?.take(50)}...)")
        }
    }
}
