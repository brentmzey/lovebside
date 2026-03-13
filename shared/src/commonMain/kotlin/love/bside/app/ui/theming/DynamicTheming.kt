package love.bside.app.ui.theming

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/**
 * Enterprise theming system with dynamic color support
 */

/**
 * Theme mode
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM,  // Follow system preference
    AUTO     // Light during day, dark at night
}

/**
 * Color palette with dynamic color support
 */
data class BSideColorPalette(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    
    // Custom semantic colors
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color,
    val info: Color,
    val onInfo: Color
) {
    fun toMaterialColorScheme(): ColorScheme {
        return ColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = primary,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surface,
            surfaceDim = surfaceVariant,
            surfaceContainer = surfaceVariant,
            surfaceContainerHigh = surfaceVariant,
            surfaceContainerHighest = surfaceVariant,
            surfaceContainerLow = surface,
            surfaceContainerLowest = surface
        )
    }
}

/**
 * Theme configuration
 */
data class BSideThemeConfig(
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColors: Boolean = true,  // Android 12+ dynamic colors
    val useHighContrast: Boolean = false,
    val fontSize: FontScale = FontScale.NORMAL,
    val cornerRadius: CornerStyle = CornerStyle.ROUNDED
)

enum class FontScale(val scale: Float) {
    SMALL(0.85f),
    NORMAL(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f)
}

enum class CornerStyle(val multiplier: Float) {
    SHARP(0f),
    SLIGHTLY_ROUNDED(0.5f),
    ROUNDED(1.0f),
    EXTRA_ROUNDED(1.5f)
}

/**
 * Dynamic theme provider
 */
@Composable
fun BSideDynamicTheme(
    config: BSideThemeConfig = BSideThemeConfig(),
    isDarkTheme: Boolean = config.mode == ThemeMode.DARK,
    dynamicColor: Boolean = config.useDynamicColors,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && isDarkTheme -> getDynamicDarkColors()
        dynamicColor && !isDarkTheme -> getDynamicLightColors()
        isDarkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

/**
 * Get dynamic colors based on platform
 */
@Composable
expect fun getDynamicLightColors(): ColorScheme

@Composable
expect fun getDynamicDarkColors(): ColorScheme

/**
 * CompositionLocal for theme config
 */
val LocalThemeConfig = staticCompositionLocalOf { BSideThemeConfig() }
