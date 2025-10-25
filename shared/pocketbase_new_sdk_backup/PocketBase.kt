package love.bside.app.data.api.pocketbase

import io.ktor.client.HttpClient
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.JsonObject
import love.bside.app.core.Result
import love.bside.app.core.logInfo

/**
 * Full-featured PocketBase SDK for Kotlin Multiplatform
 * 
 * Complete implementation matching the JavaScript SDK:
 * - Collections API (CRUD operations)
 * - Authentication (email/password, OAuth2, refresh)
 * - Realtime subscriptions
 * - File uploads
 * - Admin API
 * - Health checks
 * - Backups
 * - Logs
 * 
 * Based on: https://pocketbase.io/docs/js-overview/
 * 
 * @param httpClient Ktor HTTP client for making requests
 * @param baseUrl PocketBase instance URL (e.g., "https://yourapp.pockethost.io")
 * 
 * Example usage:
 * ```kotlin
 * val pb = PocketBase(httpClient, "https://yourapp.pockethost.io")
 * 
 * // Authenticate
 * val authResult = pb.collection("users").authWithPassword("user@example.com", "password")
 * 
 * // Get records
 * val records = pb.collection("posts").getList<Post>(page = 1, perPage = 20)
 * 
 * // Create record
 * val newPost = pb.collection("posts").create(Post(title = "Hello"))
 * 
 * // Subscribe to changes
 * pb.collection("posts").subscribe<Post> { event ->
 *     when (event) {
 *         is RealtimeEvent.Create -> println("New post: ${event.record}")
 *         is RealtimeEvent.Update -> println("Updated post: ${event.record}")
 *         is RealtimeEvent.Delete -> println("Deleted post: ${event.record}")
 *     }
 * }
 * ```
 */
class PocketBase(
    val httpClient: HttpClient,
    val baseUrl: String
) {
    
    // Auth state management
    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()
    
    private val _authModel = MutableStateFlow<JsonObject?>(null)
    val authModel: StateFlow<JsonObject?> = _authModel.asStateFlow()
    
    /**
     * Returns whether the client is authenticated
     */
    val isValid: Boolean
        get() = _authToken.value != null && _authModel.value != null
    
    /**
     * Collection API accessor
     * 
     * Example:
     * ```kotlin
     * val records = pb.collection("users").getList<User>()
     * ```
     */
    fun collection(name: String): CollectionService {
        return CollectionService(this, name)
    }
    
    /**
     * Admin authentication API
     */
    val admins: AdminService by lazy {
        AdminService(this)
    }
    
    /**
     * Files API for generating file URLs
     */
    val files: FileService by lazy {
        FileService(this)
    }
    
    /**
     * Health check API
     */
    val health: HealthService by lazy {
        HealthService(this)
    }
    
    /**
     * Settings API (requires admin)
     */
    val settings: SettingsService by lazy {
        SettingsService(this)
    }
    
    /**
     * Realtime subscriptions API
     */
    val realtime: RealtimeService by lazy {
        RealtimeService(this)
    }
    
    /**
     * Backups API (requires admin)
     */
    val backups: BackupService by lazy {
        BackupService(this)
    }
    
    /**
     * Logs API (requires admin)
     */
    val logs: LogService by lazy {
        LogService(this)
    }
    
    /**
     * Save authentication data
     * 
     * @param token JWT token from authentication
     * @param model User/admin model from authentication response
     */
    fun saveAuth(token: String, model: JsonObject? = null) {
        _authToken.value = token
        _authModel.value = model
        logInfo("PocketBase: Auth saved")
    }
    
    /**
     * Clear authentication data
     */
    fun clearAuth() {
        _authToken.value = null
        _authModel.value = null
        logInfo("PocketBase: Auth cleared")
    }
    
    /**
     * Send an authenticated HTTP request to PocketBase
     * 
     * @param path API path (will be prefixed with /api/)
     * @param method HTTP method
     * @param body Request body
     * @param query Query parameters
     * @param headers Additional headers
     * @return Parsed response body
     */
    suspend inline fun <reified T> send(
        path: String,
        method: HttpMethod = HttpMethod.Get,
        body: Any? = null,
        query: Map<String, String>? = null,
        headers: Map<String, String>? = null
    ): Result<T> {
        return PocketBaseRequest(
            client = this,
            path = path,
            method = method,
            body = body,
            query = query,
            headers = headers
        ).execute()
    }
    
    /**
     * Build full URL for a file
     * 
     * @param record Record containing the file
     * @param filename Name of the file field
     * @param queryParams Optional query parameters (e.g., thumb size)
     * @return Full URL to access the file
     */
    fun getFileUrl(
        record: Record,
        filename: String,
        queryParams: Map<String, String> = emptyMap()
    ): String {
        val base = "$baseUrl/api/files/${record.collectionId}/${record.id}/$filename"
        if (queryParams.isEmpty()) return base
        
        val params = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$base?$params"
    }
}
