package love.bside.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import love.bside.app.di.appModule
import love.bside.app.routing.RootComponent
import org.koin.core.context.startKoin
import java.util.prefs.Preferences

fun main() {
    val settings: Settings = PreferencesSettings(
        Preferences.userRoot().node("love.bside.app")
    )
    
    val koin = startKoin {
        modules(appModule(settings))
    }.koin

    val lifecycle = LifecycleRegistry()
    val root = RootComponent(DefaultComponentContext(lifecycle), koin.get())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "B-Side App",
        ) {
            App(root)
        }
    }
}