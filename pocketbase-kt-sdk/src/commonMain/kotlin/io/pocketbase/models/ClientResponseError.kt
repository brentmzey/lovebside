package io.pocketbase.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Error response from PocketBase API.
 */
@Serializable
data class ErrorResponse(
    val code: Int = 0,
    val message: String = "",
    val data: JsonObject? = null
)

/**
 * Exception thrown when a PocketBase API request fails.
 */
class ClientResponseException(
    val url: String,
    val statusCode: Int,
    val response: ErrorResponse,
    val originalError: Throwable? = null
) : Exception("[$statusCode] ${response.message} ($url)", originalError) {
    
    val isAbort: Boolean
        get() = originalError is kotlinx.coroutines.CancellationException
}
