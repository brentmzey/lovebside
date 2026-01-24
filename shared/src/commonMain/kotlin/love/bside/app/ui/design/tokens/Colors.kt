package love.bside.app.ui.design.tokens

import androidx.compose.ui.graphics.Color

/**
 * BSide color palette extracted from Figma designs.
 * 
 * Design System: BSide Brand Colors
 * 
 * Color naming convention:
 * - Primary colors: Main brand identity
 * - Secondary colors: Accents and highlights
 * - Neutral: Backgrounds, surfaces, dividers
 * - Semantic: Success, error, warning, info
 */
object BsideColors {
    
    // === Primary Colors (Vibrant Purple/Blue) ===
    val Primary = Color(0xFF7A6FF0)          // A modern, vibrant purple
    val PrimaryVariant = Color(0xFF5D53D4)   // A slightly darker shade
    val OnPrimary = Color(0xFFFFFFFF)        // White text on primary
    
    // === Secondary Colors (Vibrant Pink) ===
    val Secondary = Color(0xFFF53C78)        // A modern, vibrant pink
    val SecondaryVariant = Color(0xFFD92C62) // A slightly darker shade
    val OnSecondary = Color(0xFFFFFFFF)      // White text on secondary
    
    // === Tertiary Colors (Vibrant Teal) ===
    val Teal = Color(0xFF2CEAA3)             // A modern, vibrant teal
    val TealDark = Color(0xFF1CB981)         // A slightly darker shade
    val TealLight = Color(0xFF6BFFC9)        // A lighter shade
    
    // === Background & Surface Colors (for Liquid Glass effect) ===
    val GlassyBackground = Color(0x99FFFFFF) // Semi-transparent white for light mode
    val GlassyBackgroundDark = Color(0x99000000) // Semi-transparent black for dark mode
    val GlassySurface = Color(0x4DFFFFFF)   // More transparent white for surfaces
    val GlassySurfaceDark = Color(0x4D1A1A1A) // More transparent dark for surfaces

    val Background = Color(0xFFF2F2F7)       // Apple-like light gray background
    val BackgroundDark = Color(0xFF000000)   // Pure black for dark mode (OLED)
    val Surface = Color(0xFFFFFFFF)          // White surface
    val SurfaceDark = Color(0xFF1C1C1E)      // Apple-like dark gray surface
    
    // === Text Colors ===
    val TextPrimary = Color(0xFF000000)      // Pure black for light mode
    val TextSecondary = Color(0x993C3C43)    // Apple-like secondary gray
    val TextTertiary = Color(0x4D3C3C43)     // Apple-like tertiary gray
    val TextOnDark = Color(0xFFFFFFFF)       // White on dark
    
    // === Message Bubble Colors ===
    val MessageSent = Primary
    val MessageReceived = Color(0xFFE5E5EA)  // Apple-like received message bubble
    val MessageSentGradientStart = Primary
    val MessageSentGradientEnd = Color(0xFF5D53D4)
    
    // === Semantic Colors ===
    val Success = Color(0xFF34C759)          // Apple-like green
    val Error = Color(0xFFFF3B30)            // Apple-like red
    val Warning = Color(0xFFFF9500)          // Apple-like orange
    val Info = Color(0xFF007AFF)             // Apple-like blue
    
    // === Neutral Palette ===
    val Neutral50 = Color(0xFFF2F2F7)
    val Neutral100 = Color(0xFFE5E5EA)
    val Neutral200 = Color(0xFFD1D1D6)
    val Neutral300 = Color(0xFFC7C7CC)
    val Neutral400 = Color(0xFFAEAEB2)
    val Neutral500 = Color(0xFF8E8E93)
    val Neutral600 = Color(0xFF636366)
    val Neutral700 = Color(0xFF48484A)
    val Neutral800 = Color(0xFF3A3A3C)
    val Neutral900 = Color(0xFF2C2C2E)
    
    // === Dividers & Borders ===
    val Divider = Color(0x203C3C43)          // Apple-like divider
    val Border = Color(0x303C3C43)           // Apple-like border
    
    // === Overlay Colors ===
    val Scrim = Color(0x99000000)            // 60% black overlay
    val Shimmer = Color(0x33FFFFFF)          // Shimmer effect
    
    // === Status Colors ===
    val Online = Color(0xFF34C759)           // Green
    val Offline = Color(0xFF8E8E93)          // Gray
    val Typing = Color(0xFF007AFF)           // Blue
    
    // === Pastel Colors (for avatars from Figma) ===
    val PastelPurple = Color(0xFFBA68C8)
    val PastelPink = Color(0xFFF48FB1)
    val PastelBlue = Color(0xFF7986CB)
    val PastelGreen = Color(0xFF81C784)
    val PastelRed = Color(0xFFE57373)
    val PastelYellow = Color(0xFFFFD54F)
    val PastelOrange = Color(0xFFFF8A65)
}

/**
 * Extension property for accessing colors
 */
val BsideColorScheme = BsideColors
