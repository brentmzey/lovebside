package love.bside.app.ui.messaging

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import love.bside.app.ui.design.tokens.*

/**
 * Beautiful message bubble component with animations.
 * 
 * KMP-compatible, matches Figma designs with gradient backgrounds,
 * rounded corners with tail effect, and smooth entrance animations.
 */
@Composable
fun MessageBubble(
    content: String,
    timestamp: Instant,
    isSent: Boolean,
    modifier: Modifier = Modifier,
    showAvatar: Boolean = true,
    senderInitials: String? = null,
    isRead: Boolean = false
) {
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300)) + 
                scaleIn(initialScale = 0.8f, animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ))
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = BsideSpacing.ScreenPadding),
            horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
        ) {
            // Avatar for received messages
            if (!isSent && showAvatar && senderInitials != null) {
                BsideAvatar(
                    initial = senderInitials,
                    size = BsideIconSizes.Avatar.Small,
                    backgroundColor = BsideColors.PastelPurple,
                    modifier = Modifier.padding(end = BsideSpacing.Small)
                )
            }
            
            Column(
                horizontalAlignment = if (isSent) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                // Message bubble
                Box(
                    modifier = Modifier
                        .clip(if (isSent) BsideShapes.MessageBubbleSent else BsideShapes.MessageBubble)
                        .background(
                            if (isSent) {
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        BsideColors.MessageSentGradientStart,
                                        BsideColors.MessageSentGradientEnd
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        BsideColors.MessageReceived,
                                        BsideColors.MessageReceived
                                    )
                                )
                            }
                        )
                        .padding(BsideSpacing.MessageBubblePadding)
                ) {
                    Text(
                        text = content,
                        style = BsideTypography.MessageText,
                        color = if (isSent) BsideColors.OnPrimary else BsideColors.TextPrimary
                    )
                }
                
                // Timestamp and read status
                Row(
                    modifier = Modifier.padding(
                        top = BsideSpacing.ExtraSmall,
                        start = if (isSent) 0.dp else BsideSpacing.Small,
                        end = if (isSent) BsideSpacing.Small else 0.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start
                ) {
                    Text(
                        text = formatTimestamp(timestamp),
                        style = BsideTypography.MessageTimestamp,
                        color = BsideColors.TextTertiary
                    )
                    
                    if (isSent) {
                        Spacer(modifier = Modifier.width(BsideSpacing.ExtraSmall))
                        Text(
                            text = if (isRead) "✓✓" else "✓",
                            style = BsideTypography.MessageTimestamp,
                            color = if (isRead) BsideColors.Teal else BsideColors.TextTertiary
                        )
                    }
                }
            }
            
            // Avatar for sent messages (optional)
            if (isSent && showAvatar && senderInitials != null) {
                BsideAvatar(
                    initial = senderInitials,
                    size = BsideIconSizes.Avatar.Small,
                    backgroundColor = BsideColors.Primary,
                    modifier = Modifier.padding(start = BsideSpacing.Small)
                )
            }
        }
    }
}

/**
 * Format timestamp for display (e.g., "2:30 PM")
 */
private fun formatTimestamp(timestamp: Instant): String {
    // TODO: Use kotlinx-datetime to format properly across platforms
    return "2:30 PM" // Placeholder
}

/**
 * Avatar component placeholder (will use BsideAvatar from components)
 */
@Composable
private fun BsideAvatar(
    initial: String,
    size: androidx.compose.ui.unit.Dp,
    backgroundColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial.take(2).uppercase(),
            style = BsideTypography.LabelSmall,
            color = BsideColors.OnPrimary
        )
    }
}
