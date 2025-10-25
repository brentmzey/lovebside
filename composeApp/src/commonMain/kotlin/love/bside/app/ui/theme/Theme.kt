package love.bside.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// bside.app Brand Colors - Refined for dating app aesthetic
// Primary: Warm, inviting rose/pink - represents connection and romance
private val BsidePrimary = Color(0xFFE91E63) // Material Pink 500 - vibrant, friendly
private val BsidePrimaryLight = Color(0xFFF48FB1) // Lighter for containers
private val BsidePrimaryDark = Color(0xFFC2185B) // Darker for depth

// Secondary: Deep purple - represents depth, thoughtfulness (Proust vibe)
private val BsideSecondary = Color(0xFF6200EA) // Deep Purple A700 - sophisticated
private val BsideSecondaryLight = Color(0xFFB388FF) // Lighter variant
private val BsideSecondaryDark = Color(0xFF4A00B8) // Darker variant

// Tertiary: Coral/Peachy - warm accent for highlights
private val BsideTertiary = Color(0xFFFF7043) // Deep Orange 400 - energetic, warm

// Semantic Colors
private val BsideSuccess = Color(0xFF4CAF50) // Green for matches, success states
private val BsideWarning = Color(0xFFFF9800) // Orange for warnings
private val BsideError = Color(0xFFF44336) // Red for errors

// Neutral Grays
private val BsideGray50 = Color(0xFFFAFAFA)
private val BsideGray100 = Color(0xFFF5F5F5)
private val BsideGray200 = Color(0xFFEEEEEE)
private val BsideGray700 = Color(0xFF616161)
private val BsideGray900 = Color(0xFF212121)

// Light Theme Colors - Clean, modern dating app aesthetic
private val LightColorScheme = lightColorScheme(
    primary = BsidePrimary,
    onPrimary = Color.White,
    primaryContainer = BsidePrimaryLight,
    onPrimaryContainer = BsidePrimaryDark,
    
    secondary = BsideSecondary,
    onSecondary = Color.White,
    secondaryContainer = BsideSecondaryLight,
    onSecondaryContainer = BsideSecondaryDark,
    
    tertiary = BsideTertiary,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFCCBC), // Light coral
    onTertiaryContainer = Color(0xFFBF360C), // Dark coral
    
    background = BsideGray50, // Soft, warm background
    onBackground = BsideGray900,
    surface = Color.White,
    onSurface = BsideGray900,
    surfaceVariant = BsideGray100,
    onSurfaceVariant = BsideGray700,
    
    error = BsideError,
    onError = Color.White,
    errorContainer = Color(0xFFFFCDD2),
    onErrorContainer = Color(0xFFC62828),
    
    outline = BsideGray200,
    outlineVariant = BsideGray100,
)

// Dark Theme Colors - Sophisticated night mode for late-night browsing
private val DarkColorScheme = darkColorScheme(
    primary = BsidePrimaryLight,
    onPrimary = BsidePrimaryDark,
    primaryContainer = BsidePrimaryDark,
    onPrimaryContainer = BsidePrimaryLight,
    
    secondary = BsideSecondaryLight,
    onSecondary = BsideSecondaryDark,
    secondaryContainer = BsideSecondaryDark,
    onSecondaryContainer = BsideSecondaryLight,
    
    tertiary = Color(0xFFFFAB91), // Light coral for dark mode
    onTertiary = Color(0xFFBF360C),
    tertiaryContainer = Color(0xFFD84315),
    onTertiaryContainer = Color(0xFFFFCCBC),
    
    background = Color(0xFF121212), // True dark for OLED
    onBackground = Color(0xFFE1E1E1),
    surface = Color(0xFF1E1E1E), // Slightly elevated dark
    onSurface = Color(0xFFE1E1E1),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFB0B0B0),
    
    error = Color(0xFFEF5350),
    onError = Color(0xFF1A0000),
    errorContainer = Color(0xFFC62828),
    onErrorContainer = Color(0xFFFFCDD2),
    
    outline = Color(0xFF404040),
    outlineVariant = Color(0xFF2C2C2C),
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
