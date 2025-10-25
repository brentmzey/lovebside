package love.bside.app

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    val windowState = rememberWindowState(
        width = 375.dp,
        height = 812.dp
    )
    
    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "B-Side Dating App"
    ) {
        App()
    }
}