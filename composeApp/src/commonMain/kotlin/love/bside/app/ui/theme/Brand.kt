package love.bside.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Centralized palette + typography tokens so every platform (Android, iOS, Desktop, Web)
 * renders the same brand look that matches the B-Side logo (teal tile + deep-purple heart).
 */
object BsideBrand {
    // Icon / logo pairing colors (sampled from bside-logo.png)
    val TealTile = Color(0xFF6ACAC4)
    val TealTileLight = Color(0xFFAEE5E1)
    val TealTileDark = Color(0xFF2F8A85)

    val PlumHeart = Color(0xFF4B164C)
    val PlumHeartLight = Color(0xFF7A3A7D)
    val PlumHeartDark = Color(0xFF2D0630)

    // Supporting neutrals
    val MistyTeal = Color(0xFFE4F4F2)
    val Linen = Color(0xFFF9F7F4)
    val Charcoal = Color(0xFF11131A)
    val OutlineMint = Color(0xFF7FB8B3)

    // Accent tones for states/illustrations
    val CoralGlow = Color(0xFFFF8A82)
    val SoftLilac = Color(0xFFE6D8EE)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFB04C)
    val Error = Color(0xFFD6455D)

    // Typography (placeholder until custom font files arrive)
    val DisplayFont: FontFamily = FontFamily.SansSerif
    val BodyFont: FontFamily = FontFamily.SansSerif
}
