package love.bside.app.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

/**
 * Main API client with resilience features:
 * - Retry with exponential backoff
 * - Circuit breaker for fail-fast
 * - Rate limiting
 * - Request/response logging
 * - Timeout handling
 */
class ApiClient(
    private val baseUrl: String = "http://127.0.0.1:8090",
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val circuitBreaker: CircuitBreaker = CircuitBreaker(),
    private val rateLimiter: RateLimiter = RateLimiter()
) {
    private val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = false
            })
        }
        
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000 // 30s default
            connectTimeoutMillis = 10_000 // 10s connect
            socketTimeoutMillis = 30_000  // 30s socket
        }
        
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
    
    /**
     * Execute HTTP request with resilience (retry, circuit breaker, rate limit)
     */
    suspend fun <T> execute(
        block: suspend HttpClient.() -> T
    ): Result<T> {
        // Check circuit breaker first
        if (!circuitBreaker.shouldAllowRequest()) {
            return Result.failure(
                CircuitBreakerOpenException("Circuit breaker is OPEN, request rejected")
            )
        }
        
        // Check rate limit
        if (!rateLimiter.acquireToken(waitIfExceeded = true)) {
            return Result.failure(
                RateLimitExceededException("Rate limit exceeded")
            )
        }
        
        // Execute with retry logic
        var attempt = 0
        var lastException: Throwable? = null
        
        while (attempt < retryPolicy.maxRetries) {
            attempt++
            
            try {
                val result = client.block()
                circuitBreaker.recordSuccess()
                return Result.success(result)
                
            } catch (e: Exception) {
                lastException = e
                
                // Extract status code if available
                val statusCode = (e as? ResponseException)?.response?.status?.value
                
                // Check if retryable
                if (!retryPolicy.isRetryable(statusCode, e)) {
                    circuitBreaker.recordFailure()
                    return Result.failure(e)
                }
                
                // Check if we should retry
                if (!retryPolicy.shouldRetry(attempt)) {
                    circuitBreaker.recordFailure()
                    return Result.failure(e)
                }
                
                // Wait before retry
                delay(retryPolicy.calculateDelay(attempt))
            }
        }
        
        // All retries failed
        circuitBreaker.recordFailure()
        return Result.failure(
            lastException ?: Exception("Request failed after ${retryPolicy.maxRetries} retries")
        )
    }
    
    /**
     * GET request
     */
    suspend fun <T> get(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> = execute {
        get(path, block)
    }
    
    /**
     * POST request
     */
    suspend fun <T> post(
        path: String,
        body: T? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> = execute {
        post(path) {
            if (body != null) setBody(body)
            block()
        }
    }
    
    /**
     * PUT request
     */
    suspend fun <T> put(
        path: String,
        body: T? = null,
        block: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> = execute {
        put(path) {
            if (body != null) setBody(body)
            block()
        }
    }
    
    /**
     * DELETE request
     */
    suspend fun delete(
        path: String,
        block: HttpRequestBuilder.() -> Unit = {}
    ): Result<HttpResponse> = execute {
        delete(path, block)
    }
    
    /**
     * Gets current circuit breaker state (for monitoring)
     */
    fun getCircuitState(): CircuitBreaker.State = circuitBreaker.getState()
    
    /**
     * Closes the HTTP client (cleanup)
     */
    fun close() {
        client.close()
    }
}

/**
 * Exception thrown when circuit breaker is open
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Exception thrown when rate limit is exceeded
 */
class RateLimitExceededException(message: String) : Exception(message)
