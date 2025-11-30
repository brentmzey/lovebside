package love.bside.app

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import love.bside.app.routing.RootComponent
import org.koin.core.Koin

@Suppress("UNUSED_PARAMETER")
fun MainViewController(koin: Koin) = ComposeUIViewController {
    val lifecycle = LifecycleRegistry()
    val rootComponent = RootComponent(
        componentContext = DefaultComponentContext(lifecycle)
    )
    val appDependencies = remember(koin) { buildAppDependencies(koin) }
    App(rootComponent, appDependencies)
}