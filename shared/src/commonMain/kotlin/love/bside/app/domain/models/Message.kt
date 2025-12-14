package love.bside.app.domain.models

import kotlinx.datetime.Instant

enum class MessageType {
    TEXT, IMAGE, FILE, SYSTEM
}

data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val content: String,
    val messageType: MessageType,
    val attachments: List<String>,
    val sentAt: Instant,
    val editedAt: Instant?,
    val deletedAt: Instant?,
    val readByCount: Int,
    // Threading fields
    val replyToMessageId: String?,
    val threadRootId: String?,
    val threadDepth: Int?,
    val threadReplyCount: Int?,
    // Metadata
    val created: Instant,
    val updated: Instant
)
