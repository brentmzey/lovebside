package love.bside.app.security

class ApplePasskeyBridge : PasskeyBridge {
    override suspend fun availability(): PasskeyAvailability =
        PasskeyAvailability.Unavailable("Passkeys coming soon on Apple platforms")
}
