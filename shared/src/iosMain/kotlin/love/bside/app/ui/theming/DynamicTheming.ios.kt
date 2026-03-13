package love.bside.app.ui.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun getDynamicLightColors(): ColorScheme {
    // iOS does not have Material You dynamic colors
    return lightColorScheme()
}

@Composable
actual fun getDynamicDarkColors(): ColorScheme {
    // iOS does not have Material You dynamic colors
    return darkColorScheme()
}
