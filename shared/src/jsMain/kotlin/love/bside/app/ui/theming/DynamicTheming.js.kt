package love.bside.app.ui.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getDynamicLightColors(): ColorScheme {
    return lightColorScheme()
}

@Composable
actual fun getDynamicDarkColors(): ColorScheme {
    return darkColorScheme()
}
