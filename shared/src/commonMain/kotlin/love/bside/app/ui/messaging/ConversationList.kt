package love.bside.app.ui.messaging

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import love.bside.app.ui.design.tokens.*

/**
 * Data class representing a conversation item
 */
data class ConversationItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Instant,
    val avatarInitials: String,
    val isOnline: Boolean = false,
    val unreadCount: Int = 0
)

/**
 * Beautiful conversation list matching Figma designs.
 * 
 * Features:
 * - Avatar with online indicator
 * - Conversation name and preview
 * - Timestamp
 * - Unread badge
 * - Smooth animations
 */
@Composable
fun ConversationList(
    conversations: List<ConversationItem>,
    onConversationClick: (ConversationItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = BsideSpacing.Small)
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            ConversationListItem(
                conversation = conversation,
                onClick = { onConversationClick(conversation) }
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(start = BsideIconSizes.Avatar.Medium + BsideSpacing.Medium * 2),
                color = BsideColors.Divider
            )
        }
    }
}

/**
 * Individual conversation list item
 */
@Composable
private fun ConversationListItem(
    conversation: ConversationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = BsideSpacing.ConversationItemPadding,
                vertical = BsideSpacing.ConversationItemPadding
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            Box(
                modifier = Modifier
                    .size(BsideIconSizes.Avatar.Medium)
                    .clip(CircleShape)
                    .background(getAvatarColor(conversation.avatarInitials)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.avatarInitials,
                    style = BsideTypography.TitleMedium,
                    color = BsideColors.OnPrimary
                )
            }
            
            // Online indicator
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(BsideColors.Online)
                        .padding(2.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(BsideSpacing.ConversationAvatarSpacing))
        
        // Conversation details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Conversation name
                Text(
                    text = conversation.title,
                    style = BsideTypography.ConversationTitle,
                    color = BsideColors.TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Timestamp
                Text(
                    text = formatConversationTime(conversation.timestamp),
                    style = BsideTypography.LabelSmall,
                    color = BsideColors.TextTertiary
                )
            }
            
            Spacer(modifier = Modifier.height(BsideSpacing.ConversationItemVerticalSpacing))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Last message preview
                Text(
                    text = conversation.lastMessage,
                    style = BsideTypography.ConversationPreview,
                    color = if (conversation.unreadCount > 0) BsideColors.TextPrimary else BsideColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                // Unread badge
                AnimatedVisibility(
                    visible = conversation.unreadCount > 0,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(BsideColors.Primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (conversation.unreadCount > 99) "99+" else conversation.unreadCount.toString(),
                            style = BsideTypography.UnreadBadge,
                            color = BsideColors.OnPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Get avatar color based on initials hash
 */
private fun getAvatarColor(initials: String): androidx.compose.ui.graphics.Color {
    val colors = listOf(
        BsideColors.PastelPurple,
        BsideColors.PastelPink,
        BsideColors.PastelBlue,
        BsideColors.PastelGreen,
        BsideColors.PastelOrange,
        BsideColors.PastelYellow
    )
    return colors[initials.hashCode().mod(colors.size)]
}

/**
 * Format conversation timestamp (e.g., "2:30 PM", "Yesterday", "Mon")
 */
private fun formatConversationTime(timestamp: Instant): String {
    // TODO: Use kotlinx-datetime for proper formatting
    return "2:30 PM" // Placeholder
}
