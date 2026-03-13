package love.bside.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import love.bside.app.ui.theme.BsideBrand

/**
 * Bside Card Component
 * Follows Apple HIG principles for cards with proper elevation, padding, and rounded corners
 */
@Composable
fun BsideCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ),
    elevation: CardElevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 4.dp,
        hoveredElevation = 6.dp
    ),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = RoundedCornerShape(16.dp),
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = colors,
            elevation = elevation,
            border = border,
            content = content
        )
    }
}

/**
 * Elevated Card - Higher elevation for important content
 */
@Composable
fun BsideElevatedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    BsideCard(
        modifier = modifier,
        onClick = onClick,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 8.dp,
            hoveredElevation = 10.dp
        ),
        content = content
    )
}

/**
 * Outlined Card - Subtle border, no elevation
 */
@Composable
fun BsideOutlinedCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    content: @Composable ColumnScope.() -> Unit
) {
    BsideCard(
        modifier = modifier,
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, borderColor),
        content = content
    )
}

/**
 * Message Card - Specialized for chat messages
 */
@Composable
fun MessageCard(
    modifier: Modifier = Modifier,
    isOutgoing: Boolean,
    onClick: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = if (isOutgoing) {
        CardDefaults.cardColors(
            containerColor = BsideBrand.PlumHeart,
            contentColor = Color.White
        )
    } else {
        CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    
    BsideCard(
        modifier = modifier,
        onClick = onClick,
        colors = colors,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp,
            pressedElevation = 2.dp
        ),
        content = content
    )
}

/**
 * Profile Card - For user profiles with avatar
 */
@Composable
fun ProfileCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    isMatch: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val border = if (isMatch) {
        BorderStroke(2.dp, BsideBrand.TealTile)
    } else null
    
    BsideCard(
        modifier = modifier,
        onClick = onClick,
        border = border,
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp,
            pressedElevation = 6.dp,
            hoveredElevation = 8.dp
        ),
        content = content
    )
}
