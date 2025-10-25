package io.pocketbase

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.pocketbase.models.ClientResponseException
import io.pocketbase.models.ErrorResponse
import io.pocketbase.services.RealtimeService
import io.pocketbase.services.RecordService
import io.pocketbase.stores.AuthStore
import io.pocketbase.stores.MemoryAuthStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*

/**
 * PocketBase Kotlin Multiplatform SDK Client.
 * 
 * This is a pure Kotlin implementation of the PocketBase client that works across all platforms:
 * - JVM (Android, Desktop)
 * - iOS
 * - JavaScript (Browser, Node.js)
 * 
 * Example usage:
 * ```kotlin
 * val pb = PocketBase("https://your-instance.pockethost.io")
 * 
 * // Authenticate
 * pb.collection("users").authWithPassword("user@example.com", "password")
 * 
 * // Get records
 * val records = pb.collection("t_message").getList()
 * 
 * // Subscribe to realtime changes
 * pb.collection("t_message").subscribe("*") { event ->
 *     println("Action: ${event.action}")
 *     println("Record: ${event.record}")
 * }
 * ```
 */
class PocketBase(
    /**
     * The base URL of your PocketBase instance.
     */
    var baseURL: String,
    
    /**
     * Authentication store for managing auth tokens and user data.
     * Defaults to an in-memory store. Provide a custom implementation for persistence.
     */
    val authStore: AuthStore = MemoryAuthStore()
) {
    @PublishedApi
    internal val httpClient: HttpClient
    @PublishedApi
    internal val requestMutex = Mutex()
    @PublishedApi
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
    }
    
    /**
     * Realtime service for SSE subscriptions.
     */
    val realtime: RealtimeService by lazy { RealtimeService(this) }
    
    /**
     * Hook called before sending each request.
     * Return modified url and/or options, or null to use defaults.
     */
    var beforeSend: (suspend (url: String, options: RequestOptions) -> RequestModification?)? = null
    
    /**
     * Hook called after receiving a response.
     * Can modify the parsed data before it's returned to the caller.
     */
    var afterSend: (suspend (response: HttpResponse, data: JsonObject) -> JsonObject)? = null
    
    init {
        // Normalize base URL - remove trailing slash
        baseURL = baseURL.trimEnd('/')
        
        httpClient = HttpClient {
            install(ContentNegotiation) {
                json(this@PocketBase.json)
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30000
                connectTimeoutMillis = 15000
                socketTimeoutMillis = 30000
            }
            
            // Don't throw on non-2xx responses, we'll handle them manually
            expectSuccess = false
        }
    }
    
    /**
     * Get a RecordService for a specific collection.
     */
    fun collection(collectionIdOrName: String): RecordService {
        return RecordService(this, collectionIdOrName)
    }
    
    /**
     * Build a full URL from a path.
     */
    @PublishedApi
    internal fun buildURL(path: String): String {
        val normalizedPath = path.trimStart('/')
        return "$baseURL/$normalizedPath"
    }
    
    /**
     * Send an HTTP request to the PocketBase API.
     * 
     * @param path The API path (e.g., "/api/collections/users/records")
     * @param method HTTP method
     * @param query Query parameters
     * @param body Request body (will be JSON encoded)
     * @param headers Additional headers
     * @return Parsed JSON response
     */
    suspend inline fun <reified T> send(
        path: String,
        method: String = "GET",
        query: Map<String, String>? = null,
        body: Any? = null,
        headers: Map<String, String>? = null
    ): T = requestMutex.withLock {
        var url = buildURL(path)
        
        // Add query parameters
        if (!query.isNullOrEmpty()) {
            val queryString = query.entries.joinToString("&") { (key, value) ->
                "${key.encodeURLParameter()}=${value.encodeURLParameter()}"
            }
            url += if (url.contains("?")) "&$queryString" else "?$queryString"
        }
        
        val requestOptions = RequestOptions(
            method = method,
            body = body,
            query = query,
            headers = headers?.toMutableMap() ?: mutableMapOf()
        )
        
        // Apply beforeSend hook
        val modification = beforeSend?.invoke(url, requestOptions)
        if (modification != null) {
            url = modification.url ?: url
            modification.headers?.let { requestOptions.headers.putAll(it) }
        }
        
        // Build request
        val response = httpClient.request(url) {
            this.method = HttpMethod.parse(requestOptions.method)
            
            // Set default headers
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)
            
            // Add auth token if available
            if (authStore.token.isNotEmpty()) {
                header(HttpHeaders.Authorization, authStore.token)
            }
            
            // Add custom headers
            requestOptions.headers.forEach { (key, value) ->
                header(key, value)
            }
            
            // Set body if present
            requestOptions.body?.let {
                setBody(it)
            }
        }
        
        // Handle response
        if (response.status.value !in 200..299) {
            val errorBody = try {
                response.body<ErrorResponse>()
            } catch (e: Exception) {
                ErrorResponse(
                    code = response.status.value,
                    message = response.status.description
                )
            }
            
            throw ClientResponseException(
                url = url,
                statusCode = response.status.value,
                response = errorBody
            )
        }
        
        // Parse response
        val responseBody = response.bodyAsText()
        
        return when (T::class) {
            JsonObject::class -> {
                if (responseBody.isEmpty()) {
                    JsonObject(emptyMap()) as T
                } else {
                    var parsed = json.parseToJsonElement(responseBody).jsonObject
                    
                    // Apply afterSend hook
                    afterSend?.let {
                        parsed = it(response, parsed)
                    }
                    
                    parsed as T
                }
            }
            String::class -> responseBody as T
            Unit::class -> Unit as T
            else -> {
                if (responseBody.isEmpty()) {
                    json.decodeFromString<T>("{}")
                } else {
                    json.decodeFromString<T>(responseBody)
                }
            }
        }
    }
    
    /**
     * Close the HTTP client and clean up resources.
     */
    fun close() {
        httpClient.close()
    }
}

/**
 * Options for HTTP requests.
 */
data class RequestOptions(
    var method: String,
    var body: Any? = null,
    var query: Map<String, String>? = null,
    var headers: MutableMap<String, String> = mutableMapOf()
)

/**
 * Result of beforeSend hook modification.
 */
data class RequestModification(
    val url: String? = null,
    val headers: Map<String, String>? = null
)

/**
 * Simple URL encoding for query parameters.
 */
@PublishedApi
internal fun String.encodeURLParameter(): String {
    return this.encodeURLPath()
}
