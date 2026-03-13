package love.bside.app.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Design System Tokens for B-Side
 * 
 * Centralized design tokens ensuring consistency across Android, iOS, Web, Desktop.
 * Based on 8dp grid system with golden ratio spacing progression.
 * 
 * Usage:
 * ```kotlin
 * Box(
 *     modifier = Modifier
 *         .padding(BSideTokens.Spacing.M)
 *         .clip(RoundedCornerShape(BSideTokens.Radius.M))
 * )
 * ```
 */
object BSideTokens {
    
    /**
     * Spacing Scale
     * Based on 8dp grid with golden ratio progression (8, 12, 16, 24, 32, 48, 64)
     */
    object Spacing {
        /** 4dp - Minimal spacing (chip padding, icon gaps) */
        val XXS: Dp = 4.dp
        
        /** 8dp - Tight spacing (button padding, list item gaps) */
        val XS: Dp = 8.dp
        
        /** 12dp - Compact spacing (card internal padding, form field gaps) */
        val S: Dp = 12.dp
        
        /** 16dp - Standard spacing (screen padding, section gaps) */
        val M: Dp = 16.dp
        
        /** 24dp - Generous spacing (major section separation) */
        val L: Dp = 24.dp
        
        /** 32dp - Extra spacing (screen top/bottom margins) */
        val XL: Dp = 32.dp
        
        /** 48dp - Hero spacing (landing sections, major divisions) */
        val XXL: Dp = 48.dp
        
        /** 64dp - Ultra spacing (full-bleed sections) */
        val XXXL: Dp = 64.dp
    }
    
    /**
     * Border Radius Scale
     * Consistent rounding for all interactive elements
     */
    object Radius {
        /** 4dp - Subtle rounding (chips, badges, small buttons) */
        val XS: Dp = 4.dp
        
        /** 8dp - Standard rounding (cards, buttons) */
        val S: Dp = 8.dp
        
        /** 12dp - Prominent rounding (modals, dialogs) */
        val M: Dp = 12.dp
        
        /** 16dp - Large rounding (major cards, hero elements) */
        val L: Dp = 16.dp
        
        /** 24dp - Extra rounding (full-screen sheets, special cards) */
        val XL: Dp = 24.dp
        
        /** 9999dp - Pill/circular shape (pills, avatars, FABs) */
        val Full: Dp = 9999.dp
    }
    
    /**
     * Elevation Scale
     * Consistent shadow depths for layering
     */
    object Elevation {
        /** 0dp - Flat, no elevation */
        val None: Dp = 0.dp
        
        /** 1dp - Subtle elevation (cards on surface) */
        val XS: Dp = 1.dp
        
        /** 2dp - Standard elevation (buttons, chips) */
        val S: Dp = 2.dp
        
        /** 4dp - Raised elevation (floating action button) */
        val M: Dp = 4.dp
        
        /** 8dp - High elevation (dialogs, bottom sheets) */
        val L: Dp = 8.dp
        
        /** 16dp - Maximum elevation (modals, important overlays) */
        val XL: Dp = 16.dp
    }
    
    /**
     * Border Width Scale
     */
    object BorderWidth {
        /** 1dp - Standard border */
        val Thin: Dp = 1.dp
        
        /** 2dp - Emphasized border */
        val Medium: Dp = 2.dp
        
        /** 4dp - Heavy border (focus states, special emphasis) */
        val Thick: Dp = 4.dp
    }
    
    /**
     * Icon Sizes
     * Consistent sizing for all iconography
     */
    object IconSize {
        /** 12dp - Tiny icons (inline text icons, indicators) */
        val XXS: Dp = 12.dp
        
        /** 16dp - Small icons (list item icons, chips) */
        val XS: Dp = 16.dp
        
        /** 20dp - Standard icons (toolbar icons, buttons) */
        val S: Dp = 20.dp
        
        /** 24dp - Default icons (navigation, primary actions) */
        val M: Dp = 24.dp
        
        /** 32dp - Large icons (emphasized actions) */
        val L: Dp = 32.dp
        
        /** 48dp - Extra large icons (hero icons, empty states) */
        val XL: Dp = 48.dp
        
        /** 64dp - Giant icons (splash, onboarding illustrations) */
        val XXL: Dp = 64.dp
    }
    
    /**
     * Avatar Sizes
     * For profile images and user representations
     */
    object AvatarSize {
        /** 24dp - Tiny avatar (message sender in list) */
        val XS: Dp = 24.dp
        
        /** 32dp - Small avatar (chat bubble avatar) */
        val S: Dp = 32.dp
        
        /** 40dp - Standard avatar (list item, toolbar) */
        val M: Dp = 40.dp
        
        /** 56dp - Large avatar (profile header, match card) */
        val L: Dp = 56.dp
        
        /** 80dp - Extra large avatar (profile screen) */
        val XL: Dp = 80.dp
        
        /** 120dp - Hero avatar (full profile view) */
        val XXL: Dp = 120.dp
    }
    
    /**
     * Animation Durations (in milliseconds)
     * Consistent timing for all animations
     */
    object Duration {
        /** 100ms - Instant feedback (ripple, press) */
        const val Instant: Int = 100
        
        /** 200ms - Quick transitions (fade, slide) */
        const val Quick: Int = 200
        
        /** 300ms - Standard transitions (screen navigation) */
        const val Standard: Int = 300
        
        /** 500ms - Deliberate transitions (emphasis, hero) */
        const val Deliberate: Int = 500
        
        /** 1000ms - Long transitions (complex animations) */
        const val Long: Int = 1000
    }
    
    /**
     * Z-Index Layers
     * Consistent stacking order for UI elements
     */
    object ZIndex {
        const val Background: Float = -1f
        const val Content: Float = 0f
        const val Card: Float = 1f
        const val Dropdown: Float = 10f
        const val Sticky: Float = 100f
        const val AppBar: Float = 1000f
        const val Drawer: Float = 1100f
        const val Modal: Float = 1200f
        const val Popover: Float = 1300f
        const val Toast: Float = 1400f
        const val Tooltip: Float = 1500f
    }
    
    /**
     * Opacity Values
     * Consistent transparency levels
     */
    object Opacity {
        const val Disabled: Float = 0.38f
        const val Inactive: Float = 0.54f
        const val Active: Float = 1.0f
        const val Overlay: Float = 0.5f
        const val Scrim: Float = 0.32f
    }
    
    /**
     * Line Heights
     * Relative to font size for proper typography
     */
    object LineHeight {
        const val Tight: Float = 1.2f    // Headers
        const val Normal: Float = 1.5f   // Body text
        const val Relaxed: Float = 1.75f // Long-form content
    }
    
    /**
     * Letter Spacing
     * In SP for proper text rendering
     */
    object LetterSpacing {
        const val Tight: Float = -0.5f   // Large headlines
        const val Normal: Float = 0f      // Body text
        const val Wide: Float = 0.5f      // All-caps labels
        const val Wider: Float = 1.0f     // Extreme emphasis
    }
}
