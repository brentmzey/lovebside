package love.bside.app.security

import love.bside.app.domain.models.AuthDetails

sealed interface SecureAuthResult {
    data class Success(val details: AuthDetails) : SecureAuthResult
    data object NoEnrollment : SecureAuthResult
    data object Canceled : SecureAuthResult
    data class Error(val message: String) : SecureAuthResult
    data class Unavailable(val availability: BiometricAvailability) : SecureAuthResult
}

sealed interface SecureEnrollmentResult {
    data class Enrolled(val record: SecureEnrollmentRecord) : SecureEnrollmentResult
    data class Updated(val record: SecureEnrollmentRecord) : SecureEnrollmentResult
}
