package love.bside.app.data.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import love.bside.app.core.AppException
import love.bside.app.core.AppLogger
import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.core.logError
import love.bside.app.core.logInfo
import love.bside.app.core.logWarn
import love.bside.app.core.network.retryable

/**
 * PocketBase SDK-like client for Kotlin
 * Provides CRUD operations and authentication similar to the official PocketBase SDKs
 */
class PocketBaseClient(
    val client: HttpClient,  // Changed to public
    val baseUrl: String = "https://bside.pockethost.io/api/"  // Changed to public
) {
    
    /**
     * Generic list response from PocketBase
     */
    @Serializable
    data class ListResult<T>(
        val page: Int = 1,
        val perPage: Int = 30,
        val totalItems: Int = 0,
        val totalPages: Int = 0,
        val items: List<T> = emptyList()
    )

    /**
     * Auth response from PocketBase
     */
    @Serializable
    data class AuthResponse<T>(
        val token: String,
        val record: T
    )

    /**
     * Get a list of records from a collection
     */
    suspend inline fun <reified T> getList(
        collection: String,
        page: Int = 1,
        perPage: Int = 30,
        filter: String? = null,
        sort: String? = null,
        expand: String? = null
    ): Result<ListResult<T>> {
        val logPrefix = "PocketBase.getList($collection)"
        logDebug("$logPrefix: page=$page, perPage=$perPage, filter=$filter")
        
        return retryable {
            try {
                val response = client.get("${baseUrl}collections/$collection/records") {
                    parameter("page", page)
                    parameter("perPage", perPage)
                    filter?.let { parameter("filter", it) }
                    sort?.let { parameter("sort", it) }
                    expand?.let { parameter("expand", it) }
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { response.body<String>() } catch (e: Exception) { "Unable to read error" }
                    logError("$logPrefix: HTTP ${response.status.value} - $errorBody")
                    throw AppException.Network.ServerError(response.status.value, errorBody)
                }
                
                val result = response.body<ListResult<T>>()
                logInfo("$logPrefix: ✅ Success - fetched ${result.items.size}/${result.totalItems} items")
                result
                
            } catch (e: Exception) {
                logError("$logPrefix: ❌ Failed", e)
                throw e
            }
        }
    }

    /**
     * Get a single record by ID
     */
    suspend inline fun <reified T> getOne(
        collection: String,
        id: String,
        expand: String? = null
    ): Result<T> {
        val logPrefix = "PocketBase.getOne($collection/$id)"
        logDebug("$logPrefix: expand=$expand")
        
        return retryable {
            try {
                val response = client.get("${baseUrl}collections/$collection/records/$id") {
                    expand?.let { parameter("expand", it) }
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { response.body<String>() } catch (e: Exception) { "Unable to read error" }
                    logError("$logPrefix: HTTP ${response.status.value} - $errorBody")
                    
                    if (response.status.value == 404) {
                        throw AppException.Business.ResourceNotFound(collection, id)
                    }
                    throw AppException.Network.ServerError(response.status.value, errorBody)
                }
                
                val result = response.body<T>()
                logInfo("$logPrefix: ✅ Success")
                result
                
            } catch (e: Exception) {
                logError("$logPrefix: ❌ Failed", e)
                throw e
            }
        }
    }

    /**
     * Get the first record matching a filter
     */
    suspend inline fun <reified T> getFirstListItem(
        collection: String,
        filter: String,
        expand: String? = null
    ): Result<T> {
        logDebug("PocketBase getFirstListItem: collection=$collection, filter=$filter")
        
        return retryable {
            val listResult = client.get("${baseUrl}collections/$collection/records") {
                parameter("page", 1)
                parameter("perPage", 1)
                parameter("filter", filter)
                expand?.let { parameter("expand", it) }
            }.body<ListResult<T>>()
            
            if (listResult.items.isEmpty()) {
                throw AppException.Business.ResourceNotFound(collection, null)
            }
            
            listResult.items.first()
        }
    }

    /**
     * Create a new record
     */
    suspend inline fun <reified T, reified R> create(
        collection: String,
        body: T,
        expand: String? = null
    ): Result<R> {
        val logPrefix = "PocketBase.create($collection)"
        logDebug("$logPrefix: body=$body")
        
        return retryable {
            try {
                val response = client.post("${baseUrl}collections/$collection/records") {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                    expand?.let { parameter("expand", it) }
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { response.body<String>() } catch (e: Exception) { "Unable to read error" }
                    logError("$logPrefix: HTTP ${response.status.value} - $errorBody")
                    
                    when (response.status.value) {
                        400 -> throw AppException.Validation.InvalidInput("request", errorBody)
                        403 -> throw AppException.Auth.Unauthorized()
                        else -> throw AppException.Network.ServerError(response.status.value, errorBody)
                    }
                }
                
                val result = response.body<R>()
                logInfo("$logPrefix: ✅ Success - record created")
                result
                
            } catch (e: Exception) {
                logError("$logPrefix: ❌ Failed", e)
                throw e
            }
        }
    }

    /**
     * Update a record
     */
    suspend inline fun <reified T, reified R> update(
        collection: String,
        id: String,
        body: T,
        expand: String? = null
    ): Result<R> {
        logDebug("PocketBase update: collection=$collection, id=$id")
        
        return retryable {
            val response = client.patch("${baseUrl}collections/$collection/records/$id") {
                contentType(ContentType.Application.Json)
                setBody(body)
                expand?.let { parameter("expand", it) }
            }
            response.body<R>()
        }
    }

    /**
     * Delete a record
     */
    suspend fun delete(
        collection: String,
        id: String
    ): Result<Unit> {
        logDebug("PocketBase delete: collection=$collection, id=$id")
        
        return retryable {
            client.delete("${baseUrl}collections/$collection/records/$id")
        }
    }

    /**
     * Authenticate with email and password
     */
    suspend inline fun <reified T> authWithPassword(
        collection: String,
        identity: String,
        password: String
    ): Result<AuthResponse<T>> {
        val logPrefix = "PocketBase.authWithPassword($collection)"
        logDebug("$logPrefix: identity=$identity")
        
        return retryable {
            try {
                val response = client.post("${baseUrl}collections/$collection/auth-with-password") {
                    contentType(ContentType.Application.Json)
                    setBody(AuthRequest(identity, password))
                }
                
                if (!response.status.isSuccess()) {
                    val errorBody = try { response.body<String>() } catch (e: Exception) { "Unable to read error" }
                    logError("$logPrefix: HTTP ${response.status.value} - $errorBody")
                    
                    when (response.status.value) {
                        400, 401 -> throw AppException.Auth.InvalidCredentials()
                        403 -> throw AppException.Auth.Unauthorized()
                        429 -> {
                            logWarn("$logPrefix: Rate limited - too many login attempts")
                            throw AppException.Network.ServerError(429, "Too many login attempts. Please try again later.")
                        }
                        else -> throw AppException.Network.ServerError(response.status.value, errorBody)
                    }
                }
                
                val result = response.body<AuthResponse<T>>()
                logInfo("$logPrefix: ✅ Success - user authenticated")
                result
                
            } catch (e: Exception) {
                logError("$logPrefix: ❌ Authentication failed", e)
                throw e
            }
        }
    }
    
    /**
     * Auth request model
     */
    @Serializable
    data class AuthRequest(val identity: String, val password: String)

    /**
     * Refresh authentication token
     */
    suspend inline fun <reified T> authRefresh(
        collection: String = "users"
    ): Result<AuthResponse<T>> {
        logDebug("PocketBase authRefresh: collection=$collection")
        
        return retryable {
            val response = client.post("${baseUrl}collections/$collection/auth-refresh")
            response.body<AuthResponse<T>>()
        }
    }

    /**
     * Request password reset
     */
    suspend fun requestPasswordReset(
        collection: String,
        email: String
    ): Result<Unit> {
        logDebug("PocketBase requestPasswordReset: collection=$collection, email=$email")
        
        return retryable {
            @Serializable
            data class ResetRequest(val email: String)
            
            client.post("${baseUrl}collections/$collection/request-password-reset") {
                contentType(ContentType.Application.Json)
                setBody(ResetRequest(email))
            }
        }
    }

    /**
     * Confirm password reset
     */
    suspend fun confirmPasswordReset(
        collection: String,
        token: String,
        password: String,
        passwordConfirm: String
    ): Result<Unit> {
        logDebug("PocketBase confirmPasswordReset: collection=$collection")
        
        return retryable {
            @Serializable
            data class ConfirmRequest(
                val token: String,
                val password: String,
                val passwordConfirm: String
            )
            
            client.post("${baseUrl}collections/$collection/confirm-password-reset") {
                contentType(ContentType.Application.Json)
                setBody(ConfirmRequest(token, password, passwordConfirm))
            }
        }
    }

    /**
     * Execute custom SQL query (if enabled)
     */
    suspend inline fun <reified T> query(
        sql: String
    ): Result<List<T>> {
        logDebug("PocketBase query: sql=$sql")
        
        return retryable {
            val response = client.post("${baseUrl}_/query") {
                contentType(ContentType.Application.Json)
                setBody(QueryRequest(sql))
            }
            response.body<List<T>>()
        }
    }
    
    /**
     * Query request model
     */
    @Serializable
    data class QueryRequest(val sql: String)
}

/**
 * Extension function to handle PocketBase errors
 */
suspend inline fun <reified T> handlePocketBaseResponse(
    response: HttpResponse
): T {
    return when (response.status.value) {
        in 200..299 -> response.body()
        400 -> {
            val errorBody = response.body<String>()
            AppLogger.error("PocketBase", "Validation error: $errorBody")
            throw AppException.Validation.InvalidInput("request", "validation failed")
        }
        401 -> throw AppException.Auth.InvalidCredentials()
        403 -> throw AppException.Auth.Unauthorized()
        404 -> throw AppException.Business.ResourceNotFound("Resource", null)
        429 -> throw AppException.Network.ServerError(429, "Too many requests")
        in 500..599 -> throw AppException.Network.ServiceUnavailable()
        else -> throw AppException.Unknown("Unexpected response: ${response.status}")
    }
}
