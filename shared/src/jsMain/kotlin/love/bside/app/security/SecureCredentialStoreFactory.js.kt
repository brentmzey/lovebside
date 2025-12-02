package love.bside.app.security

actual class SecureCredentialStoreFactory {
    actual fun create(): SecureCredentialStore = InMemorySecureCredentialStore()
}
