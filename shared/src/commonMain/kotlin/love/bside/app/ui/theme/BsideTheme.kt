package love.bside.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun BsideTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (!useDarkTheme) {
        BsideLightColors
    } else {
        BsideDarkColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}
