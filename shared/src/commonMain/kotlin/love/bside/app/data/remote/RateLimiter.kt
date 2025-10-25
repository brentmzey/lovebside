package love.bside.app.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Client-side rate limiter to prevent overwhelming the API
 * 
 * Strategy: Token bucket algorithm
 * - 100 requests per minute
 * - Burst capacity: 10 requests
 * - Tokens refill at ~1.67 per second
 */
class RateLimiter(
    private val maxRequests: Int = 100,
    private val timeWindow: Duration = 1.minutes,
    private val burstCapacity: Int = 10
) {
    private val mutex = Mutex()
    private var tokens: Int = burstCapacity
    private var lastRefillTime: Instant = Clock.System.now()
    
    /**
     * Acquires a token for a request (suspends if rate limit exceeded)
     * Returns false if circuit should fail-fast instead of waiting
     */
    suspend fun acquireToken(waitIfExceeded: Boolean = true): Boolean = mutex.withLock {
        refillTokens()
        
        return if (tokens > 0) {
            tokens--
            true
        } else if (waitIfExceeded) {
            // Calculate how long to wait for next token
            val tokensPerSecond = maxRequests.toDouble() / timeWindow.inWholeSeconds
            val waitTime = (1.0 / tokensPerSecond * 1000).toLong()
            
            // Release lock while waiting
            mutex.unlock()
            try {
                delay(waitTime)
                acquireToken(waitIfExceeded = false) // Try again without waiting
            } finally {
                mutex.lock()
            }
        } else {
            false
        }
    }
    
    /**
     * Refills tokens based on elapsed time
     */
    private fun refillTokens() {
        val now = Clock.System.now()
        val elapsed = now - lastRefillTime
        
        if (elapsed.inWholeMilliseconds > 0) {
            val tokensToAdd = (maxRequests * elapsed.inWholeSeconds.toDouble() / timeWindow.inWholeSeconds).toInt()
            
            if (tokensToAdd > 0) {
                tokens = minOf(tokens + tokensToAdd, burstCapacity)
                lastRefillTime = now
            }
        }
    }
    
    /**
     * Gets current available tokens (for monitoring)
     */
    suspend fun getAvailableTokens(): Int = mutex.withLock {
        refillTokens()
        tokens
    }
}
