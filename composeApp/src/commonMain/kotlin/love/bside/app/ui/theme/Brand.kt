package love.bside.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

/**
 * Centralized palette + typography tokens so every platform (Android, iOS, Desktop, Web)
 * renders the same brand look that matches the B-Side logo (teal tile + deep-purple heart).
 */
object BsideBrand {
    // Icon / logo pairing colors
    val MintTile = Color(0xFFA7D8D1)
    val MintTileDark = Color(0xFF5F8680)

    val PlumHeart = Color(0xFF5A2F55)
    val PlumHeartDark = Color(0xFF2B102C)
    val PlumHeartLight = Color(0xFF8D5C89)

    // Supporting neutrals
    val LavenderMist = Color(0xFFF4EAF4)
    val OffWhite = Color(0xFFFCFAF8)
    val DeepEggplant = Color(0xFF1F0F20)
    val OutlineLavender = Color(0xFFD9CEDA)

    // Semantic
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFFB04C)
    val Error = Color(0xFFD6455D)

    // Typography (placeholder until design delivers custom font files)
    val DisplayFont: FontFamily = FontFamily.SansSerif
    val BodyFont: FontFamily = FontFamily.SansSerif
}
