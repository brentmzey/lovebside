package love.bside.app.data.api.pocketbase

import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.core.network.retryable

/**
 * Collection service for CRUD operations
 * 
 * Matches PocketBase JS SDK collection API:
 * - getList(): Get paginated list of records
 * - getOne(): Get single record by ID
 * - getFirstListItem(): Get first record matching filter
 * - getFullList(): Get all records (no pagination)
 * - create(): Create new record
 * - update(): Update existing record
 * - delete(): Delete record
 * - authWithPassword(): Authenticate with email/password
 * - authWithOAuth2(): Authenticate with OAuth2 provider
 * - authRefresh(): Refresh authentication token
 * - requestPasswordReset(): Request password reset email
 * - confirmPasswordReset(): Confirm password reset
 * - requestVerification(): Request email verification
 * - confirmVerification(): Confirm email verification
 * - requestEmailChange(): Request email change
 * - confirmEmailChange(): Confirm email change
 * - listAuthMethods(): List available auth methods
 * - subscribe(): Subscribe to realtime changes
 * 
 * Example:
 * ```kotlin
 * val collection = pb.collection("posts")
 * 
 * // List records
 * val posts = collection.getList<Post>(
 *     page = 1,
 *     perPage = 20,
 *     filter = "status = 'published'",
 *     sort = "-created"
 * )
 * 
 * // Get one record
 * val post = collection.getOne<Post>("RECORD_ID")
 * 
 * // Create record
 * val newPost = collection.create(PostCreate(title = "Hello"))
 * 
 * // Update record
 * val updated = collection.update("RECORD_ID", PostUpdate(title = "Updated"))
 * 
 * // Delete record
 * collection.delete("RECORD_ID")
 * 
 * // Authenticate
 * val auth = collection.authWithPassword<User>("user@example.com", "password")
 * ```
 */
