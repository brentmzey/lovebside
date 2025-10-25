package love.bside.app.data.api.pocketbase

import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.core.logError
import love.bside.app.core.network.retryable

/**
 * Internal request handler for PocketBase
 */
internal class PocketBaseRequest(
    private val client: PocketBase,
    private val path: String,
    private val method: HttpMethod,
    private val body: Any? = null,
    private val query: Map<String, String>? = null,
    private val headers: Map<String, String>? = null
) {
    suspend inline fun <reified T> execute(): Result<T> {
        return retryable {
            try {
                val url = if (path.startsWith("http")) path else "${client.baseUrl}/api/$path"
                
                logDebug("PocketBase request: $method $url")
                
                val response = client.httpClient.request(url) {
                    this.method = method
                    
                    // Add auth header if available
                    client.authToken.value?.let {
                        header(HttpHeaders.Authorization, it)
                    }
                    
                    // Add custom headers
                    if (headers != null) {
                        for ((key, value) in headers) {
                            header(key, value)
                        }
                    }
                    
                    // Add query parameters
                    if (query != null) {
                        for ((key, value) in query) {
                            parameter(key, value)
                        }
                    }
                    
                    // Add body if present
                    body?.let {
                        contentType(ContentType.Application.Json)
                        setBody(it)
                    }
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { 
                        response.body<PocketBaseError>() 
                    } catch (e: Exception) { 
                        PocketBaseError(response.status.value, "Unknown error", mapOf())
                    }
                    
                    logError("PocketBase error: ${response.status.value} - ${errorBody.message}")
                    throw AppException.Network.ServerError(response.status.value, errorBody.message)
                }
                
                response.body<T>()
                
            } catch (e: Exception) {
                logError("PocketBase request failed", e)
                throw e
            }
        }
    }
}

/**
 * Admin service for admin operations
 */
class AdminService(private val client: PocketBase) {
    
    /**
     * Authenticate as admin
     */
    suspend fun authWithPassword(
        identity: String,
        password: String
    ): Result<AuthResponse<JsonObject>> {
        logDebug("AdminService.authWithPassword")
        
        @Serializable
        data class AuthRequest(val identity: String, val password: String)
        
        return retryable {
            val result: AuthResponse<JsonObject> = client.send<AuthResponse<JsonObject>>(
                path = "admins/auth-with-password",
                method = HttpMethod.Post,
                body = AuthRequest(identity, password)
            ).getOrThrow()
            
            client.saveAuth(result.token, result.record)
            result
        }
    }
    
    /**
     * Refresh admin auth token
     */
    suspend fun authRefresh(): Result<AuthResponse<JsonObject>> {
        logDebug("AdminService.authRefresh")
        
        return retryable {
            val result: AuthResponse<JsonObject> = client.send<AuthResponse<JsonObject>>(
                path = "admins/auth-refresh",
                method = HttpMethod.Post
            ).getOrThrow()
            
            client.saveAuth(result.token, result.record)
            result
        }
    }
    
    /**
     * Request admin password reset
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        @Serializable
        data class ResetRequest(val email: String)
        
        return client.send(
            path = "admins/request-password-reset",
            method = HttpMethod.Post,
            body = ResetRequest(email)
        )
    }
    
    /**
     * Confirm admin password reset
     */
    suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): Result<Unit> {
        @Serializable
        data class ConfirmRequest(
            val token: String,
            val password: String,
            val passwordConfirm: String
        )
        
        return client.send(
            path = "admins/confirm-password-reset",
            method = HttpMethod.Post,
            body = ConfirmRequest(token, password, passwordConfirm)
        )
    }
}

/**
 * File service for file operations
 */
class FileService(private val client: PocketBase) {
    
    /**
     * Get file URL
     */
    fun getUrl(
        record: Record,
        filename: String,
        queryParams: Map<String, String> = emptyMap()
    ): String {
        return client.getFileUrl(record, filename, queryParams)
    }
    
    /**
     * Get file token for protected files
     */
    suspend fun getToken(): Result<FileToken> {
        return client.send(
            path = "files/token",
            method = HttpMethod.Post
        )
    }
}

@Serializable
data class FileToken(
    val token: String
)

/**
 * Health check service
 */
class HealthService(private val client: PocketBase) {
    
    /**
     * Check API health
     */
    suspend fun check(): Result<HealthCheck> {
        return client.send(
            path = "health",
            method = HttpMethod.Get
        )
    }
}

/**
 * Settings service (admin only)
 */
