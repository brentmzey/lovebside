package love.bside.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import love.bside.app.AppConstants
import love.bside.app.domain.models.Message
import love.bside.app.domain.models.MessageType

@Composable
fun MessageBubble(
        message: Message,
        isCurrentUser: Boolean,
        modifier: Modifier = Modifier,
        isMyMessage: Boolean = isCurrentUser, // Compatibility alias
        onReplyDrag: ((String) -> Unit)? = null
) {
        Row(
                modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
        ) {
                Surface(
                        shape =
                                RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                                        bottomEnd = if (isCurrentUser) 4.dp else 16.dp
                                ),
                        color =
                                if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.widthIn(max = 280.dp)
                ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                        text = message.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color =
                                                if (isCurrentUser)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (message.messageType == MessageType.IMAGE &&
                                                message.attachments.isNotEmpty()
                                ) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        message.attachments.forEach { fileName ->
                                                val url =
                                                        "${AppConstants.POCKETBASE_URL}/api/files/m_messages/${message.id}/$fileName"
                                                AsyncImage(
                                                        model = url,
                                                        contentDescription = "Attachment",
                                                        modifier =
                                                                Modifier.fillMaxWidth()
                                                                        .heightIn(max = 200.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        8.dp
                                                                                )
                                                                        ),
                                                        contentScale = ContentScale.Crop
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                        }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                        text = formatTimestamp(message.sentAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color =
                                                if (isCurrentUser)
                                                        MaterialTheme.colorScheme.onPrimaryContainer
                                                                .copy(alpha = 0.7f)
                                                else
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                                .copy(alpha = 0.7f)
                                )

                                if (message.readAt != null && isCurrentUser) {
                                        Text(
                                                text = "Read",
                                                style = MaterialTheme.typography.labelSmall,
                                                color =
                                                        MaterialTheme.colorScheme.primary.copy(
                                                                alpha = 0.7f
                                                        )
                                        )
                                }
                        }
                }
        }
}

private fun formatTimestamp(timestamp: Instant): String {
        val local = timestamp.toLocalDateTime(TimeZone.currentSystemDefault())
        val hour = local.hour.toString().padStart(2, '0')
        val minute = local.minute.toString().padStart(2, '0')
        return "$hour:$minute"
}
