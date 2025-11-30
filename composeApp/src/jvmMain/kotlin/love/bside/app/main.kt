package love.bside.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import com.russhwolf.settings.PreferencesSettings
import love.bside.app.di.appModule
import love.bside.app.routing.RootComponent
import org.koin.core.context.startKoin
import java.util.prefs.Preferences

fun main() = application {
    val settings = PreferencesSettings(Preferences.userRoot())
    val koin = startKoin {
        modules(appModule(settings))
    }.koin
    
    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )
    val appDependencies = buildAppDependencies(koin)
    
    val windowState = rememberWindowState(
        width = 375.dp,
        height = 812.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "B-Side Dating App"
    ) {
        App(rootComponent, appDependencies)
    }
}