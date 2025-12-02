package love.bside.app.security

interface BiometricPromptBridge {
    suspend fun canAuthenticate(
        factor: SecureAuthFactor.Biometric = SecureAuthFactor.Biometric()
    ): BiometricAvailability

    suspend fun authenticate(
        prompt: SecurePromptText,
        factor: SecureAuthFactor.Biometric = SecureAuthFactor.Biometric()
    ): BiometricAuthResult
}

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data object Canceled : BiometricAuthResult
    data class Error(val message: String) : BiometricAuthResult
}

class UnsupportedBiometricPromptBridge : BiometricPromptBridge {
    override suspend fun canAuthenticate(
        factor: SecureAuthFactor.Biometric
    ): BiometricAvailability = BiometricAvailability.Unavailable("Not supported on this platform")

    override suspend fun authenticate(
        prompt: SecurePromptText,
        factor: SecureAuthFactor.Biometric
    ): BiometricAuthResult = BiometricAuthResult.Error("Biometric authentication not available")
}
