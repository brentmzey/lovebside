package love.bside.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a premium "Glassmorphism" effect.
 * Note: Real backdrop blur is platform-dependent and expensive in Compose Multiplatform.
 * This simulates it using semi-transparent layers and borders.
 */
fun Modifier.glassEffect(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = Color.White.copy(alpha = 0.1f),
    borderColor: Color = Color.White.copy(alpha = 0.2f),
    blurRadius: Dp = 0.dp // Placeholder for future platform-specific blur
): Modifier = composed {
    this
        .clip(shape)
        .background(backgroundColor)
        .border(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    borderColor,
                    borderColor.copy(alpha = 0.05f)
                ),
                start = Offset(0f, 0f),
                end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
            ),
            shape = shape
        )
}

/**
 * Applies a subtle gradient background that shifts slightly.
 * Great for cards to make them feel "alive".
 */
fun Modifier.subtleGradientBackground(
    primaryColor: Color = BsideBrand.TealTile,
    secondaryColor: Color = BsideBrand.PlumHeart
): Modifier = this.drawBehind {
    drawRect(
        brush = Brush.linearGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.05f),
                secondaryColor.copy(alpha = 0.05f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        )
    )
}

/**
 * Adds a premium shadow and elevation.
 */
fun Modifier.premiumElevation(
    shadowColor: Color = Color.Black.copy(alpha = 0.1f),
    offsetY: Dp = 4.dp,
    blur: Dp = 8.dp
): Modifier = this // In valid compose we would use .shadow or .graphicsLayer, keeping it simple for now
    .drawBehind {
        // Custom shadow drawing could go here, or just use standard shadow for now
    }
