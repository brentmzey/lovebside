package love.bside.app.core

/**
 * Base sealed class for all application exceptions.
 * Provides structured error handling with user-friendly messages.
 */
sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null,
    val code: String? = null
) : Exception(message, cause) {

    /**
     * Network-related errors
     */
    sealed class Network(
        override val message: String,
        override val cause: Throwable? = null,
        code: String? = null
    ) : AppException(message, cause, code) {
        
        data class NoConnection(
            override val cause: Throwable? = null
        ) : Network("No internet connection available. Please check your network.", cause, "NETWORK_001")

        data class Timeout(
            override val cause: Throwable? = null
        ) : Network("Request timed out. Please try again.", cause, "NETWORK_002")

        data class ServerError(
            val statusCode: Int,
            override val message: String = "Server error occurred (HTTP $statusCode)",
            override val cause: Throwable? = null
        ) : Network(message, cause, "NETWORK_003")

        data class ServiceUnavailable(
            override val cause: Throwable? = null
        ) : Network("Service is temporarily unavailable. Please try again later.", cause, "NETWORK_004")
    }

    /**
     * Authentication and authorization errors
     */
    sealed class Auth(
        override val message: String,
        override val cause: Throwable? = null,
        code: String? = null
    ) : AppException(message, cause, code) {
        
        data class InvalidCredentials(
            override val cause: Throwable? = null
        ) : Auth("Invalid email or password.", cause, "AUTH_001")

        data class SessionExpired(
            override val cause: Throwable? = null
        ) : Auth("Your session has expired. Please log in again.", cause, "AUTH_002")

        data class Unauthorized(
            override val cause: Throwable? = null
        ) : Auth("You are not authorized to perform this action.", cause, "AUTH_003")

        data class AccountLocked(
            val reason: String? = null,
            override val cause: Throwable? = null
        ) : Auth("Your account has been locked. ${reason ?: ""}", cause, "AUTH_004")
    }

    /**
     * Validation errors
     */
    sealed class Validation(
        override val message: String,
        override val cause: Throwable? = null,
        code: String? = null
    ) : AppException(message, cause, code) {
        
        data class InvalidInput(
            val field: String,
            val reason: String
        ) : Validation("Invalid $field: $reason", null, "VALIDATION_001")

        data class RequiredField(
            val field: String
        ) : Validation("$field is required.", null, "VALIDATION_002")

        data class InvalidFormat(
            val field: String,
            val expectedFormat: String
        ) : Validation("$field must be in format: $expectedFormat", null, "VALIDATION_003")
    }

    /**
     * Business logic errors
     */
    sealed class Business(
        override val message: String,
        override val cause: Throwable? = null,
        code: String? = null
    ) : AppException(message, cause, code) {
        
        data class ResourceNotFound(
            val resource: String,
            val id: String? = null
        ) : Business("$resource${id?.let { " with id $it" } ?: ""} not found.", null, "BUSINESS_001")

        data class DuplicateResource(
            val resource: String
        ) : Business("$resource already exists.", null, "BUSINESS_002")

        data class OperationNotAllowed(
            val operation: String,
            val reason: String? = null
        ) : Business("$operation is not allowed.${reason?.let { " $it" } ?: ""}", null, "BUSINESS_003")
    }

    /**
     * Data parsing and serialization errors
     */
    data class Parsing(
        override val message: String = "Failed to parse data.",
        override val cause: Throwable? = null
    ) : AppException(message, cause, "PARSING_001")

    /**
     * Unknown or unexpected errors
     */
    data class Unknown(
        override val message: String = "An unexpected error occurred.",
        override val cause: Throwable? = null
    ) : AppException(message, cause, "UNKNOWN_001")

    /**
     * User-friendly message for display in UI
     */
    fun getUserMessage(): String = message

    /**
     * Technical message for logging
     */
    fun getTechnicalMessage(): String = buildString {
        append("[${code ?: "NO_CODE"}] $message")
        cause?.let { append(" | Cause: ${it.message}") }
    }
}
