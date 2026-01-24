package love.bside.app.ui.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Responsive modifiers for platform-adaptive UI following Apple HIG
 * 
 * Ensures elements don't stretch awkwardly on large screens (desktop/web)
 * while maintaining full-width behavior on mobile when appropriate
 */

/**
 * Maximum widths for different content types
 */
object ResponsiveWidth {
    val FormMaxWidth: Dp = 480.dp      // Login, signup forms (matches Apple standards)
    val ContentMaxWidth: Dp = 800.dp   // Main content areas
    val WideMaxWidth: Dp = 1200.dp     // Full-width sections
    val Compact: Dp = 360.dp           // Minimal forms
}

/**
 * Apply responsive width for form elements (buttons, inputs)
 * 
 * - Mobile: Full width
 * - Desktop/Web: Max 480dp, centered
 * 
 * Usage:
 * ```
 * BsideButton(
 *     text = "Login",
 *     onClick = {},
 *     modifier = Modifier.responsiveFormWidth()
 * )
 * ```
 */
@Composable
fun Modifier.responsiveFormWidth(): Modifier {
    return if (isMobilePlatform()) {
        this.fillMaxWidth()
    } else {
        this.widthIn(max = ResponsiveWidth.FormMaxWidth)
    }
}

/**
 * Apply responsive width for content areas
 */
@Composable
fun Modifier.responsiveContentWidth(): Modifier {
    return if (isMobilePlatform()) {
        this.fillMaxWidth()
    } else {
        this.widthIn(max = ResponsiveWidth.ContentMaxWidth)
    }
}

/**
 * Apply responsive width for wide sections
 */
@Composable
fun Modifier.responsiveWideWidth(): Modifier {
    return if (isMobilePlatform()) {
        this.fillMaxWidth()
    } else {
        this.widthIn(max = ResponsiveWidth.WideMaxWidth)
    }
}

/**
 * Platform detection helpers (expect/actual pattern)
 */
@Composable
expect fun isDesktopPlatform(): Boolean

@Composable
expect fun isWebPlatform(): Boolean

@Composable
expect fun isMobilePlatform(): Boolean
