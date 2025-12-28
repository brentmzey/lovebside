package love.bside.app.ui.design.tokens

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * BSide spacing system for consistent layout.
 * 
 * Based on 8dp grid system with semantic naming.
 */
object BsideSpacing {
    
    // === Base Grid (8dp system) ===
    val None: Dp = 0.dp
    val ExtraSmall: Dp = 4.dp      // 0.5x
    val Small: Dp = 8.dp           // 1x
    val Medium: Dp = 16.dp         // 2x
    val Large: Dp = 24.dp          // 3x
    val ExtraLarge: Dp = 32.dp     // 4x
    val ExtraExtraLarge: Dp = 48.dp // 6x
    
    // === Semantic Spacing ===
    
    /** Internal padding within components */
    val ComponentPadding = Medium
    
    /** Padding between components */
    val ComponentSpacing = Medium
    
    /** Screen edge padding */
    val ScreenPadding = Large
    
    /** Card padding */
    val CardPadding = Medium
    
    /** List item padding */
    val ListItemPadding = Medium
    
    /** Bottom sheet padding */
    val BottomSheetPadding = Large
    
    /** Dialog padding */
    val DialogPadding = Large
    
    /** Button padding horizontal */
    val ButtonPaddingHorizontal = Large
    
    /** Button padding vertical */
    val ButtonPaddingVertical = Small
    
    // === Message-Specific Spacing ===
    
    /** Space between message bubbles */
    val MessageBubbleSpacing = Small
    
    /** Padding inside message bubble */
    val MessageBubblePadding = Medium
    
    /** Space between messages in different groups */
    val MessageGroupSpacing = Medium
    
    /** Message composer padding */
    val MessageComposerPadding = Medium
    
    /** Space between avatar and message */
    val AvatarMessageSpacing = Small
    
    // === Conversation List Spacing ===
    
    /** Padding for conversation list item */
    val ConversationItemPadding = Medium
    
    /** Space between avatar and text */
    val ConversationAvatarSpacing = Medium
    
    /** Vertical spacing in conversation item */
    val ConversationItemVerticalSpacing = ExtraSmall
}

/**
 * Icon sizes for consistent iconography
 */
object BsideIconSizes {
    val ExtraSmall: Dp = 16.dp
    val Small: Dp = 20.dp
    val Medium: Dp = 24.dp
    val Large: Dp = 32.dp
    val ExtraLarge: Dp = 48.dp
    
    /** Avatar sizes */
    object Avatar {
        val Small: Dp = 32.dp
        val Medium: Dp = 40.dp
        val Large: Dp = 56.dp
        val ExtraLarge: Dp = 80.dp
    }
}

/**
 * Elevation levels for depth
 */
object BsideElevation {
    val None: Dp = 0.dp
    val Level1: Dp = 1.dp
    val Level2: Dp = 3.dp
    val Level3: Dp = 6.dp
    val Level4: Dp = 8.dp
    val Level5: Dp = 12.dp
}