class CollectionService(
    
    internal val client: PocketBase,
    
    internal val collectionName: String
) {
    
    /**
     * Get paginated list of records
     * 
     * @param page Page number (starts at 1)
     * @param perPage Records per page (max 500)
     * @param filter Filter query (PocketBase filter syntax)
     * @param sort Sort fields (prefix with - for descending)
     * @param expand Relations to expand
     * @param fields Specific fields to return
     * @return Paginated list of records
     */
    suspend inline fun <reified T> getList(
        page: Int = 1,
        perPage: Int = 30,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null,
        fields: String? = null
    ): Result<ListResult<T>> {
        logDebug("CollectionService.getList: collection=$collectionName, page=$page, perPage=$perPage")
        
        val query = buildMap<String, String> {
            put("page", page.toString())
            put("perPage", perPage.toString())
            filter?.let { put("filter", it) }
            sort?.let { put("sort", it) }
            expand?.let { put("expand", it) }
            fields?.let { put("fields", it) }
        }
        
        return client.send(
            path = "collections/$collectionName/records",
            method = HttpMethod.Get,
            query = query
        )
    }
    
    /**
     * Get all records without pagination
     * 
     * Note: Use with caution on large collections
     * 
     * @param batch Batch size for fetching (default 500)
     * @param filter Filter query
     * @param sort Sort fields
     * @param expand Relations to expand
     * @param fields Specific fields to return
     * @return All matching records
     */
    suspend inline fun <reified T> getFullList(
        batch: Int = 500,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null,
        fields: String? = null
    ): Result<List<T>> {
        logDebug("CollectionService.getFullList: collection=$collectionName")
        
        return retryable {
            val allItems = mutableListOf<T>()
            var page = 1
            
            while (true) {
                val result = getList<T>(
                    page = page,
                    perPage = batch,
                    filter = filter,
                    sort = sort,
                    expand = expand,
                    fields = fields
                ).getOrThrow()
                
                allItems.addAll(result.items)
                
                if (result.items.size < batch) break
                page++
            }
            
            allItems
        }
    }
    
    /**
     * Get first record matching filter
     * 
     * @param filter Filter query
     * @param expand Relations to expand
     * @param fields Specific fields to return
     * @return First matching record
     */
    suspend inline fun <reified T> getFirstListItem(
        filter: String,
        expand: String? = null,
        fields: String? = null
    ): Result<T> {
        logDebug("CollectionService.getFirstListItem: collection=$collectionName, filter=$filter")
        
        return retryable {
            val result = getList<T>(
                page = 1,
                perPage = 1,
                filter = filter,
                expand = expand,
                fields = fields
            ).getOrThrow()
            
            result.items.firstOrNull() 
                ?: throw AppException.Business.ResourceNotFound(collectionName, filter)
        }
    }
    
    /**
     * Get single record by ID
     * 
     * @param id Record ID
     * @param expand Relations to expand
     * @param fields Specific fields to return
     * @return Record
     */
    suspend inline fun <reified T> getOne(
        id: String,
        expand: String? = null,
        fields: String? = null
    ): Result<T> {
        logDebug("CollectionService.getOne: collection=$collectionName, id=$id")
        
        val query = buildMap<String, String> {
            expand?.let { put("expand", it) }
            fields?.let { put("fields", it) }
        }
        
        return client.send(
            path = "collections/$collectionName/records/$id",
            method = HttpMethod.Get,
            query = query
        )
    }
    
    /**
     * Create new record
     * 
     * @param body Record data
     * @param expand Relations to expand in response
     * @param fields Specific fields to return
     * @return Created record
     */
    suspend inline fun <reified B, reified T> create(
        body: B,
        expand: String? = null,
        fields: String? = null
    ): Result<T> {
        logDebug("CollectionService.create: collection=$collectionName")
        
        val query = buildMap<String, String> {
            expand?.let { put("expand", it) }
            fields?.let { put("fields", it) }
        }
        
        return client.send(
            path = "collections/$collectionName/records",
            method = HttpMethod.Post,
            body = body,
            query = query
        )
    }
    
    /**
     * Update existing record
     * 
     * @param id Record ID
     * @param body Update data
     * @param expand Relations to expand in response
     * @param fields Specific fields to return
     * @return Updated record
     */
    suspend inline fun <reified B, reified T> update(
        id: String,
        body: B,
        expand: String? = null,
        fields: String? = null
    ): Result<T> {
        logDebug("CollectionService.update: collection=$collectionName, id=$id")
        
        val query = buildMap<String, String> {
            expand?.let { put("expand", it) }
            fields?.let { put("fields", it) }
        }
        
        return client.send(
            path = "collections/$collectionName/records/$id",
            method = HttpMethod.Patch,
            body = body,
            query = query
        )
    }
    
    /**
     * Delete record
     * 
     * @param id Record ID
     */
    suspend fun delete(id: String): Result<Unit> {
        logDebug("CollectionService.delete: collection=$collectionName, id=$id")
        
        return client.send(
            path = "collections/$collectionName/records/$id",
            method = HttpMethod.Delete
        )
    }
    
    // Authentication methods
    
    /**
     * Authenticate with email and password
     * 
     * @param identity Email or username
     * @param password Password
     * @return Auth response with token and user record
     */
    suspend fun <T> authWithPassword(
        identity: String,
        password: String
    ): Result<AuthResponse<T>> {
        logDebug("CollectionService.authWithPassword: collection=$collectionName, identity=$identity")
        
        
        return retryable {
            val result: AuthResponse<T> = client.send<AuthResponse<T>>(
                path = "collections/$collectionName/auth-with-password",
                method = HttpMethod.Post,
                body = AuthPasswordRequest(identity, password)
            ).getOrThrow()
            
            // Save auth state
            client.saveAuth(result.token)
            
            result
        }
    }
    
    /**
     * Authenticate with OAuth2 provider
     * 
     * @param provider OAuth2 provider name (e.g., "google", "github")
     * @param code OAuth2 authorization code
     * @param codeVerifier PKCE code verifier
     * @param redirectUrl OAuth2 redirect URL
     * @return Auth response with token and user record
     */
    suspend fun <T> authWithOAuth2(
        provider: String,
        code: String,
        codeVerifier: String,
        redirectUrl: String
    ): Result<OAuth2AuthResponse<T>> {
        logDebug("CollectionService.authWithOAuth2: collection=$collectionName, provider=$provider")
        
        @Serializable
        data class OAuth2Request(
            val provider: String,
            val code: String,
            val codeVerifier: String,
            val redirectUrl: String
        )
        
        return retryable {
            val result: OAuth2AuthResponse<T> = client.send<OAuth2AuthResponse<T>>(
                path = "collections/$collectionName/auth-with-oauth2",
                method = HttpMethod.Post,
                body = OAuth2Request(provider, code, codeVerifier, redirectUrl)
            ).getOrThrow()
            
            // Save auth state
            client.saveAuth(result.token)
            
            result
        }
    }
    
    /**
     * Refresh authentication token
     * 
     * @return New auth response
     */
    suspend fun <T> authRefresh(): Result<AuthResponse<T>> {
        logDebug("CollectionService.authRefresh: collection=$collectionName")
        
        return retryable {
            val result: AuthResponse<T> = client.send<AuthResponse<T>>(
                path = "collections/$collectionName/auth-refresh",
                method = HttpMethod.Post
            ).getOrThrow()
            
            // Update auth state
            client.saveAuth(result.token)
            
            result
        }
    }
    
    /**
     * Request password reset email
     * 
     * @param email User email
     */
    suspend fun requestPasswordReset(email: String): Result<Unit> {
        logDebug("CollectionService.requestPasswordReset: collection=$collectionName, email=$email")
        
        @Serializable
        data class ResetRequest(val email: String)
        
        return client.send(
            path = "collections/$collectionName/request-password-reset",
            method = HttpMethod.Post,
            body = ResetRequest(email)
        )
    }
    
    /**
     * Confirm password reset
     * 
     * @param token Reset token from email
     * @param password New password
     * @param passwordConfirm Password confirmation
     */
    suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): Result<Unit> {
        logDebug("CollectionService.confirmPasswordReset: collection=$collectionName")
        
        @Serializable
        data class ConfirmRequest(
            val token: String,
            val password: String,
            val passwordConfirm: String
        )
        
        return client.send(
            path = "collections/$collectionName/confirm-password-reset",
            method = HttpMethod.Post,
            body = ConfirmRequest(token, password, passwordConfirm)
        )
    }
    
    /**
     * Request email verification
     * 
     * @param email User email
     */
    suspend fun requestVerification(email: String): Result<Unit> {
        logDebug("CollectionService.requestVerification: collection=$collectionName, email=$email")
        
        @Serializable
        data class VerifyRequest(val email: String)
        
        return client.send(
            path = "collections/$collectionName/request-verification",
            method = HttpMethod.Post,
            body = VerifyRequest(email)
        )
    }
    
    /**
     * Confirm email verification
     * 
     * @param token Verification token from email
     */
    suspend fun confirmVerification(token: String): Result<Unit> {
        logDebug("CollectionService.confirmVerification: collection=$collectionName")
        
        @Serializable
        data class ConfirmRequest(val token: String)
        
        return client.send(
            path = "collections/$collectionName/confirm-verification",
            method = HttpMethod.Post,
            body = ConfirmRequest(token)
        )
    }
    
    /**
     * Request email change
     * 
     * @param newEmail New email address
     */
    suspend fun requestEmailChange(newEmail: String): Result<Unit> {
        logDebug("CollectionService.requestEmailChange: collection=$collectionName")
        
        @Serializable
        data class EmailChangeRequest(val newEmail: String)
        
        return client.send(
            path = "collections/$collectionName/request-email-change",
            method = HttpMethod.Post,
            body = EmailChangeRequest(newEmail)
        )
    }
    
    /**
     * Confirm email change
     * 
     * @param token Email change token
     * @param password User password
     */
    suspend fun confirmEmailChange(
        token: String,
        password: String
    ): Result<Unit> {
        logDebug("CollectionService.confirmEmailChange: collection=$collectionName")
        
        @Serializable
        data class ConfirmRequest(val token: String, val password: String)
        
        return client.send(
            path = "collections/$collectionName/confirm-email-change",
            method = HttpMethod.Post,
            body = ConfirmRequest(token, password)
        )
    }
    
    /**
     * List available authentication methods
     * 
     * @return List of auth methods and OAuth2 providers
     */
    suspend fun listAuthMethods(): Result<AuthMethods> {
        logDebug("CollectionService.listAuthMethods: collection=$collectionName")
        
        return client.send(
            path = "collections/$collectionName/auth-methods",
            method = HttpMethod.Get
        )
    }
    
    /**
     * Subscribe to realtime changes
     * 
     * @param callback Function called when records change
     * @return Subscription ID for unsubscribing
     */
    fun subscribe(
        callback: (RealtimeEvent) -> Unit
    ): String {
        return client.realtime.subscribe(collectionName, callback)
    }
    
    /**
     * Unsubscribe from realtime changes
     * 
     * @param subscriptionId ID returned from subscribe()
     */
    fun unsubscribe(subscriptionId: String) {
        client.realtime.unsubscribe(subscriptionId)
    }
}

/**
 * Available authentication methods
 */
@Serializable
data class AuthMethods(
    val usernamePassword: Boolean,
    val emailPassword: Boolean,
    val authProviders: List<AuthProvider>
)

@Serializable
data class AuthProvider(
    val name: String,
    val state: String,
    val codeVerifier: String,
    val codeChallenge: String,
    val codeChallengeMethod: String,
    val authUrl: String
)

