package love.bside.app.security

import android.content.Context
import love.bside.app.security.storage.AndroidSecureCredentialStore

actual class SecureCredentialStoreFactory(private val context: Context) {
    actual fun create(): SecureCredentialStore = AndroidSecureCredentialStore(context)
}
