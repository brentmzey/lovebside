package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class TypingStatus(
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val lastTyped: Instant
)
