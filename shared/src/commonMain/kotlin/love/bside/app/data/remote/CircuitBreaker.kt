package love.bside.app.data.remote

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.concurrent.Volatile
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Circuit breaker pattern to prevent cascade failures
 * 
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Too many failures, reject immediately
 * - HALF_OPEN: Testing if service recovered
 * 
 * Strategy:
 * - After 5 failures → OPEN for 30s
 * - After 30s → HALF_OPEN (allow 1 test request)
 * - If test succeeds → CLOSED
 * - If test fails → OPEN for another 30s
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val successThreshold: Int = 2,
    private val openDuration: Duration = 30.seconds
) {
    private val mutex = Mutex()
    
    enum class State {
        CLOSED,      // Normal operation
        OPEN,        // Rejecting requests
        HALF_OPEN    // Testing if recovered
    }
    
    @Volatile
    private var state: State = State.CLOSED
    
    @Volatile
    private var failureCount: Int = 0
    
    @Volatile
    private var successCount: Int = 0
    
    @Volatile
    private var lastFailureTime: Instant? = null
    
    /**
     * Checks if a request should be allowed
     */
    suspend fun shouldAllowRequest(): Boolean = mutex.withLock {
        when (state) {
            State.CLOSED -> true
            State.OPEN -> {
                // Check if cooldown period has passed
                val lastFailure = lastFailureTime
                if (lastFailure != null &&
                    Clock.System.now() - lastFailure >= openDuration
                ) {
                    // Transition to half-open to test
                    state = State.HALF_OPEN
                    successCount = 0
                    true
                } else {
                    false
                }
            }
            State.HALF_OPEN -> true
        }
    }
    
    /**
     * Records a successful request
     */
    suspend fun recordSuccess() = mutex.withLock {
        when (state) {
            State.CLOSED -> {
                failureCount = 0
            }
            State.HALF_OPEN -> {
                successCount++
                if (successCount >= successThreshold) {
                    // Service recovered, close circuit
                    state = State.CLOSED
                    failureCount = 0
                    lastFailureTime = null
                }
            }
            State.OPEN -> {
                // Shouldn't happen, but reset counts
                state = State.CLOSED
                failureCount = 0
                successCount = 0
                lastFailureTime = null
            }
        }
    }
    
    /**
     * Records a failed request
     */
    suspend fun recordFailure() = mutex.withLock {
        failureCount++
        lastFailureTime = Clock.System.now()
        
        when (state) {
            State.CLOSED -> {
                if (failureCount >= failureThreshold) {
                    state = State.OPEN
                }
            }
            State.HALF_OPEN -> {
                // Test failed, go back to open
                state = State.OPEN
                successCount = 0
            }
            State.OPEN -> {
                // Already open, update time
            }
        }
    }
    
    /**
     * Gets current circuit state
     */
    fun getState(): State = state
    
    /**
     * Resets the circuit breaker (useful for testing/admin)
     */
    suspend fun reset() = mutex.withLock {
        state = State.CLOSED
        failureCount = 0
        successCount = 0
        lastFailureTime = null
    }
}
