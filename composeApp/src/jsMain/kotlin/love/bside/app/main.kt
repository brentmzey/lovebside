package love.bside.app

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.StorageSettings
import kotlinx.browser.document
import kotlinx.browser.localStorage
import love.bside.app.di.appModule
import love.bside.app.routing.RootComponent
import love.bside.app.security.SecureCredentialStoreFactory
import love.bside.app.security.UnsupportedBiometricPromptBridge
import love.bside.app.security.UnsupportedPasskeyBridge
import love.bside.app.security.di.secureAuthModule
import org.koin.core.context.startKoin

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val settings = StorageSettings(localStorage)
    val koin = startKoin {
        modules(
            appModule(settings),
            secureAuthModule(
                credentialStore = SecureCredentialStoreFactory().create(),
                biometricPromptBridge = UnsupportedBiometricPromptBridge(),
                passkeyBridge = UnsupportedPasskeyBridge()
            )
        )
    }.koin
    
    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )
    val appDependencies = buildAppDependencies(koin)
    
    ComposeViewport(document.getElementById("root")!!) {
        App(rootComponent, appDependencies)
    }
}