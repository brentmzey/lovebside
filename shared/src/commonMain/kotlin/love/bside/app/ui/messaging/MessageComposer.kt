package love.bside.app.ui.messaging

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape  
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import love.bside.app.ui.components.BsideTextField
import love.bside.app.ui.design.tokens.*

/**
 * Message composer component for typing and sending messages.
 * 
 * Features:
 * - Multi-line input
 * - Send button (only enabled with content)
 * - Typing indicator
 * - Beautiful animations
 * - Attachment button (optional)
 */
@Composable
fun MessageComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type a message...",
    showAttachmentButton: Boolean = true,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BsideColors.Surface)
            .padding(BsideSpacing.MessageComposerPadding),
        verticalAlignment = Alignment.Bottom
    ) {
        // Attachment button (optional)
        if (showAttachmentButton) {
            IconButton(
                onClick = { /* TODO: Handle attachment */ },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Attach",
                    tint = BsideColors.TextSecondary
                )
            }
        }
        
        // Message input field
        BsideTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            enabled = enabled,
            singleLine = false,
            maxLines = 4,
            modifier = Modifier.weight(1f)
        )
        
        Spacer(modifier = Modifier.width(BsideSpacing.Small))
        
        // Send button
        if (value.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(BsideColors.Primary),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onSend,
                    enabled = enabled,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = BsideColors.OnPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Typing indicator component
 */
@Composable
fun TypingIndicator(
    typingUsers: List<String>,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = typingUsers.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = BsideSpacing.ScreenPadding,
                vertical = BsideSpacing.Small
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated dots
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    var scale by remember { mutableStateOf(1f) }
                    LaunchedEffect(Unit) {
                        while (true) {
                            kotlinx.coroutines.delay(300L * index)
                            scale = 1.3f
                            kotlinx.coroutines.delay(300L)
                            scale = 1f
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp * scale)
                            .clip(CircleShape)
                            .background(BsideColors.Typing)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(BsideSpacing.Small))
            
            Text(
                text = when {
                    typingUsers.size == 1 -> "${typingUsers[0]} is typing..."
                    typingUsers.size == 2 -> "${typingUsers[0]} and ${typingUsers[1]} are typing..."
                    else -> "${typingUsers.size} people are typing..."
                },
                style = BsideTypography.TypingIndicator,
                color = BsideColors.TextSecondary
            )
        }
    }
}
