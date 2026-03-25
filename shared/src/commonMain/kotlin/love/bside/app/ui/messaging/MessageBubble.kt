package love.bside.app.ui.messaging

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Instant
import love.bside.app.ui.design.tokens.*
import coil3.compose.AsyncImage

import love.bside.app.domain.models.Message
import arrow.core.getOrElse
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale

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
    isRead: Boolean = false,
    mediaUrls: List<String> = emptyList(),
    quotedReply: @Composable (() -> Unit)? = null,
    reactions: Map<String, Int> = emptyMap(), // Emoji -> Count
    onReactionClick: (String) -> Unit = {}
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
                // Quoted Reply
                quotedReply?.invoke()

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

                    // Attachments
                    if (mediaUrls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        mediaUrls.forEach { url ->
                            AsyncImage(
                                model = url,
                                contentDescription = "Attachment",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                // Reactions
                if (reactions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = if (isSent) Arrangement.End else Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        reactions.forEach { (emoji, count) ->
                            Box(
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BsideColors.Neutral100)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$emoji $count",
                                    style = BsideTypography.LabelSmall,
                                    color = BsideColors.TextSecondary
                                )
                            }
                        }
                    }
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
    return try {
        val iso = timestamp.toString() // 2024-01-30T14:30:00Z
        val timePart = iso.substringAfter("T").substringBefore("Z").take(5) // 14:30
        timePart
    } catch (e: Exception) {
        ""
    }
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

@Composable
private fun QuotedReply(
    message: Message,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(bottom = BsideSpacing.ExtraSmall)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(BsideColors.Background)
            .padding(BsideSpacing.Small)
    ) {
        Icon(
            imageVector = Icons.Default.Reply,
            contentDescription = "Replied to",
            tint = BsideColors.TextSecondary,
            modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
        )
        Spacer(Modifier.width(BsideSpacing.Small))
        Column {
            Text(
                text = message.senderId, // TODO: Get sender name
                style = BsideTypography.LabelSmall,
                color = BsideColors.Primary,
                maxLines = 1
            )
            Text(
                text = message.content.getOrElse { "" },
                style = BsideTypography.BodySmall,
                color = BsideColors.TextSecondary,
                maxLines = 1
            )
        }
    }
}


/**
 * Overload for Message object.
 */
@Composable
fun MessageBubble(
    message: Message,
    isMyMessage: Boolean,
    modifier: Modifier = Modifier,
    onReplyClick: () -> Unit = {},
    onReplyDrag: () -> Unit = {}
) {
    // Construct URLs
    // Assuming local dev: http://127.0.0.1:8090
    // In prod, should be injected or from config
    val baseUrl = "http://127.0.0.1:8090"
    val mediaUrls = message.attachments.map { fileName ->
        "$baseUrl/api/files/${message.collectionId}/${message.id}/$fileName"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isMyMessage) Arrangement.End else Arrangement.Start,
        modifier = Modifier.fillMaxWidth()
    ) {
        if (!isMyMessage) {
            IconButton(onClick = onReplyClick) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = BsideColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        val quotedReply = if (message.replyToMessage != null) {
            @Composable { QuotedReply(message.replyToMessage) }
        } else {
            null
        }
        
        val reactionCounts = message.reactions.mapValues { it.value.size }

        MessageBubble(
            content = message.content.getOrElse { "" },
            timestamp = message.sentAt,
            isSent = isMyMessage,
            modifier = modifier,
            showAvatar = !isMyMessage, // Logic from ChatScreen
            senderInitials = "??", // TODO: Get from sender name via repo or message expansion
            mediaUrls = mediaUrls,
            quotedReply = quotedReply,
            reactions = reactionCounts
        )

        if (isMyMessage) {
            IconButton(onClick = onReplyClick) {
                Icon(
                    imageVector = Icons.Default.Reply,
                    contentDescription = "Reply",
                    tint = BsideColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
