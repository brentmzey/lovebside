package io.pocketbase.services

import io.pocketbase.PocketBase
import io.pocketbase.models.*
import kotlinx.serialization.json.*

/**
 * Service for managing records in a specific collection.
 */
class RecordService(
    client: PocketBase,
    private val collectionIdOrName: String
) : BaseService(client) {
    
    private val basePath: String
        get() = "/api/collections/${collectionIdOrName}/records"
    
    /**
     * Get a list of records from the collection.
     */
    suspend fun getList(options: QueryOptions? = null): ListResult<JsonObject> {
        val response = client.send<JsonObject>(
            path = basePath,
            method = "GET",
            query = options?.toQueryMap()
        )
        
        return Json.decodeFromJsonElement(response)
    }
    
    /**
     * Get the full list of records (handles pagination automatically).
     */
    suspend fun getFullList(options: QueryOptions? = null): List<JsonObject> {
        val allRecords = mutableListOf<JsonObject>()
        var page = 1
        val perPage = options?.perPage ?: 500
        
        while (true) {
            val result = getList(
                options?.copy(page = page, perPage = perPage) 
                    ?: QueryOptions(page = page, perPage = perPage)
            )
            
            allRecords.addAll(result.items)
            
            if (result.page >= result.totalPages) break
            page++
        }
        
        return allRecords
    }
    
    /**
     * Get the first record that matches the filter.
     */
    suspend fun getFirstListItem(filter: String, options: QueryOptions? = null): JsonObject {
        val result = getList(
            options?.copy(filter = filter, perPage = 1, skipTotal = true)
                ?: QueryOptions(filter = filter, perPage = 1, skipTotal = true)
        )
        
        if (result.items.isEmpty()) {
            throw Exception("No record found matching the filter")
        }
        
        return result.items.first()
    }
    
    /**
     * Get a single record by its ID.
     */
    suspend fun getOne(id: String, options: QueryOptions? = null): JsonObject {
        return client.send(
            path = "$basePath/$id",
            method = "GET",
            query = options?.toQueryMap()
        )
    }
    
    /**
     * Create a new record in the collection.
     */
    suspend fun create(body: Map<String, Any>, options: QueryOptions? = null): JsonObject {
        return client.send(
            path = basePath,
            method = "POST",
            body = body,
            query = options?.toQueryMap()
        )
    }
    
    /**
     * Update an existing record by its ID.
     */
    suspend fun update(id: String, body: Map<String, Any>, options: QueryOptions? = null): JsonObject {
        return client.send(
            path = "$basePath/$id",
            method = "PATCH",
            body = body,
            query = options?.toQueryMap()
        )
    }
    
    /**
     * Delete a record by its ID.
     */
    suspend fun delete(id: String): Boolean {
        return try {
            client.send<JsonObject>(
                path = "$basePath/$id",
                method = "DELETE"
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Subscribe to realtime changes for this collection.
     * 
     * @param recordId Optional specific record ID, or "*" for all records. Defaults to "*".
     * @param callback Function to call when an event is received
     * @param options Optional query options (filter, expand, etc.)
     */
    suspend fun subscribe(
        recordId: String = "*",
        callback: RealtimeEventCallback,
        options: QueryOptions? = null
    ): UnsubscribeFunc {
        val topic = if (recordId == "*") {
            collectionIdOrName
        } else {
            "$collectionIdOrName/$recordId"
        }
        
        return client.realtime.subscribe(topic, callback, options)
    }
    
    /**
     * Unsubscribe from realtime changes.
     */
    suspend fun unsubscribe(recordId: String = "*") {
        val topic = if (recordId == "*") {
            collectionIdOrName
        } else {
            "$collectionIdOrName/$recordId"
        }
        
        client.realtime.unsubscribe(topic)
    }
    
    /**
     * Authenticate a record with email/username and password.
     */
    suspend fun authWithPassword(
        usernameOrEmail: String,
        password: String,
        options: QueryOptions? = null
    ): AuthResponse {
        val response = client.send<JsonObject>(
            path = "$basePath/auth-with-password",
            method = "POST",
            body = mapOf(
                "identity" to usernameOrEmail,
                "password" to password
            ),
            query = options?.toQueryMap()
        )
        
        val authResponse = Json.decodeFromJsonElement<AuthResponse>(response)
        
        // Save auth data to store
        client.authStore.save(authResponse.token, authResponse.record)
        
        return authResponse
    }
    
    /**
     * Refresh the authentication token.
     */
    suspend fun authRefresh(options: QueryOptions? = null): AuthResponse {
        val response = client.send<JsonObject>(
            path = "$basePath/auth-refresh",
            method = "POST",
            query = options?.toQueryMap()
        )
        
        val authResponse = Json.decodeFromJsonElement<AuthResponse>(response)
        
        // Save updated auth data
        client.authStore.save(authResponse.token, authResponse.record)
        
        return authResponse
    }
    
    /**
     * Request a password reset email.
     */
    suspend fun requestPasswordReset(email: String): Boolean {
        return try {
            client.send<JsonObject>(
                path = "$basePath/request-password-reset",
                method = "POST",
                body = mapOf("email" to email)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Confirm a password reset.
     */
    suspend fun confirmPasswordReset(
        token: String,
        password: String,
        passwordConfirm: String
    ): Boolean {
        return try {
            client.send<JsonObject>(
                path = "$basePath/confirm-password-reset",
                method = "POST",
                body = mapOf(
                    "token" to token,
                    "password" to password,
                    "passwordConfirm" to passwordConfirm
                )
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request a verification email.
     */
    suspend fun requestVerification(email: String): Boolean {
        return try {
            client.send<JsonObject>(
                path = "$basePath/request-verification",
                method = "POST",
                body = mapOf("email" to email)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Confirm email verification.
     */
    suspend fun confirmVerification(token: String): Boolean {
        return try {
            client.send<JsonObject>(
                path = "$basePath/confirm-verification",
                method = "POST",
                body = mapOf("token" to token)
            )
            true
        } catch (e: Exception) {
            false
        }
    }
}

/**
 * Convert QueryOptions to a query parameter map.
 */
private fun QueryOptions.toQueryMap(): Map<String, String> {
    return buildMap {
        filter?.let { put("filter", it) }
        sort?.let { put("sort", it) }
        expand?.let { put("expand", it) }
        fields?.let { put("fields", it) }
        page?.let { put("page", it.toString()) }
        perPage?.let { put("perPage", it.toString()) }
        skipTotal?.let { if (it) put("skipTotal", "1") }
    }
}
