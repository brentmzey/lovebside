package love.bside.server.plugins

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.http.*

/**
 * Validation middleware for automatic request validation
 * Validates incoming requests before they reach route handlers
 * 
 * NOTE: This is a framework for validation middleware.
 * Specific validation logic should be added based on your API models.
 */
fun Application.configureValidationMiddleware() {
    
    intercept(ApplicationCallPipeline.Call) {
        val path = call.request.path()
        val method = call.request.httpMethod
        
        // Only validate POST/PUT/PATCH requests
        if (method !in listOf(HttpMethod.Post, HttpMethod.Put, HttpMethod.Patch)) {
            return@intercept
        }
        
        // Add validation logic here based on path and method
        // Example:
        // when {
        //     path == "/api/auth/register" -> validateRegistration()
        //     path == "/api/profile" -> validateProfileUpdate()
        // }
    }
}

/**
 * User principal for JWT authentication
 */
data class UserPrincipal(
    val userId: String,
    val email: String
)

/**
 * Request size limits to prevent DoS attacks
 */
fun Application.configureRequestLimits() {
    intercept(ApplicationCallPipeline.Call) {
        val contentLength = call.request.headers["Content-Length"]?.toLongOrNull()
        
        if (contentLength != null && contentLength > 10_000_000) { // 10MB limit
            call.respond(HttpStatusCode.PayloadTooLarge, mapOf(
                "error" to "payload_too_large",
                "message" to "Request body exceeds maximum size of 10MB"
            ))
            finish()
            return@intercept
        }
    }
}

/**
 * Rate limiting by IP address
 */
class RateLimiter {
    private val requestCounts = mutableMapOf<String, MutableList<Long>>()
    private val windowMs = 60_000L // 1 minute window
    private val maxRequests = 100 // Max requests per window
    
    fun isAllowed(identifier: String): Boolean {
        val now = System.currentTimeMillis()
        val requests = requestCounts.getOrPut(identifier) { mutableListOf() }
        
        // Remove old requests outside the window
        requests.removeIf { it < now - windowMs }
        
        if (requests.size >= maxRequests) {
            return false
        }
        
        requests.add(now)
        return true
    }
    
    fun cleanup() {
        val now = System.currentTimeMillis()
        requestCounts.entries.removeIf { (_, requests) ->
            requests.removeIf { it < now - windowMs }
            requests.isEmpty()
        }
    }
}

fun Application.configureRateLimiting() {
    val rateLimiter = RateLimiter()
    
    intercept(ApplicationCallPipeline.Call) {
        // Skip rate limiting for health checks
        if (call.request.path() == "/health") {
            return@intercept
        }
        
        val identifier = call.request.local.remoteAddress
        
        if (!rateLimiter.isAllowed(identifier)) {
            call.respond(HttpStatusCode.TooManyRequests, mapOf(
                "error" to "rate_limit_exceeded",
                "message" to "Too many requests. Please try again later."
            ))
            finish()
            return@intercept
        }
    }
}
