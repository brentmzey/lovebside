package love.bside.server.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import love.bside.server.models.api.ApiError
import love.bside.server.models.api.ApiResponse
import love.bside.server.utils.toErrorResponse

/**
 * Configure error handling and status pages
 */
fun Application.configureStatusPages() {
    install(StatusPages) {
        // Handle general exceptions
        exception<Throwable> { call, cause ->
            call.application.environment.log.error("Unhandled exception", cause)
            
            // Check if development mode
            val isDevelopment = call.application.environment.config
                .propertyOrNull("app.environment")?.getString() == "development"
            
            call.respond(
                HttpStatusCode.InternalServerError,
                "Internal server error".toErrorResponse(
                    code = "INTERNAL_ERROR",
                    details = if (isDevelopment) {
                        mapOf("exception" to (cause.message ?: "Unknown error"))
                    } else null
                )
            )
        }
        
        // Handle validation errors
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                (cause.message ?: "Validation error").toErrorResponse(
                    code = "VALIDATION_ERROR",
                    details = cause.details
                )
            )
        }
        
        // Handle authentication errors
        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                (cause.message ?: "Authentication failed").toErrorResponse(code = "AUTHENTICATION_ERROR")
            )
        }
        
        // Handle authorization errors
        exception<AuthorizationException> { call, cause ->
            call.respond(
                HttpStatusCode.Forbidden,
                (cause.message ?: "Access denied").toErrorResponse(code = "AUTHORIZATION_ERROR")
            )
        }
        
        // Handle not found errors
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                (cause.message ?: "Not found").toErrorResponse(code = "NOT_FOUND")
            )
        }
        
        // Handle conflict errors (e.g., duplicate email)
        exception<ConflictException> { call, cause ->
            call.respond(
                HttpStatusCode.Conflict,
                (cause.message ?: "Conflict").toErrorResponse(code = "CONFLICT")
            )
        }
        
        // Handle 404 Not Found
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                "Resource not found".toErrorResponse(code = "NOT_FOUND")
            )
        }
        
        // Handle 401 Unauthorized
        status(HttpStatusCode.Unauthorized) { call, status ->
            call.respond(
                status,
                "Authentication required".toErrorResponse(code = "AUTHENTICATION_REQUIRED")
            )
        }
    }
}

// ===== Custom Exceptions =====

class ValidationException(
    message: String,
    val details: Map<String, String>? = null
) : Exception(message)

class AuthenticationException(message: String) : Exception(message)

class AuthorizationException(message: String) : Exception(message)

class NotFoundException(message: String) : Exception(message)

class ConflictException(message: String) : Exception(message)
