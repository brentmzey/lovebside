package love.bside.app.security

import love.bside.app.security.storage.IosSecureCredentialStore

actual class SecureCredentialStoreFactory {
    actual fun create(): SecureCredentialStore = IosSecureCredentialStore()
}
