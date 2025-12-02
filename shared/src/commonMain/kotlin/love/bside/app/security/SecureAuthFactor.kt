package love.bside.app.security

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SecureAuthFactor {
    @Serializable
    @SerialName("biometric")
    data class Biometric(
        val strength: Strength = Strength.STRONG,
        val allowDeviceCredentialFallback: Boolean = true
    ) : SecureAuthFactor {
        @Serializable
        enum class Strength { STRONG, WEAK }
    }

    @Serializable
    @SerialName("passkey")
    data class Passkey(
        val relyingPartyId: String,
        val userName: String
    ) : SecureAuthFactor
}

sealed interface BiometricAvailability {
    data object Unknown : BiometricAvailability
    data object Available : BiometricAvailability
    data object NotEnrolled : BiometricAvailability
    data object NoHardware : BiometricAvailability
    data class Unavailable(val reason: String) : BiometricAvailability
}
