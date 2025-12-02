package love.bside.app

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import love.bside.app.di.appModule
import love.bside.app.security.ApplePasskeyBridge
import love.bside.app.security.IosBiometricPromptBridge
import love.bside.app.security.SecureCredentialStoreFactory
import love.bside.app.security.di.secureAuthModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import platform.Foundation.NSUserDefaults

fun initKoin(): KoinApplication {
    val settings: Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    val secureStore = SecureCredentialStoreFactory().create()
    val biometricBridge = IosBiometricPromptBridge()
    val passkeyBridge = ApplePasskeyBridge()

    return startKoin {
        modules(
            appModule(settings),
            secureAuthModule(secureStore, biometricBridge, passkeyBridge)
        )
    }
}
