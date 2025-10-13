package love.bside.app

import androidx.compose.runtime.Composable
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.Children
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation.stackAnimation
import love.bside.app.routing.RootComponent
import love.bside.app.ui.theme.BsideTheme

@Composable
fun App(root: RootComponent) {
    BsideTheme {
        Children(
            stack = root.childStack,
            animation = stackAnimation(slide())
        ) {
            when (val child = it.instance) {
                is RootComponent.Child.Login -> LoginScreen(child.component)
                is RootComponent.Child.Main -> MainScreen(child.component)
            }
        }
    }
}