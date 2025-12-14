package love.bside.app

import android.app.Application
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import love.bside.app.di.appModule
import love.bside.app.security.AndroidBiometricPromptBridge
import love.bside.app.security.AndroidPasskeyBridge
import love.bside.app.security.CurrentActivityHolder
import love.bside.app.security.SecureCredentialStoreFactory
import love.bside.app.security.di.secureAuthModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BsideApp : Application() {
    private val activityHolder = CurrentActivityHolder()

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(activityHolder)

        val settings: Settings = SharedPreferencesSettings(
            getSharedPreferences("bside_prefs", MODE_PRIVATE)
        )
        val secureStore = SecureCredentialStoreFactory(this).create()
        val biometricBridge = AndroidBiometricPromptBridge(this, activityHolder)
        val passkeyBridge = AndroidPasskeyBridge()

        startKoin {
            androidContext(this@BsideApp)
            modules(
                appModule(settings),
                secureAuthModule(secureStore, biometricBridge, passkeyBridge),
                love.bside.app.di.uiModule
            )
        }
    }
}
