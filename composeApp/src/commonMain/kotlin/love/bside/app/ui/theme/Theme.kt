package love.bside.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BsideBrand.PlumHeart,
    onPrimary = Color.White,
    primaryContainer = BsideBrand.PlumHeartLight,
    onPrimaryContainer = Color.White,

    secondary = BsideBrand.PlumHeart,
    onSecondary = Color.White,
    secondaryContainer = BsideBrand.TealTile,
    onSecondaryContainer = BsideBrand.PlumHeartDark,

    tertiary = BsideBrand.TealTileDark,
    onTertiary = Color.White,
    tertiaryContainer = BsideBrand.TealTileLight,
    onTertiaryContainer = BsideBrand.PlumHeartDark,

    background = BsideBrand.TealTileLight,
    onBackground = BsideBrand.PlumHeartDark,
    surface = BsideBrand.Linen,
    onSurface = BsideBrand.PlumHeart,
    surfaceVariant = BsideBrand.MistyTeal,
    onSurfaceVariant = BsideBrand.PlumHeart,

    error = BsideBrand.Error,
    onError = Color.White,
    errorContainer = Color(0xFFFFD5DD),
    onErrorContainer = BsideBrand.Error,

    outline = BsideBrand.OutlineMint,
    outlineVariant = BsideBrand.MistyTeal,
    inversePrimary = BsideBrand.TealTileDark
)

private val DarkColorScheme = darkColorScheme(
    primary = BsideBrand.TealTile,
    onPrimary = BsideBrand.PlumHeartDark,
    primaryContainer = BsideBrand.TealTileDark,
    onPrimaryContainer = Color.White,

    secondary = BsideBrand.PlumHeartLight,
    onSecondary = Color.White,
    secondaryContainer = BsideBrand.PlumHeartDark,
    onSecondaryContainer = BsideBrand.TealTileLight,

    tertiary = BsideBrand.CoralGlow,
    onTertiary = Color.White,
    tertiaryContainer = BsideBrand.PlumHeartDark,
    onTertiaryContainer = BsideBrand.TealTileLight,

    background = BsideBrand.Charcoal,
    onBackground = Color(0xFFE8E1F0),
    surface = BsideBrand.PlumHeartDark,
    onSurface = Color(0xFFF5EAF4),
    surfaceVariant = Color(0xFF26122B),
    onSurfaceVariant = BsideBrand.TealTileLight,

    error = BsideBrand.Error,
    onError = Color.White,
    errorContainer = Color(0xFF7A1D2E),
    onErrorContainer = Color(0xFFFFEDEF),

    outline = Color(0xFF56445D),
    outlineVariant = Color(0xFF39253E),
    inversePrimary = BsideBrand.TealTileLight
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

@Composable
fun BsideBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val backdrop = remember(darkTheme) {
        if (darkTheme) {
            Brush.linearGradient(
                colors = listOf(
                    BsideBrand.Charcoal,
                    BsideBrand.PlumHeartDark
                )
            )
        } else {
            Brush.linearGradient(
                colors = listOf(
                    BsideBrand.TealTileDark,
                    BsideBrand.TealTile,
                    BsideBrand.TealTileLight
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
    ) {
        content()
    }
}
