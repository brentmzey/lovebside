package love.bside.app

import androidx.compose.ui.window.ComposeUIViewController
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import love.bside.app.routing.RootComponent
import org.koin.core.Koin

fun MainViewController(koin: Koin) = ComposeUIViewController { App(RootComponent(DefaultComponentContext(LifecycleRegistry()), koin.get())) }