class SettingsService(private val client: PocketBase) {
    
    /**
     * Get all settings
     */
    suspend fun getAll(): Result<Settings> {
        return client.send(
            path = "settings",
            method = HttpMethod.Get
        )
    }
    
    /**
     * Update settings
     */
    suspend fun update(settings: Settings): Result<Settings> {
        return client.send(
            path = "settings",
            method = HttpMethod.Patch,
            body = settings
        )
    }
    
    /**
     * Test S3 settings
     */
    suspend fun testS3(filesystem: String = "storage"): Result<Unit> {
        return client.send(
            path = "settings/test/s3",
            method = HttpMethod.Post,
            query = mapOf("filesystem" to filesystem)
        )
    }
    
    /**
     * Test email settings
     */
    suspend fun testEmail(
        email: String,
        template: String = "verification"
    ): Result<Unit> {
        @Serializable
        data class EmailTest(val email: String, val template: String)
        
        return client.send(
            path = "settings/test/email",
            method = HttpMethod.Post,
            body = EmailTest(email, template)
        )
    }
}

/**
 * Realtime subscriptions service
 */
class RealtimeService(private val client: PocketBase) {
    
    private val subscriptions = mutableMapOf<String, (RealtimeEvent) -> Unit>()
    private var subscriptionCounter = 0
    
    /**
     * Subscribe to collection changes
     * 
     * Note: This is a simplified implementation.
     * Full implementation would use SSE (Server-Sent Events)
     */
    fun subscribe(
        collection: String,
        callback: (RealtimeEvent) -> Unit
    ): String {
        val id = "${collection}_${subscriptionCounter++}"
        subscriptions[id] = callback
        logDebug("Realtime: Subscribed to $collection with id $id")
        return id
    }
    
    /**
     * Unsubscribe from collection changes
     */
    fun unsubscribe(subscriptionId: String) {
        subscriptions.remove(subscriptionId)
        logDebug("Realtime: Unsubscribed $subscriptionId")
    }
    
    /**
     * Unsubscribe all
     */
    fun unsubscribeAll() {
        subscriptions.clear()
        logDebug("Realtime: Unsubscribed all")
    }
}

/**
 * Backup service (admin only)
 */
class BackupService(private val client: PocketBase) {
    
    /**
     * Get all backups
     */
    suspend fun getFullList(): Result<List<BackupInfo>> {
        return client.send(
            path = "backups",
            method = HttpMethod.Get
        )
    }
    
    /**
     * Create backup
     */
    suspend fun create(name: String): Result<Unit> {
        @Serializable
        data class BackupCreate(val name: String)
        
        return client.send(
            path = "backups",
            method = HttpMethod.Post,
            body = BackupCreate(name)
        )
    }
    
    /**
     * Delete backup
     */
    suspend fun delete(key: String): Result<Unit> {
        return client.send(
            path = "backups/$key",
            method = HttpMethod.Delete
        )
    }
    
    /**
     * Restore backup
     */
    suspend fun restore(key: String): Result<Unit> {
        return client.send(
            path = "backups/$key/restore",
            method = HttpMethod.Post
        )
    }
    
    /**
     * Download backup
     */
    fun getDownloadUrl(key: String, token: String? = null): String {
        val base = "${client.baseUrl}/api/backups/$key"
        return if (token != null) "$base?token=$token" else base
    }
}

/**
 * Logs service (admin only)
 */
class LogService(private val client: PocketBase) {
    
    /**
     * Get logs
     */
    suspend fun getList(
        page: Int = 1,
        perPage: Int = 30,
        filter: String? = null,
        sort: String? = null
    ): Result<ListResult<LogRequest>> {
        val query = buildMap<String, String> {
            put("page", page.toString())
            put("perPage", perPage.toString())
            filter?.let { put("filter", it) }
            sort?.let { put("sort", it) }
        }
        
        return client.send(
            path = "logs",
            method = HttpMethod.Get,
            query = query
        )
    }
    
    /**
     * Get single log
     */
    suspend fun getOne(id: String): Result<LogRequest> {
        return client.send(
            path = "logs/$id",
            method = HttpMethod.Get
        )
    }
    
    /**
     * Get log statistics
     */
    suspend fun getStats(filter: String? = null): Result<List<LogStat>> {
        val query = filter?.let { mapOf("filter" to it) }
        
        return client.send(
            path = "logs/stats",
            method = HttpMethod.Get,
            query = query
        )
    }
}

@Serializable
data class LogStat(
    val total: Int,
    val date: String
)

