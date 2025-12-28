package love.bside.app.domain.models

import kotlinx.datetime.Instant

enum class ConversationType {
    DIRECT, GROUP, CHANNEL
}

data class Conversation(
    val id: String,
    val conversationType: ConversationType,
    val conversationName: String?,
    val conversationAvatar: String?,
    val lastMessageText: String?,
    val lastMessageAt: Instant?,
    val totalMessageCount: Int,
    val maxParticipants: Int,
    val isArchived: Boolean,
    val created: Instant,
    val updated: Instant
)
