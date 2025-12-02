package love.bside.app.security

class AndroidPasskeyBridge : PasskeyBridge {
    override suspend fun availability(): PasskeyAvailability =
        PasskeyAvailability.Unavailable("Passkeys not implemented yet")
}
