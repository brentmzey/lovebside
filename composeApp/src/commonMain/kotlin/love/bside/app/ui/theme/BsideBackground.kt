package love.bside.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import love.bside.app.ui.design.tokens.BsideColors

@Composable
fun BsideBackground(
    content: @Composable () -> Unit
) {
    val (backgroundColor, blurColor) = if (isSystemInDarkTheme()) {
        BsideColors.BackgroundDark to BsideColors.GlassyBackgroundDark
    } else {
        BsideColors.Background to BsideColors.GlassyBackground
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // This is a simple implementation of a glassy background.
        // A more advanced version might use a blurred image or a more complex gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 70.dp)
                .background(blurColor)
        )
        content()
    }
}
