package love.bside.app.ui.responsive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Enterprise responsive design system
 */

object Breakpoints {
    val Compact = 0.dp..599.dp
    val Medium = 600.dp..839.dp
    val Expanded = 840.dp..1199.dp
    val Large = 1200.dp..1599.dp
    val ExtraLarge = 1600.dp..9999.dp
}

enum class WindowSizeClass {
    COMPACT, MEDIUM, EXPANDED, LARGE, EXTRA_LARGE;
    
    companion object {
        fun fromWidth(width: Dp): WindowSizeClass {
            return when {
                width in Breakpoints.Compact -> COMPACT
                width in Breakpoints.Medium -> MEDIUM
                width in Breakpoints.Expanded -> EXPANDED
                width in Breakpoints.Large -> LARGE
                else -> EXTRA_LARGE
            }
        }
    }
}

class ResponsiveValue<T>(
    val compact: T,
    val medium: T = compact,
    val expanded: T = medium,
    val large: T = expanded,
    val extraLarge: T = large
) {
    fun getValue(sizeClass: WindowSizeClass): T {
        return when (sizeClass) {
            WindowSizeClass.COMPACT -> compact
            WindowSizeClass.MEDIUM -> medium
            WindowSizeClass.EXPANDED -> expanded
            WindowSizeClass.LARGE -> large
            WindowSizeClass.EXTRA_LARGE -> extraLarge
        }
    }
}

fun <T> responsiveValue(
    compact: T,
    medium: T = compact,
    expanded: T = medium,
    large: T = expanded,
    extraLarge: T = large
) = ResponsiveValue(compact, medium, expanded, large, extraLarge)

object ResponsiveSize {
    val buttonHeight = responsiveValue(compact = 48.dp, medium = 52.dp, expanded = 56.dp)
    val cardPadding = responsiveValue(compact = 12.dp, medium = 16.dp, expanded = 24.dp)
    val iconMedium = responsiveValue(compact = 24.dp, medium = 28.dp, expanded = 32.dp)
    val avatarMedium = responsiveValue(compact = 48.dp, medium = 56.dp, expanded = 64.dp)
    val maxContentWidth = responsiveValue(compact = 600.dp, medium = 840.dp, expanded = 1200.dp, large = 1600.dp)
    val gridColumns = responsiveValue(compact = 1, medium = 2, expanded = 3, large = 4, extraLarge = 6)
}

@Composable
fun <T> ResponsiveValue<T>.current(width: Dp): T {
    val sizeClass = remember(width) { WindowSizeClass.fromWidth(width) }
    return getValue(sizeClass)
}
