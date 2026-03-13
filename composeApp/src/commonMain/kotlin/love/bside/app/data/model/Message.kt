package love.bside.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val id: String,
    val conversation: String,
    val sender: String,
    val content: String,
    val messageType: String = "text", // "text", "image", "video", "audio"
    val replyTo: String? = null,
    val edited: Boolean = false,
    val sequence: Int = 0,
    val created: String,
    val updated: String
)

@Serializable
data class MessageCreate(
    val conversation: String,
    val sender: String,
    val content: String,
    val messageType: String = "text",
    val replyTo: String? = null
)

@Serializable
data class MessageReaction(
    val id: String,
    val message: String,
    val user: String,
    val emoji: String,
    val created: String
)

@Serializable
data class ReadReceipt(
    val id: String,
    val conversation: String,
    val user: String,
    val lastReadMessage: String,
    val readAt: String
)

@Serializable
data class TypingStatus(
    val id: String,
    val conversation: String,
    val user: String,
    val isTyping: Boolean,
    val lastTyped: String
)

@Serializable
data class Conversation(
    val id: String,
    val conversationType: String = "direct", // "direct" or "group"
    val name: String? = null,
    val avatar: String? = null,
    val lastMessage: String? = null,
    val lastMessageAt: String? = null,
    val created: String,
    val updated: String
)

@Serializable
data class ConversationParticipant(
    val id: String,
    val conversation: String,
    val user: String,
    val role: String = "member", // "owner", "admin", "member"
    val joinedAt: String
)
