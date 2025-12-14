package love.bside.app.domain.models

import kotlinx.datetime.Instant

enum class ParticipantRole {
    ADMIN, MEMBER, READONLY
}

data class ConversationParticipant(
    val id: String,
    val conversationId: String,
    val userId: String,
    val role: ParticipantRole,
    val unreadCount: Int,
    val lastReadAt: Instant?,
    val joinedAt: Instant,
    val leftAt: Instant?,
    val isMuted: Boolean,
    val isPinned: Boolean,
    val created: Instant,
    val updated: Instant
)
