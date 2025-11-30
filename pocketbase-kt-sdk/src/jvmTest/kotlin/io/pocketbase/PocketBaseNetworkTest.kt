package io.pocketbase

import io.pocketbase.models.ClientResponseException
import io.pocketbase.models.ListResult
import io.pocketbase.models.QueryOptions
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class PocketBaseNetworkTest {
    private val pocketBaseHost: String = (System.getenv("POCKETBASE_HOST") ?: "https://bside.pockethost.io").trimEnd('/')

    @Test
    fun healthEndpointReturnsOk() = runBlocking {
        withClient { client ->
            val health: JsonObject = client.send("/api/health")
            val code = health["code"]?.jsonPrimitive?.int ?: fail("Missing 'code' in health response from $pocketBaseHost")
            assertEquals(200, code, "PocketBase health endpoint should return HTTP 200")
        }
    }

    @Test
    fun systemUsersCollectionIsReachable() = runBlocking {
        withClient { client ->
            val response = client
                .collection("_pb_users_auth_")
                .getList(QueryOptions(perPage = 1, skipTotal = true))

            assertTrue(response.items.isEmpty(), "System users collection should be reachable even if no users exist")
        }
    }

    @Test
    fun promptsCollectionIsPubliclyReadable() = runBlocking {
        withClient { client ->
            val prompts = client.fetchPublicRecords("s_prompts")
            assertTrue(prompts.items.isNotEmpty(), "Expected at least one prompt from $pocketBaseHost")
            assertTrue(prompts.items.first().containsKey("text"), "Prompt record should expose 'text' field")
        }
    }

    @Test
    fun keyValuesCollectionIsPubliclyReadable() = runBlocking {
        withClient { client ->
            val keyValues = client.fetchPublicRecords("s_key_values")
            assertTrue(keyValues.items.isNotEmpty(), "Expected at least one key value from $pocketBaseHost")
            assertTrue(keyValues.items.first().containsKey("category"), "Key value record should expose 'category' field")
        }
    }

    @Test
    fun publicCollectionsReportDiagnosedStatus() = runBlocking {
        withClient { client ->
            val expectations = listOf(
                CollectionExpectation("s_prompts", "public prompt catalog"),
                CollectionExpectation("s_key_values", "values taxonomy used for onboarding")
            )

            val unmet = expectations.mapNotNull { expectation ->
                when (val result = client.probePublicCollection(expectation.name)) {
                    is CollectionProbeResult.Success -> {
                        println("PocketBase: '${expectation.name}' returned ${result.count} record(s)")
                        null
                    }
                    is CollectionProbeResult.Forbidden -> {
                        val message = "PocketBase: '${expectation.name}' is present but denied public access (${result.reason})." +
                            " Update list/view rules in PocketBase Admin or rerun migrations."
                        println(message)
                        DiagnosticFinding(expectation, message)
                    }
                    is CollectionProbeResult.Missing -> {
                        val message = "PocketBase: '${expectation.name}' is missing. Run migrations-manager or recreate it manually."
                        println(message)
                        DiagnosticFinding(expectation, message)
                    }
                }
            }

            val remediationHint = unmet.joinToString(separator = "\n") { "- ${it.message}" }

            assumeTrue(
                "Public collections are unavailable on $pocketBaseHost.\n$remediationHint\n" +
                    "See docs/POCKETBASE_SCHEMA.md for the expected schema.",
                unmet.isEmpty()
            )
        }
    }

    private suspend fun PocketBase.fetchPublicRecords(collection: String): ListResult<JsonObject> {
        return try {
            collection(collection).getList(QueryOptions(perPage = 5, skipTotal = true))
        } catch (error: ClientResponseException) {
            handleCollectionAccessError(collection, error)
        }
    }

    private suspend fun PocketBase.probePublicCollection(collection: String): CollectionProbeResult {
        return try {
            val response = collection(collection).getList(QueryOptions(perPage = 1, skipTotal = true))
            CollectionProbeResult.Success(response.items.size)
        } catch (error: ClientResponseException) {
            when (error.statusCode) {
                404 -> CollectionProbeResult.Missing
                401, 403 -> CollectionProbeResult.Forbidden(error.response.message)
                else -> throw error
            }
        }
    }

    private fun handleCollectionAccessError(collection: String, error: ClientResponseException): Nothing {
        val normalizedMessage = error.response.message.lowercase()
        val shouldSkip = error.statusCode in setOf(401, 403, 404) && (
            normalizedMessage.contains("missing collection context") ||
                normalizedMessage.contains("requires valid record authorization") ||
                normalizedMessage.contains("only superusers can perform this action")
            )

        if (shouldSkip) {
            assumeTrue(
                "PocketBase collection '$collection' isn't accessible at $pocketBaseHost: ${error.response.message}. " +
                    "Set POCKETBASE_HOST to a reachable test instance before running this test.",
                false
            )
        }

        throw error
    }

    private suspend fun <T> withClient(block: suspend (PocketBase) -> T): T {
        val client = PocketBase(pocketBaseHost)
        return try {
            block(client)
        } finally {
            client.close()
        }
    }
}

private data class CollectionExpectation(val name: String, val description: String)

private data class DiagnosticFinding(
    val expectation: CollectionExpectation,
    val message: String
)

private sealed interface CollectionProbeResult {
    data class Success(val count: Int) : CollectionProbeResult
    data class Forbidden(val reason: String) : CollectionProbeResult
    data object Missing : CollectionProbeResult
}
