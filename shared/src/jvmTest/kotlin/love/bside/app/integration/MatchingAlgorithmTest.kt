package love.bside.app.integration

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Verifies the Backend Matching Algorithm (JS Hook) by:
 * 1. Creating two compatible profiles.
 * 2. Creating Proust questionnaire answers.
 * 3. Triggering the matching job via the test endpoint.
 * 4. Asserting the Match record creation and score.
 */
class MatchingAlgorithmTest {

    private val pocketBaseURL = System.getenv("TEST_PB_URL") ?: "http://localhost:8091"
    private val adminEmail = "test@example.com" 
    private val adminPassword = "test12345"

    @Test
    fun testMatchingAlgorithmEndToEnd() = runTest {
        try {
            println("=== 🧬 TEST: Matching Algorithm (Jaccard + Proust) START ===")
            
            // 1. Setup Client
            val pb = PocketBase(pocketBaseURL)
            
            // Ensure Admin exists (via debug hook fallback)
            try {
                pb.collection("users").authWithPassword(adminEmail, adminPassword)
            } catch (e: Exception) {
                try {
                    pb.send<String>(
                        path = "/api/debug/create-user",
                        method = "POST",
                        body = null,
                        headers = emptyMap(),
                        query = mapOf("email" to adminEmail, "password" to adminPassword)
                    )
                } catch (ex: Exception) {}
            }
            
            val userA = "match_user_a_${Clock.System.now().epochSeconds}"
            val userB = "match_user_b_${Clock.System.now().epochSeconds}"
            val emailA = "$userA@example.com"
            val emailB = "$userB@example.com"
            
            var userAId: String? = null
            var userBId: String? = null

            try {
                println("Creating User A ($emailA) via Debug Hook...")
                val respA = pb.send<String>(
                    path = "/api/debug/create-user", 
                    method = "POST",
                    body = null,
                    headers = emptyMap(),
                    query = mapOf("email" to emailA, "password" to "test12345")
                )
                userAId = Json.parseToJsonElement(respA).jsonObject["id"]?.jsonPrimitive?.content
                println("User A ID: $userAId")

                println("Creating User B ($emailB) via Debug Hook...")
                val respB = pb.send<String>(
                    path = "/api/debug/create-user", 
                    method = "POST", 
                    body = null,
                    headers = emptyMap(),
                    query = mapOf("email" to emailB, "password" to "test12345")
                )
                userBId = Json.parseToJsonElement(respB).jsonObject["id"]?.jsonPrimitive?.content
                println("User B ID: $userBId")

                if (userAId == null || userBId == null) fail("Failed to obtain User IDs")

                // 3. User A Setup (Profile & Answers)
                println("Setting up User A...")
                pb.collection("users").authWithPassword(emailA, "test12345") // Switch Auth to A
                
                pb.collection("s_profiles").create(mapOf(
                    "userId" to userAId!!,
                    "firstName" to "Alice",
                    "lastName" to "Algorithm",
                    "birthDate" to "1990-01-01",
                    "seeking" to "relationship",
                    "location" to "New York",
                    "interests" to listOf("music", "travel")
                ))

                val questions = pb.collection("p_questionnaires").getList(QueryOptions(perPage = 1)).items
                val qId = if (questions.isNotEmpty()) questions[0]["id"]?.jsonPrimitive?.content ?: "q_dummy_id" else "q_dummy_id"
                
                pb.collection("t_user_questionnaire_responses").create(mapOf(
                    "user_id" to userAId!!,
                    "question_id" to qId,
                    "answer_text" to "Sleeping in a hammock"
                ))

                // 4. User B Setup (Profile & Answers)
                println("Setting up User B...")
                pb.collection("users").authWithPassword(emailB, "test12345") // Switch Auth to B
                
                pb.collection("s_profiles").create(mapOf(
                    "userId" to userBId!!,
                    "firstName" to "Bob",
                    "lastName" to "Builder",
                    "birthDate" to "1990-01-01",
                    "seeking" to "relationship",
                    "location" to "New York",
                    "interests" to listOf("music", "reading")
                ))
                
                pb.collection("t_user_questionnaire_responses").create(mapOf(
                    "user_id" to userBId!!,
                    "question_id" to qId,
                    "answer_text" to "Sleeping in a hammock on the beach"
                ))

                // 5. Trigger Matching Algorithm (Can be anonymous or admin?)
                // The hook didn't enforce admin check in my last edit, so public trigger is fine.
                // Or I can auth as admin just in case.
                println("🚀 Triggering Matching Algorithm via Hook...")
                val response = pb.send<String>(
                    path = "/api/test/trigger-matching",
                    method = "POST",
                    body = "{}", // Send empty JSON object just in case
                    headers = mapOf()
                )
                println("Hook Response: $response")

                // 6. Verify Match Created (Need to auth as one of the users to see it? Or Admin?)
                // m_matches listRule: "userId = @request.auth.id || matchedUserId = @request.auth.id"
                // So I must receive matches for the currently authenticated user (User B currently).
                // User B should see a match with User A.
                
                println("🔍 Verifying Match Record (As User B)...")
                val matches = pb.collection("m_matches").getList(QueryOptions(
                    filter = "userId='$userBId' || matchedUserId='$userBId'"
                ))
                
                if (matches.items.isEmpty()) {
                    println("❌ No matches found for User B.")
                }
                
                // Note: The hook creates the match record.
                // It sets userId = uaId, matchedUserId = ubId (or vice versa depending on loop order).
                // Since our query allows seeing both directions, we just need to find the record.
                
                val match = matches.items.find { 
                    (it["userId"]?.jsonPrimitive?.content == userAId && it["matchedUserId"]?.jsonPrimitive?.content == userBId) ||
                    (it["userId"]?.jsonPrimitive?.content == userBId && it["matchedUserId"]?.jsonPrimitive?.content == userAId)
                }
                
                if (match != null) {
                    val score = match["matchScore"]?.jsonPrimitive?.int ?: 0
                    println("✅ Match Found! Score: $score")
                    assertTrue(score > 30, "Score should be significant (>30)")
                } else {
                    fail("Match record not found in ${matches.items.size} records.")
                }

            } catch (e: Exception) {
                e.printStackTrace()
                fail("Test failed: ${e.message}")
            } finally {
                println("🧹 Cleanup...")
                // Cleanup Users (Cascade deletes profiles/matches)
                // Need to Auth as Admin to delete users? Or users deleting themselves?
                // Users usually can delete themselves.
                try {
                    pb.collection("users").authWithPassword(emailA, "test12345")
                    pb.collection("users").delete(userAId!!) 
                } catch (e: Exception) {}
                
                try { 
                    pb.collection("users").authWithPassword(emailB, "test12345")
                    pb.collection("users").delete(userBId!!) 
                } catch (e: Exception) {}
            }
        } catch (t: Throwable) {
            println("❌❌❌ CRITICAL FAILURE: ${t.message}")
            t.printStackTrace()
            fail("Critical failure: ${t.message}")
        }
    }
}
