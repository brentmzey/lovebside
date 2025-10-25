package love.bside.app.validation

import love.bside.app.core.validation.RequestValidators
import love.bside.app.core.validation.ValidationResult
import kotlin.test.*

/**
 * Comprehensive tests for RequestValidators
 * Tests all validation rules without external dependencies
 */
class RequestValidatorsTest {

    // ============================================================================
    // AUTHENTICATION TESTS
    // ============================================================================

    @Test
    fun `valid registration passes validation`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-05-15",
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `registration with invalid email fails`() {
        val result = RequestValidators.validateRegistration(
            email = "not-an-email",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-05-15",
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `registration with weak password fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "weak",
            passwordConfirm = "weak",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-05-15",
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `registration with mismatched passwords fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "DifferentPass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-05-15",
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `registration with underage birthdate fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "2015-01-01", // Too young
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `registration with invalid seeking value fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-05-15",
            seeking = "INVALID_SEEKING"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `valid login passes validation`() {
        val result = RequestValidators.validateLogin(
            email = "john@example.com",
            password = "SecurePass123"
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `login with empty email fails`() {
        val result = RequestValidators.validateLogin(
            email = "",
            password = "SecurePass123"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // PROFILE VALIDATION TESTS
    // ============================================================================

    @Test
    fun `valid profile update passes validation`() {
        val result = RequestValidators.validateProfileUpdate(
            firstName = "John",
            lastName = "Doe",
            bio = "I love hiking and photography",
            location = "San Francisco, CA"
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `profile with HTML in bio fails`() {
        val result = RequestValidators.validateProfileUpdate(
            firstName = "John",
            lastName = "Doe",
            bio = "I love <script>alert('xss')</script>",
            location = "San Francisco, CA"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `profile with too long bio fails`() {
        val bio = "a".repeat(501) // Over 500 char limit
        val result = RequestValidators.validateProfileUpdate(
            firstName = "John",
            lastName = "Doe",
            bio = bio,
            location = "San Francisco, CA"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `profile with invalid name characters fails`() {
        val result = RequestValidators.validateProfileUpdate(
            firstName = "John123", // Numbers not allowed
            lastName = "Doe",
            bio = "Valid bio",
            location = "San Francisco, CA"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // VALUES VALIDATION TESTS
    // ============================================================================

    @Test
    fun `valid value selection passes validation`() {
        val result = RequestValidators.validateValueSelection(
            keyValueId = "value_123",
            importance = 8
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `value selection with invalid importance fails`() {
        val result = RequestValidators.validateValueSelection(
            keyValueId = "value_123",
            importance = 11 // Out of 1-10 range
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `value selection with zero importance fails`() {
        val result = RequestValidators.validateValueSelection(
            keyValueId = "value_123",
            importance = 0
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // QUESTIONNAIRE VALIDATION TESTS
    // ============================================================================

    @Test
    fun `valid answer submission passes validation`() {
        val result = RequestValidators.validateAnswerSubmission(
            promptId = "prompt_123",
            answer = "This is a thoughtful answer to the prompt."
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `answer with too short text fails`() {
        val result = RequestValidators.validateAnswerSubmission(
            promptId = "prompt_123",
            answer = "Short" // Less than 10 chars
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `answer with too long text fails`() {
        val longText = "a".repeat(501) // Over 500 chars
        val result = RequestValidators.validateAnswerSubmission(
            promptId = "prompt_123",
            answer = longText
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `answer with HTML tags fails`() {
        val result = RequestValidators.validateAnswerSubmission(
            promptId = "prompt_123",
            answer = "This is <b>bold</b> text with HTML tags"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // MATCHING VALIDATION TESTS
    // ============================================================================

    @Test
    fun `valid match request passes validation`() {
        val result = RequestValidators.validateMatchRequest(
            userId = "user_123",
            matchedUserId = "user_456"
        )
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `match request to self fails`() {
        val result = RequestValidators.validateMatchRequest(
            userId = "user_123",
            matchedUserId = "user_123" // Same user
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `valid compatibility score passes validation`() {
        val result = RequestValidators.validateCompatibilityScore(75.5)
        
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `compatibility score above 100 fails`() {
        val result = RequestValidators.validateCompatibilityScore(100.1)
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `negative compatibility score fails`() {
        val result = RequestValidators.validateCompatibilityScore(-5.0)
        
        assertTrue(result is ValidationResult.Invalid)
    }

    // ============================================================================
    // EDGE CASES & SECURITY TESTS
    // ============================================================================

    @Test
    fun `profile with SQL injection attempt fails`() {
        val result = RequestValidators.validateProfileUpdate(
            firstName = "John",
            lastName = "Doe'; DROP TABLE users--",
            bio = "Valid bio",
            location = "San Francisco"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `name with only whitespace fails`() {
        val result = RequestValidators.validateProfileUpdate(
            firstName = "   ",
            lastName = "Doe",
            bio = "Valid bio",
            location = "San Francisco"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `birth date in future fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "2030-01-01", // Future date
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }

    @Test
    fun `very old birth date fails`() {
        val result = RequestValidators.validateRegistration(
            email = "john@example.com",
            password = "SecurePass123",
            passwordConfirm = "SecurePass123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1900-01-01", // Over 100 years old
            seeking = "FRIENDSHIP"
        )
        
        assertTrue(result is ValidationResult.Invalid)
    }
}
