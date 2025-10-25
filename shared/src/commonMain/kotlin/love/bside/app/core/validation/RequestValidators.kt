package love.bside.app.core.validation

import kotlinx.datetime.*
import love.bside.app.core.AppException
import kotlin.time.Duration.Companion.days

/**
 * Comprehensive request validation for all API operations
 * Validates business rules, data constraints, and security requirements
 */
object RequestValidators {
    
    // ============================================================================
    // AUTHENTICATION VALIDATION
    // ============================================================================
    
    /**
     * Validates registration request
     */
    fun validateRegistration(
        email: String,
        password: String,
        passwordConfirm: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        seeking: String
    ): ValidationResult {
        val errors = mutableListOf<AppException.Validation>()
        
        // Email validation
        Validators.validateEmail(email).getErrorOrNull()?.let { errors.add(it) }
        
        // Password validation
        Validators.validatePassword(password).getErrorOrNull()?.let { errors.add(it) }
        
        // Password confirmation
        if (password != passwordConfirm) {
            errors.add(AppException.Validation.InvalidInput("passwordConfirm", "must match password"))
        }
        
        // Name validation
        validateName(firstName, "firstName").getErrorOrNull()?.let { errors.add(it) }
        validateName(lastName, "lastName").getErrorOrNull()?.let { errors.add(it) }
        
        // Birth date validation
        validateBirthDate(birthDate).getErrorOrNull()?.let { errors.add(it) }
        
        // Seeking validation
        validateSeeking(seeking).getErrorOrNull()?.let { errors.add(it) }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first()) // Return first error
        }
    }
    
    /**
     * Validates login request
     */
    fun validateLogin(email: String, password: String): ValidationResult {
        val errors = mutableListOf<AppException.Validation>()
        
        Validators.validateRequired(email, "email").getErrorOrNull()?.let { errors.add(it) }
        Validators.validateRequired(password, "password").getErrorOrNull()?.let { errors.add(it) }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first())
        }
    }
    
    // ============================================================================
    // PROFILE VALIDATION
    // ============================================================================
    
    /**
     * Validates profile update request
     */
    fun validateProfileUpdate(
        firstName: String? = null,
        lastName: String? = null,
        bio: String? = null,
        location: String? = null,
        seeking: String? = null
    ): ValidationResult {
        val errors = mutableListOf<AppException.Validation>()
        
        firstName?.let {
            validateName(it, "firstName").getErrorOrNull()?.let { e -> errors.add(e) }
        }
        
        lastName?.let {
            validateName(it, "lastName").getErrorOrNull()?.let { e -> errors.add(e) }
        }
        
        bio?.let {
            validateBio(it).getErrorOrNull()?.let { e -> errors.add(e) }
        }
        
        location?.let {
            validateLocation(it).getErrorOrNull()?.let { e -> errors.add(e) }
        }
        
        seeking?.let {
            validateSeeking(it).getErrorOrNull()?.let { e -> errors.add(e) }
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first())
        }
    }
    
    /**
     * Validates a name field (first name or last name)
     */
    fun validateName(name: String, fieldName: String): ValidationResult {
        val trimmed = name.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField(fieldName)
            )
            trimmed.length > 50 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(fieldName, "must not exceed 50 characters")
            )
            !trimmed.matches(Regex("^[a-zA-Z '-]+$")) -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    fieldName,
                    "can only contain letters, spaces, hyphens, and apostrophes"
                )
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates bio text
     */
    fun validateBio(bio: String): ValidationResult {
        val trimmed = bio.trim()
        
        return when {
            trimmed.length > 500 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("bio", "must not exceed 500 characters")
            )
            trimmed.contains(Regex("<[^>]+>")) -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("bio", "cannot contain HTML tags")
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates location text
     */
    fun validateLocation(location: String): ValidationResult {
        val trimmed = location.trim()
        
        return when {
            trimmed.length > 100 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("location", "must not exceed 100 characters")
            )
            trimmed.contains(Regex("<[^>]+>")) -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("location", "cannot contain HTML tags")
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates birth date
     * Requirements:
     * - Format: YYYY-MM-DD
     * - User must be 18-100 years old
     * - No future dates
     */
    fun validateBirthDate(birthDate: String): ValidationResult {
        return try {
            val date = LocalDate.parse(birthDate)
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val age = today.year - date.year - if (
                today.monthNumber < date.monthNumber ||
                (today.monthNumber == date.monthNumber && today.dayOfMonth < date.dayOfMonth)
            ) 1 else 0
            
            when {
                date > today -> ValidationResult.Invalid(
                    AppException.Validation.InvalidInput("birthDate", "cannot be in the future")
                )
                age < 18 -> ValidationResult.Invalid(
                    AppException.Validation.InvalidInput("birthDate", "you must be at least 18 years old")
                )
                age > 100 -> ValidationResult.Invalid(
                    AppException.Validation.InvalidInput("birthDate", "invalid age")
                )
                else -> ValidationResult.Valid
            }
        } catch (e: Exception) {
            ValidationResult.Invalid(
                AppException.Validation.InvalidFormat("birthDate", "YYYY-MM-DD")
            )
        }
    }
    
    /**
     * Validates seeking preference
     */
    fun validateSeeking(seeking: String): ValidationResult {
        val validValues = setOf("FRIENDSHIP", "RELATIONSHIP", "BOTH")
        return if (seeking in validValues) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    "seeking",
                    "must be one of: ${validValues.joinToString(", ")}"
                )
            )
        }
    }
    
    // ============================================================================
    // VALUES VALIDATION
    // ============================================================================
    
    /**
     * Validates value selection request
     */
    fun validateValueSelection(
        keyValueId: String,
        importance: Int
    ): ValidationResult {
        val errors = mutableListOf<AppException.Validation>()
        
        if (keyValueId.isBlank()) {
            errors.add(AppException.Validation.RequiredField("keyValueId"))
        }
        
        validateImportance(importance).getErrorOrNull()?.let { errors.add(it) }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first())
        }
    }
    
    /**
     * Validates importance rating (1-10)
     */
    fun validateImportance(importance: Int): ValidationResult {
        return Validators.validateRange(importance, "importance", min = 1, max = 10)
    }
    
    // ============================================================================
    // QUESTIONNAIRE VALIDATION
    // ============================================================================
    
    /**
     * Validates answer submission
     */
    fun validateAnswerSubmission(
        promptId: String,
        answer: String
    ): ValidationResult {
        val errors = mutableListOf<AppException.Validation>()
        
        if (promptId.isBlank()) {
            errors.add(AppException.Validation.RequiredField("promptId"))
        }
        
        validateAnswerText(answer).getErrorOrNull()?.let { errors.add(it) }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors.first())
        }
    }
    
    /**
     * Validates answer text
     * Requirements:
     * - Minimum 10 characters (meaningful answer)
     * - Maximum 500 characters
     * - No HTML tags
     * - Not just whitespace
     */
    fun validateAnswerText(answer: String): ValidationResult {
        val trimmed = answer.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("answer")
            )
            trimmed.length < 10 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("answer", "must be at least 10 characters")
            )
            trimmed.length > 500 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("answer", "must not exceed 500 characters")
            )
            trimmed.contains(Regex("<[^>]+>")) -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("answer", "cannot contain HTML tags")
            )
            trimmed.all { it.isWhitespace() } -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("answer", "cannot be only whitespace")
            )
            else -> ValidationResult.Valid
        }
    }
    
    // ============================================================================
    // MATCH VALIDATION
    // ============================================================================
    
    /**
     * Validates match request
     */
    fun validateMatchRequest(
        userId: String,
        matchedUserId: String
    ): ValidationResult {
        return when {
            userId.isBlank() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("userId")
            )
            matchedUserId.isBlank() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("matchedUserId")
            )
            userId == matchedUserId -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("matchedUserId", "cannot match with yourself")
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates compatibility score
     */
    fun validateCompatibilityScore(score: Double): ValidationResult {
        return when {
            score < 0.0 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("compatibilityScore", "cannot be negative")
            )
            score > 100.0 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("compatibilityScore", "cannot exceed 100")
            )
            else -> ValidationResult.Valid
        }
    }
    
    // ============================================================================
    // PAGINATION VALIDATION
    // ============================================================================
    
    /**
     * Validates pagination parameters
     */
    fun validatePagination(page: Int, perPage: Int): ValidationResult {
        return when {
            page < 1 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("page", "must be at least 1")
            )
            perPage < 1 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("perPage", "must be at least 1")
            )
            perPage > 100 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("perPage", "must not exceed 100")
            )
            else -> ValidationResult.Valid
        }
    }
    
    // ============================================================================
    // FILE UPLOAD VALIDATION
    // ============================================================================
    
    /**
     * Validates file upload
     */
    fun validateFileUpload(
        fileName: String,
        fileSize: Long,
        mimeType: String,
        maxSizeBytes: Long = 5_000_000 // 5MB default
    ): ValidationResult {
        val allowedTypes = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
        )
        
        return when {
            fileName.isBlank() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("fileName")
            )
            fileSize <= 0 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("fileSize", "must be greater than 0")
            )
            fileSize > maxSizeBytes -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    "fileSize",
                    "must not exceed ${maxSizeBytes / 1_000_000}MB"
                )
            )
            mimeType !in allowedTypes -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput(
                    "fileType",
                    "must be one of: ${allowedTypes.joinToString(", ")}"
                )
            )
            else -> ValidationResult.Valid
        }
    }
    
    // ============================================================================
    // SEARCH & FILTER VALIDATION
    // ============================================================================
    
    /**
     * Validates search query
     */
    fun validateSearchQuery(query: String, minLength: Int = 2): ValidationResult {
        val trimmed = query.trim()
        
        return when {
            trimmed.isEmpty() -> ValidationResult.Invalid(
                AppException.Validation.RequiredField("query")
            )
            trimmed.length < minLength -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("query", "must be at least $minLength characters")
            )
            trimmed.length > 100 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("query", "must not exceed 100 characters")
            )
            trimmed.contains(Regex("[<>]")) -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("query", "contains invalid characters")
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates age range filter
     */
    fun validateAgeRange(minAge: Int?, maxAge: Int?): ValidationResult {
        return when {
            minAge != null && minAge < 18 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("minAge", "must be at least 18")
            )
            maxAge != null && maxAge > 100 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("maxAge", "must not exceed 100")
            )
            minAge != null && maxAge != null && minAge > maxAge -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("minAge", "cannot be greater than maxAge")
            )
            else -> ValidationResult.Valid
        }
    }
    
    /**
     * Validates distance filter (in kilometers)
     */
    fun validateDistance(distance: Int?): ValidationResult {
        return when {
            distance != null && distance < 1 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("distance", "must be at least 1 km")
            )
            distance != null && distance > 10000 -> ValidationResult.Invalid(
                AppException.Validation.InvalidInput("distance", "must not exceed 10,000 km")
            )
            else -> ValidationResult.Valid
        }
    }
}

/**
 * Extension function to validate and throw on error
 */
fun ValidationResult.throwOnError() {
    if (this is ValidationResult.Invalid) {
        throw this.exception
    }
}

/**
 * Extension function to get value or throw
 */
inline fun <T> ValidationResult.getOrThrow(block: () -> T): T {
    return when (this) {
        is ValidationResult.Valid -> block()
        is ValidationResult.Invalid -> throw this.exception
    }
}
