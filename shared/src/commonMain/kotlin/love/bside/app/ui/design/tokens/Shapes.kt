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
    val ExtraSmall = RoundedCornerShape(4.dp)
    val Small = RoundedCornerShape(8.dp)
    val Medium = RoundedCornerShape(12.dp)
    val Large = RoundedCornerShape(16.dp)
    val ExtraLarge = RoundedCornerShape(28.dp)
    val Full = CircleShape
    
    // === Component-Specific Shapes ===
    
    /** Message bubbles - rounded rectangles */
    val MessageBubble = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 18.dp,
        bottomEnd = 4.dp  // Small corner for tail effect
    )
    
    val MessageBubbleSent = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,  // Small corner for tail effect
        bottomEnd = 18.dp
    )
    
    /** Cards */
    val Card = Large
    
    /** Buttons */
    val Button = ExtraLarge
    
    /** Input fields */
    val TextField = Large
    
    /** Bottom sheets */
    val BottomSheet = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp,
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
