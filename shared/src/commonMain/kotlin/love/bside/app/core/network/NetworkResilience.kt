package love.bside.app.core.network

import io.ktor.client.plugins.*
import kotlinx.coroutines.delay
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.appConfig
import love.bside.app.core.logError
import love.bside.app.core.logWarn

/**
 * Network resilience utilities for handling retries, timeouts, and error mapping.
 */
object NetworkResilience {

    /**
     * Executes a network request with automatic retry logic.
     */
    suspend fun <T> withRetry(
        maxAttempts: Int = appConfig().maxRetryAttempts,
        initialDelayMs: Long = appConfig().retryDelayMs,
        factor: Double = 2.0,
        block: suspend () -> T
    ): Result<T> {
        var currentDelay = initialDelayMs
        var lastException: AppException? = null

        repeat(maxAttempts) { attempt ->
            try {
                val result = block()
                return Result.Success(result)
            } catch (e: Exception) {
                lastException = e.toAppException()
                
                // Don't retry on auth errors or validation errors
                if (lastException is AppException.Auth || lastException is AppException.Validation) {
                    return Result.Error(lastException)
                }

                if (attempt < maxAttempts - 1) {
                    this.logWarn("Request failed (attempt ${attempt + 1}/$maxAttempts). Retrying in ${currentDelay}ms...", e)
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong()
                } else {
                    this.logError("Request failed after $maxAttempts attempts", e)
                }
            }
        }

        return Result.Error(lastException ?: AppException.Unknown("Unknown error occurred"))
    }

    /**
     * Executes a block and maps exceptions to AppException types.
     */
    suspend fun <T> safeCall(block: suspend () -> T): Result<T> = try {
        Result.Success(block())
    } catch (e: Exception) {
        Result.Error(e.toAppException())
    }

    /**
     * Maps generic exceptions to specific AppException types.
     */
    private fun Exception.toAppException(): AppException = when (this) {
        is AppException -> this
        is ResponseException -> {
            when (response.status.value) {
                401 -> AppException.Auth.SessionExpired(this)
                403 -> AppException.Auth.Unauthorized(this)
                404 -> AppException.Business.ResourceNotFound("Resource", null)
                408 -> AppException.Network.Timeout(this)
                429 -> AppException.Network.ServerError(429, "Too many requests. Please try again later.", this)
                in 500..599 -> AppException.Network.ServiceUnavailable(this)
                else -> AppException.Network.ServerError(response.status.value, cause = this)
            }
        }
        is HttpRequestTimeoutException -> AppException.Network.Timeout(this)
        else -> when {
            message?.contains("connection", ignoreCase = true) == true ||
            message?.contains("network", ignoreCase = true) == true -> 
                AppException.Network.NoConnection(this)
            message?.contains("timeout", ignoreCase = true) == true -> 
                AppException.Network.Timeout(this)
            message?.contains("parse", ignoreCase = true) == true ||
            message?.contains("serialization", ignoreCase = true) == true -> 
                AppException.Parsing(cause = this)
            else -> AppException.Unknown(message ?: "Unknown error", this)
        }
    }
}

/**
 * Extension function for easier retry usage
 */
suspend fun <T> retryable(block: suspend () -> T): Result<T> =
    NetworkResilience.withRetry(block = block)
