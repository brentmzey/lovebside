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
    
    // === Primary Colors (Purple/Plum from Figma) ===
    val Primary = Color(0xFF6B4FA0)          // Deep purple
    val PrimaryVariant = Color(0xFF4A1D4D)   // Dark plum (from Login button)
    val OnPrimary = Color(0xFFFFFFFF)        // White text on primary
    
    // === Secondary Colors (Pink/Rose) ===
    val Secondary = Color(0xFFE91E63)        // Vibrant pink
    val SecondaryVariant = Color(0xFFF06292) // Light pink (chat icon)
    val OnSecondary = Color(0xFFFFFFFF)      // White text on secondary
    
    // === Tertiary Colors (Teal) ===
    val Teal = Color(0xFF4DB6AC)             // Teal accent
    val TealDark = Color(0xFF00897B)         // Dark teal
    val TealLight = Color(0xFF80CBC4)        // Light teal
    
    // === Background Colors ===
    val Background = Color(0xFFFAF8FC)       // Light lavender background
    val BackgroundDark = Color(0xFF1A1A1A)   // Dark mode background  
    val Surface = Color(0xFFFFFFFF)          // White surface
    val SurfaceDark = Color(0xFF2C2C2C)      // Dark mode surface
    
    // === Text Colors ===
    val TextPrimary = Color(0xFF212121)      // Almost black
    val TextSecondary = Color(0xFF757575)    // Gray
    val TextTertiary = Color(0xFFBDBDBD)     // Light gray
    val TextOnDark = Color(0xFFFFFFFF)       // White on dark
    
    // === Message Bubble Colors ===
    val MessageSent = Color(0xFF6B4FA0)      // Primary purple for sent
    val MessageReceived = Color(0xFFF5F5F5)  // Light gray for received
    val MessageSentGradientStart = Color(0xFF6B4FA0)
    val MessageSentGradientEnd = Color(0xFF8E6CB8)
    
    // === Semantic Colors ===
    val Success = Color(0xFF4CAF50)          // Green
    val Error = Color(0xFFF44336)            // Red
    val Warning = Color(0xFFFF9800)          // Orange
    val Info = Color(0xFF2196F3)             // Blue
    
    // === Neutral Palette ===
    val Neutral50 = Color(0xFFFAFAFA)
    val Neutral100 = Color(0xFFF5F5F5)
    val Neutral200 = Color(0xFFEEEEEE)
    val Neutral300 = Color(0xFFE0E0E0)
    val Neutral400 = Color(0xFFBDBDBD)
    val Neutral500 = Color(0xFF9E9E9E)
    val Neutral600 = Color(0xFF757575)
    val Neutral700 = Color(0xFF616161)
    val Neutral800 = Color(0xFF424242)
    val Neutral900 = Color(0xFF212121)
    
    // === Dividers & Borders ===
    val Divider = Color(0x1F000000)          // 12% black
    val Border = Color(0x33000000)           // 20% black
    
    // === Overlay Colors ===
    val Scrim = Color(0x99000000)            // 60% black overlay
    val Shimmer = Color(0x33FFFFFF)          // Shimmer effect
    
    // === Status Colors ===
    val Online = Color(0xFF4CAF50)           // Green
    val Offline = Color(0xFF9E9E9E)          // Gray
    val Typing = Color(0xFF2196F3)           // Blue
    
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
