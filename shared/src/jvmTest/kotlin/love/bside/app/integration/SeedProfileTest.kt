package love.bside.app.integration

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.fail

class SeedProfileTest {

    @Test
    fun seedTesterProfile() = runBlocking {
        val url = "http://localhost:8091/"
        val pb = PocketBase(url)
        
        // 1. Admin Auth to Create Collection if needed
        println("🔐 Authenticating as Admin to check schema...")
        pb.collection("_superusers").authWithPassword("admin@bside.love", "password123456")
        
        try {
            // Check if collection exists via generic API call
            pb.send<Map<String, Any>>("/api/collections/s_profiles")
            println("✅ Collection 's_profiles' exists.")
        } catch (e: Exception) {
            println("⚠️ Collection 's_profiles' missing or error: ${e.message}. Creating...")
            
            try {
                // Create collection using JsonObject to avoid serialization issues with Map<String, Any>
                val collection = kotlinx.serialization.json.buildJsonObject {
                    put("name", kotlinx.serialization.json.JsonPrimitive("s_profiles"))
                    put("type", kotlinx.serialization.json.JsonPrimitive("base"))
                    put("schema", kotlinx.serialization.json.buildJsonArray {
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("name", kotlinx.serialization.json.JsonPrimitive("userId"))
                            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                            put("required", kotlinx.serialization.json.JsonPrimitive(true))
                        })
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("name", kotlinx.serialization.json.JsonPrimitive("firstName"))
                            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                        })
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("name", kotlinx.serialization.json.JsonPrimitive("lastName"))
                            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                        })
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("name", kotlinx.serialization.json.JsonPrimitive("birthDate"))
                            put("type", kotlinx.serialization.json.JsonPrimitive("text"))
                        })
                        add(kotlinx.serialization.json.buildJsonObject {
                            put("name", kotlinx.serialization.json.JsonPrimitive("seeking"))
                            put("type", kotlinx.serialization.json.JsonPrimitive("select"))
                            put("options", kotlinx.serialization.json.buildJsonObject {
                                put("values", kotlinx.serialization.json.buildJsonArray {
                                    add(kotlinx.serialization.json.JsonPrimitive("Friendship"))
                                    add(kotlinx.serialization.json.JsonPrimitive("Relationship"))
                                    add(kotlinx.serialization.json.JsonPrimitive("Both"))
                                })
                            })
                        })
                    })
                    put("listRule", kotlinx.serialization.json.JsonPrimitive(""))
                    put("viewRule", kotlinx.serialization.json.JsonPrimitive(""))
                    put("createRule", kotlinx.serialization.json.JsonPrimitive("@request.auth.id != ''"))
                    put("updateRule", kotlinx.serialization.json.JsonPrimitive("@request.auth.id != ''"))
                    put("deleteRule", kotlinx.serialization.json.JsonPrimitive("@request.auth.id != ''"))
                }

                // Serialize to String
                val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
                val bodyStr = json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), collection)

                // Use generic send with String body
                pb.send<kotlinx.serialization.json.JsonObject>(
                    path = "/api/collections", 
                    method = "POST", 
                    body = bodyStr
                )
                println("✅ Collection 's_profiles' created.")
            } catch (ex: Exception) {
                println("❌ Failed to create collection: $ex")
                if (ex is io.pocketbase.models.ClientResponseException) {
                    println("Body: ${ex.response}")
                }
                // Don't throw here, maybe it failed because it exists?
            }
        }

        // 2. User Auth to Create Profile
        val email = "tester@bside.love"
        val password = "password123"

        println("🔐 Authenticating as $email...")
        try {
            pb.collection("users").authWithPassword(email, password)
        } catch (e: Exception) {
            println("❌ Auth failed for $email. Ensure user exists via curl/script.")
            throw e
        }

        val userId = pb.authStore.model?.get("id").toString().replace("\"", "")
        println("✅ User ID: $userId")

        // Check if profile exists
        try {
            val profiles = pb.collection("s_profiles").getList(
                QueryOptions(filter = "userId='$userId'")
            )
            
            if (profiles.items.isNotEmpty()) {
                println("⚠️ Profile already exists for $email. Skipping.")
                return@runBlocking
            }
        } catch (e: Exception) {
             // Ignore
        }

        println("📝 Creating Profile for $email...")
        try {
            val profile = mapOf(
                "userId" to userId,
                "firstName" to "Tester",
                "lastName" to "B-Side",
                "birthDate" to "1990-01-01",
                "seeking" to "Both"
            )
            
            pb.collection("s_profiles").create(profile)
            println("✅ Profile created successfully!")
        } catch (e: Exception) {
            println("❌ Failed to create profile: $e")
            throw e
        }
    }
}
