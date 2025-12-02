package love.bside.app.security.usecase

import love.bside.app.security.SecureAuthManager
import love.bside.app.security.SecureAuthResult
import love.bside.app.security.SecurePromptText
import love.bside.app.security.BiometricAvailability

class BiometricLoginUseCase(
    private val secureAuthManager: SecureAuthManager
) {
    suspend operator fun invoke(prompt: SecurePromptText = SecurePromptText.default()): SecureAuthResult =
        secureAuthManager.authenticateWithBiometric(prompt)

    suspend fun availability(): BiometricAvailability = secureAuthManager.canUseBiometric()
}
