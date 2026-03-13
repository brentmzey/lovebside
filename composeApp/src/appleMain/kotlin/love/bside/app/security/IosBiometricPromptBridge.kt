package love.bside.app.security

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.LocalAuthentication.LAErrorUserCancel
import platform.LocalAuthentication.LAErrorSystemCancel
import kotlin.coroutines.resume

@OptIn(ExperimentalForeignApi::class)
class IosBiometricPromptBridge : BiometricPromptBridge {
    override suspend fun canAuthenticate(factor: SecureAuthFactor.Biometric): BiometricAvailability {
        val context = LAContext()
        val canEvaluate = context.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, null)
        return if (canEvaluate) BiometricAvailability.Available else BiometricAvailability.NotEnrolled
    }

    override suspend fun authenticate(
        prompt: SecurePromptText,
        factor: SecureAuthFactor.Biometric
    ): BiometricAuthResult = suspendCancellableCoroutine { continuation ->
        val context = LAContext()
        val reason = prompt.subtitle ?: prompt.title
        context.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = reason
        ) { success, error ->
            if (!continuation.isActive) {
                return@evaluatePolicy
            }
            when {
                success -> continuation.resume(BiometricAuthResult.Success)
                error?.code == LAErrorUserCancel || error?.code == LAErrorSystemCancel ->
                    continuation.resume(BiometricAuthResult.Canceled)
                else -> continuation.resume(
                    BiometricAuthResult.Error(error?.localizedDescription ?: "Biometric failure")
                )
            }
        }
        continuation.invokeOnCancellation { context.invalidate() }
    }
}
