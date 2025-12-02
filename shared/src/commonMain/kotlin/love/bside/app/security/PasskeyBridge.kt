package love.bside.app.security

sealed interface PasskeyAvailability {
    data object Unknown : PasskeyAvailability
    data object Available : PasskeyAvailability
    data class Unavailable(val reason: String) : PasskeyAvailability
}

sealed interface PasskeyAuthResult {
    data object Success : PasskeyAuthResult
    data object Canceled : PasskeyAuthResult
    data class Error(val message: String) : PasskeyAuthResult
}

data class PasskeyRegistrationRequest(
    val relyingPartyId: String,
    val userName: String,
    val displayName: String
)

data class PasskeyAssertionRequest(
    val relyingPartyId: String,
    val userNameHint: String?
)

interface PasskeyBridge {
    suspend fun availability(): PasskeyAvailability = PasskeyAvailability.Unavailable("Not implemented")
    suspend fun register(request: PasskeyRegistrationRequest): PasskeyAuthResult =
        PasskeyAuthResult.Error("Registration not implemented")
    suspend fun authenticate(request: PasskeyAssertionRequest): PasskeyAuthResult =
        PasskeyAuthResult.Error("Authentication not implemented")
}

class UnsupportedPasskeyBridge : PasskeyBridge
