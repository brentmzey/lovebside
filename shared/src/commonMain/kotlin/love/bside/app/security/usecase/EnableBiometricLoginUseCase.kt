package love.bside.app.security.usecase

import love.bside.app.domain.models.AuthDetails
import love.bside.app.security.SecureAuthFactor
import love.bside.app.security.SecureAuthManager
import love.bside.app.security.SecureEnrollmentResult

data class EnableBiometricLoginUseCase(
    private val secureAuthManager: SecureAuthManager
) {
    suspend operator fun invoke(
        authDetails: AuthDetails,
        email: String,
        label: String? = null,
        factor: SecureAuthFactor.Biometric = SecureAuthFactor.Biometric()
    ): SecureEnrollmentResult = secureAuthManager.enrollBiometric(authDetails, email, label, factor)
}
