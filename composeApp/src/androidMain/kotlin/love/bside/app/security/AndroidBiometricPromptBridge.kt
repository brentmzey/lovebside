package love.bside.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidBiometricPromptBridge(
    context: Context,
    private val activityHolder: CurrentActivityHolder
) : BiometricPromptBridge {
    private val biometricManager = BiometricManager.from(context)

    override suspend fun canAuthenticate(
        factor: SecureAuthFactor.Biometric
    ): BiometricAvailability {
        val authenticators = authenticators(factor)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NotEnrolled
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                BiometricAvailability.Unavailable("Hardware unavailable")
            else -> BiometricAvailability.Unavailable("Unknown biometric state")
        }
    }

    override suspend fun authenticate(
        prompt: SecurePromptText,
        factor: SecureAuthFactor.Biometric
    ): BiometricAuthResult = suspendCancellableCoroutine { continuation ->
        val host: FragmentActivity = activityHolder.currentActivity()
            ?: return@suspendCancellableCoroutine continuation.resume(
                BiometricAuthResult.Error("No active activity found")
            )

        val authenticators = authenticators(factor)
        val executor = ContextCompat.getMainExecutor(host)
        val biometricPrompt = BiometricPrompt(host, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                if (continuation.isActive) continuation.resume(BiometricAuthResult.Success)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                if (!continuation.isActive) return
                val cancelled = errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                continuation.resume(
                    if (cancelled) BiometricAuthResult.Canceled
                    else BiometricAuthResult.Error(errString.toString())
                )
            }

            override fun onAuthenticationFailed() {
                // Ignore and allow fallback to continue
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(prompt.title)
            .setSubtitle(prompt.subtitle)
            .setDescription(prompt.description)
            .setAllowedAuthenticators(authenticators)
            .apply {
                if (!factor.allowDeviceCredentialFallback) {
                    setNegativeButtonText(prompt.negativeButton ?: "Cancel")
                }
            }
            .build()

        biometricPrompt.authenticate(promptInfo)

        continuation.invokeOnCancellation { biometricPrompt.cancelAuthentication() }
    }

    private fun authenticators(factor: SecureAuthFactor.Biometric): Int {
        return if (factor.allowDeviceCredentialFallback) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG
        }
    }
}
