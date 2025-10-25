package love.bside.app.data.remote

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.math.pow

/**
 * Retry policy with exponential backoff for transient failures
 * 
 * Strategy: 3 retries with 1s, 2s, 4s delays
 * Retryable errors: 500, 502, 503, 504, network timeouts
 */
class RetryPolicy(
    val maxRetries: Int = 3,
    private val initialDelay: Duration = 1.seconds,
    private val maxDelay: Duration = 8.seconds,
    private val factor: Double = 2.0
) {
    /**
     * Determines if an error is retryable
     */
    fun isRetryable(statusCode: Int?, exception: Throwable?): Boolean {
        return when {
            // Transient server errors
            statusCode in listOf(500, 502, 503, 504) -> true
            // Network errors (platform-specific, handle generically)
            exception?.message?.contains("timeout", ignoreCase = true) == true -> true
            exception?.message?.contains("connection", ignoreCase = true) == true -> true
            // Other cases - don't retry
            else -> false
        }
    }
    
    /**
     * Calculates delay for retry attempt (exponential backoff)
     */
    fun calculateDelay(attempt: Int): Duration {
        val delay = initialDelay.inWholeMilliseconds * factor.pow(attempt - 1)
        return Duration.parse("${minOf(delay.toLong(), maxDelay.inWholeMilliseconds)}ms")
    }
    
    /**
     * Checks if we should retry based on attempt count
     */
    fun shouldRetry(attempt: Int): Boolean {
        return attempt < maxRetries
    }
}
