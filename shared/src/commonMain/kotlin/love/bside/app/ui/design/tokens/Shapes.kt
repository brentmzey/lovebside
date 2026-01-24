package love.bside.app.ui.design.tokens

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * BSide shape system for consistent component styling.
 * Based on Material Design 3 rounded corner system.
 */
object BsideShapes {
    
    // === Corner Radius Values ===
    val None = RoundedCornerShape(0.dp)
    val ExtraSmall = RoundedCornerShape(8.dp)
    val Small = RoundedCornerShape(12.dp)
    val Medium = RoundedCornerShape(16.dp)
    val Large = RoundedCornerShape(24.dp)
    val ExtraLarge = RoundedCornerShape(32.dp)
    val Full = CircleShape
    
    // === Component-Specific Shapes ===
    
    /** Message bubbles - rounded rectangles */
    val MessageBubble = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 20.dp,
        bottomEnd = 8.dp  // Small corner for tail effect
    )
    
    val MessageBubbleSent = RoundedCornerShape(
        topStart = 20.dp,
        topEnd = 20.dp,
        bottomStart = 8.dp,  // Small corner for tail effect
        bottomEnd = 20.dp
    )
    
    /** Cards */
    val Card = Large
    
    /** Buttons */
    val Button = Full
    
    /** Input fields */
    val TextField = Small
    
    /** Bottom sheets */
    val BottomSheet = RoundedCornerShape(
        topStart = 24.dp,
        topEnd = 24.dp,
        bottomStart = 0.dp,
        bottomEnd = 0.dp
    )
    
    /** Dialogs */
    val Dialog = ExtraLarge
    
    /** Avatar */
    val Avatar = Full
    
    /** Chips */
    val Chip = Full
}
