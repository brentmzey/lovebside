package love.bside.app.core.validation

import love.bside.app.core.AppException

/**
 * Validation utilities for input validation
 */
object Validators {

    /**
     * Validates an email address
     */
    fun validateEmail(email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("Email")
            )
            !email.matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$")) -> 
                ValidationResult.Invalid(
                    AppException.Validation.InvalidFormat("Email", "user@example.com")
                )
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates a password
     */
    fun validatePassword(password: String, minLength: Int = 8): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("Password")
            )
            password.length < minLength -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("Password", "must be at least $minLength characters")
            )
            !password.any { it.isDigit() } -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("Password", "must contain at least one digit")
            )
            !password.any { it.isUpperCase() } -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("Password", "must contain at least one uppercase letter")
            )
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates required field
     */
    fun validateRequired(value: String, fieldName: String): ValidationResult {
        return if (value.isBlank()) {
            ValidationResult.Invalid(AppException.Validation.RequiredField(fieldName))
        } else {
            ValidationResult.Valid
        }
    }

    /**
     * Validates string length
     */
    fun validateLength(
        value: String,
        fieldName: String,
        minLength: Int = 0,
        maxLength: Int = Int.MAX_VALUE
    ): ValidationResult {
        return when {
            value.length < minLength -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    fieldName,
                    "must be at least $minLength characters"
                )
            )
            value.length > maxLength -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    fieldName,
                    "must not exceed $maxLength characters"
                )
            )
            else -> ValidationResult.Valid
        }
    }

    /**
     * Validates numeric range
     */
    fun validateRange(
        value: Int,
        fieldName: String,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE
    ): ValidationResult {
        return when {
            value < min -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(fieldName, "must be at least $min")
            )
            value > max -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(fieldName, "must not exceed $max")
            )
            else -> ValidationResult.Valid
        }
    }

    /**
     * Combines multiple validation results
     */
    fun combine(vararg results: ValidationResult): ValidationResult {
        val errors = results.filterIsInstance<ValidationResult.Invalid>()
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first().exception)
        }
    }
}

/**
 * Result of a validation operation
 */
sealed class ValidationResult {
    data object Valid : ValidationResult()
    data class Invalid(val exception: AppException.Validation) : ValidationResult()

    val isValid: Boolean
        get() = this is Valid

    fun getErrorOrNull(): AppException.Validation? = when (this) {
        is Invalid -> exception
        is Valid -> null
    }
}

/**
 * Extension to throw if validation fails
 */
fun ValidationResult.orThrow() {
    if (this is ValidationResult.Invalid) {
        throw exception
    }
}
