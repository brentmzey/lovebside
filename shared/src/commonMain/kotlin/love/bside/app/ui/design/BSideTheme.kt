package love.bside.app.ui.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import love.bside.app.ui.design.tokens.BsideColors
import love.bside.app.ui.design.tokens.BsideShapes
import love.bside.app.ui.design.tokens.BsideTypography

/**
 * BSide Theme following Apple Human Interface Guidelines
 * 
 * Principles:
 * - Clarity: Text legible at every size, icons precise
 * - Deference: Content-first, minimal UI
 * - Depth: Visual layers, realistic motion
 */

private val LightColorScheme = lightColorScheme(
    primary = BsideColors.Primary,
    onPrimary = BsideColors.OnPrimary,
    primaryContainer = BsideColors.PrimaryVariant,
    onPrimaryContainer = BsideColors.OnPrimary,
    
    secondary = BsideColors.Secondary,
    onSecondary = BsideColors.OnSecondary,
    secondaryContainer = BsideColors.SecondaryVariant,
    onSecondaryContainer = BsideColors.OnSecondary,
    
    tertiary = BsideColors.Teal,
    onTertiary = BsideColors.OnPrimary,
    tertiaryContainer = BsideColors.TealLight,
    onTertiaryContainer = BsideColors.TealDark,
    
    background = BsideColors.Background,
    onBackground = BsideColors.TextPrimary,
    
    surface = BsideColors.GlassySurface,
    onSurface = BsideColors.TextPrimary,
    surfaceVariant = BsideColors.GlassySurface,
    onSurfaceVariant = BsideColors.TextSecondary,
    
    error = BsideColors.Error,
    onError = BsideColors.OnPrimary,
    
    outline = BsideColors.Border,
    outlineVariant = BsideColors.Divider,
    
    scrim = BsideColors.Scrim
)

private val DarkColorScheme = darkColorScheme(
    primary = BsideColors.Primary,
    onPrimary = BsideColors.OnPrimary,
    primaryContainer = BsideColors.PrimaryVariant,
    onPrimaryContainer = BsideColors.OnPrimary,
    
    secondary = BsideColors.Secondary,
    onSecondary = BsideColors.OnSecondary,
    secondaryContainer = BsideColors.SecondaryVariant,
    onSecondaryContainer = BsideColors.OnSecondary,
    
    tertiary = BsideColors.Teal,
    onTertiary = BsideColors.OnPrimary,
    tertiaryContainer = BsideColors.TealDark,
    onTertiaryContainer = BsideColors.TealLight,
    
    background = BsideColors.BackgroundDark,
    onBackground = BsideColors.TextOnDark,
    
    surface = BsideColors.GlassySurfaceDark,
    onSurface = BsideColors.TextOnDark,
    surfaceVariant = BsideColors.GlassySurfaceDark,
    onSurfaceVariant = BsideColors.Neutral400,
    
    error = BsideColors.Error,
    onError = BsideColors.OnPrimary,
    
    outline = BsideColors.Border,
    outlineVariant = BsideColors.Divider,
    
    scrim = BsideColors.Scrim
)

private val BSideTypographyM3 = Typography(
    displayLarge = BsideTypography.DisplayLarge,
    displayMedium = BsideTypography.DisplayMedium,
    displaySmall = BsideTypography.DisplaySmall,
    headlineLarge = BsideTypography.HeadlineLarge,
    headlineMedium = BsideTypography.HeadlineMedium,
    headlineSmall = BsideTypography.HeadlineSmall,
    titleLarge = BsideTypography.TitleLarge,
    titleMedium = BsideTypography.TitleMedium,
    titleSmall = BsideTypography.TitleSmall,
    bodyLarge = BsideTypography.BodyLarge,
    bodyMedium = BsideTypography.BodyMedium,
    bodySmall = BsideTypography.BodySmall,
    labelLarge = BsideTypography.LabelLarge,
    labelMedium = BsideTypography.LabelMedium,
    labelSmall = BsideTypography.LabelSmall
)

private val BSideShapesM3 = Shapes(
    extraSmall = BsideShapes.ExtraSmall,
    small = BsideShapes.Small,
    medium = BsideShapes.Medium,
    large = BsideShapes.Large,
    extraLarge = BsideShapes.ExtraLarge
)

@Composable
fun BSideTheme(
    isDarkMode: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkMode) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = BSideTypographyM3,
        shapes = BSideShapesM3,
        content = content
    )
}
