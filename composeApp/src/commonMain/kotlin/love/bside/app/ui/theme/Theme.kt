package love.bside.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BsideBrand.PlumHeart,
    onPrimary = Color.White,
    primaryContainer = BsideBrand.PlumHeartLight,
    onPrimaryContainer = Color.White,

    secondary = BsideBrand.PlumHeart,
    onSecondary = Color.White,
    secondaryContainer = BsideBrand.MintTile,
    onSecondaryContainer = BsideBrand.DeepEggplant,

    tertiary = BsideBrand.LavenderMist,
    onTertiary = BsideBrand.PlumHeartDark,
    tertiaryContainer = BsideBrand.OffWhite,
    onTertiaryContainer = BsideBrand.PlumHeartDark,

    background = BsideBrand.MintTile,
    onBackground = BsideBrand.DeepEggplant,
    surface = BsideBrand.OffWhite,
    onSurface = BsideBrand.PlumHeartDark,
    surfaceVariant = BsideBrand.LavenderMist,
    onSurfaceVariant = BsideBrand.PlumHeart,

    error = BsideBrand.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFD5DD),
    onErrorContainer = BsideBrand.Error,

    outline = BsideBrand.OutlineLavender,
    outlineVariant = BsideBrand.LavenderMist,
    inversePrimary = BsideBrand.MintTileDark
)

private val DarkColorScheme = darkColorScheme(
    primary = BsideBrand.PlumHeartLight,
    onPrimary = Color.White,
    primaryContainer = BsideBrand.PlumHeartDark,
    onPrimaryContainer = BsideBrand.MintTile,

    secondary = BsideBrand.PlumHeart,
    onSecondary = Color.White,
    secondaryContainer = BsideBrand.PlumHeartDark,
    onSecondaryContainer = BsideBrand.MintTile,

    tertiary = BsideBrand.MintTile,
    onTertiary = BsideBrand.PlumHeartDark,
    tertiaryContainer = BsideBrand.MintTileDark,
    onTertiaryContainer = Color.White,

    background = Color(0xFF120713),
    onBackground = Color(0xFFF8F4F8),
    surface = BsideBrand.PlumHeartDark,
    onSurface = Color(0xFFF4EAF4),
    surfaceVariant = Color(0xFF2F1A32),
    onSurfaceVariant = BsideBrand.MintTile,

    error = BsideBrand.Error,
    onError = Color.White,
    errorContainer = Color(0xFF7A1D2E),
    onErrorContainer = Color(0xFFFFEDEF),

    outline = Color(0xFF4B2B4E),
    outlineVariant = Color(0xFF3A1C3A),
    inversePrimary = BsideBrand.MintTile
)

@Composable
fun BsideTheme(
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
