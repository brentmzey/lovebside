package love.bside.app.domain.models

import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import love.bside.app.utils.OptionStringSerializer


enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM
}

@Serializable
data class Message(
        val id: String,
        val collectionId: String,
        val conversationId: String,
        val senderId: String,
        @Serializable(with = OptionStringSerializer::class)
        val content: Option<String>,
        val messageType: MessageType,
        val attachments: List<String>,
        val sentAt: Instant,
        val editedAt: Instant?,
        val deletedAt: Instant?,
        val readByCount: Int,
        val isRead: Boolean = false,
        val readAt: Instant? = null,
        // Threading fields
        val replyToMessageId: String?,
        val replyToMessage: Message? = null, // Expanded record
        val threadRootId: String?,
        val threadDepth: Int?,
        val threadReplyCount: Int?,
        // Reactions
        val reactions: Map<String, List<String>> = emptyMap(),
        // Metadata
        val created: Instant,
        val updated: Instant
)
