package love.bside.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand Colors - Bside Identity
object BsideColors {
    // Primary - Warm, inviting coral/pink
    val Primary = Color(0xFFFF6B9D)
    val PrimaryVariant = Color(0xFFFF8FB3)
    val PrimaryDark = Color(0xFFE5527B)
    
    // Secondary - Complementary purple
    val Secondary = Color(0xFF9B6BFF)
    val SecondaryVariant = Color(0xFFB38FFF)
    val SecondaryDark = Color(0xFF7F52E5)
    
    // Background
    val BackgroundLight = Color(0xFFFFFBFE)
    val BackgroundDark = Color(0xFF1C1B1F)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceDark = Color(0xFF2B2930)
    
    // Text
    val TextPrimaryLight = Color(0xFF1C1B1F)
    val TextPrimaryDark = Color(0xFFE6E1E5)
    val TextSecondaryLight = Color(0xFF49454F)
    val TextSecondaryDark = Color(0xFFCAC4D0)
    
    // Status
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFEF5350)
    val Info = Color(0xFF2196F3)
    
    // Online status
    val OnlineGreen = Color(0xFF4CAF50)
    val AwayYellow = Color(0xFFFFEB3B)
    val OfflineGray = Color(0xFF9E9E9E)
}

private val LightColorScheme = lightColorScheme(
    primary = BsideColors.Primary,
    onPrimary = Color.White,
    primaryContainer = BsideColors.PrimaryVariant,
    onPrimaryContainer = BsideColors.PrimaryDark,
    
    secondary = BsideColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = BsideColors.SecondaryVariant,
    onSecondaryContainer = BsideColors.SecondaryDark,
    
    background = BsideColors.BackgroundLight,
    onBackground = BsideColors.TextPrimaryLight,
    surface = BsideColors.SurfaceLight,
    onSurface = BsideColors.TextPrimaryLight,
    
    error = BsideColors.Error,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = BsideColors.Primary,
    onPrimary = Color.White,
    primaryContainer = BsideColors.PrimaryDark,
    onPrimaryContainer = BsideColors.PrimaryVariant,
    
    secondary = BsideColors.Secondary,
    onSecondary = Color.White,
    secondaryContainer = BsideColors.SecondaryDark,
    onSecondaryContainer = BsideColors.SecondaryVariant,
    
    background = BsideColors.BackgroundDark,
    onBackground = BsideColors.TextPrimaryDark,
    surface = BsideColors.SurfaceDark,
    onSurface = BsideColors.TextPrimaryDark,
    
    error = BsideColors.Error,
    onError = Color.White,
)

@Composable
fun BsideModernTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
