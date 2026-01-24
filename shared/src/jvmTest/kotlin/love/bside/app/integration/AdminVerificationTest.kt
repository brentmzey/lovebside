package love.bside.app.integration

import io.pocketbase.PocketBase
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertTrue

class AdminVerificationTest {

    @Test
    fun verifyAdminAccess() = kotlinx.coroutines.runBlocking {
        println("🔐 Verifying Admin Access via SDK...")
        
        try {
             // Use the local Docker URL
            val url = "http://localhost:8091/"
            println("Creating PocketBase client for $url...")
            val pb = PocketBase(url)
            println("Client created.")
        
            val email = "tester_admin@bside.love"
            val password = "password123"
            
            println("Attempting to auth as $email at $url...")
            
            // Try standard authWithPassword on '_superusers' collection for PB v0.23+
            try {
                // If the SDK doesn't expose 'admins' service, use collection based auth
                pb.collection("_superusers").authWithPassword(email, password)
                println("✅ Admin authentication successful (via _superusers collection)!")
            } catch (e: Exception) {
                println("⚠️ Superuser auth failed, checking error: ${e.message}")
                e.printStackTrace()
                throw e
            }
            
            assertTrue(pb.authStore.isValid, "Auth store should be valid after login")
            
            // Try to list users to prove admin capabilities
            // getList takes QueryOptions
            val users = pb.collection("users").getList(
                io.pocketbase.models.QueryOptions(page = 1, perPage = 1)
            )
            println("✅ Successfully fetched ${users.totalItems} users as Admin.")
            
        } catch (e: Throwable) {
            println("❌ Admin Verification Failed with Exception: $e")
            e.printStackTrace()
            throw e
        }
    }
}
