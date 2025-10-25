package love.bside.app.data.models.messaging

import kotlinx.serialization.Serializable

/**
 * Represents a 1-to-1 conversation between two users
 * Maps to PocketBase collection: m_conversations
 */
@Serializable
data class Conversation(
    val id: String,
    val participant1Id: String,
    val participant2Id: String,
    val lastMessageText: String? = null,
    val lastMessageAt: String? = null,
    val participant1UnreadCount: Int = 0,
    val participant2UnreadCount: Int = 0,
    val participant1LastReadAt: String? = null,
    val participant2LastReadAt: String? = null,
    val created: String,
    val updated: String
) {
    /**
     * Get the other participant's ID
     */
    fun getOtherParticipantId(currentUserId: String): String {
        return if (participant1Id == currentUserId) participant2Id else participant1Id
    }
    
    /**
     * Get unread count for current user
     */
    fun getUnreadCount(currentUserId: String): Int {
        return if (participant1Id == currentUserId) {
            participant1UnreadCount
        } else {
            participant2UnreadCount
        }
    }
    
    /**
     * Check if user is a participant
     */
    fun isParticipant(userId: String): Boolean {
        return participant1Id == userId || participant2Id == userId
    }
}

/**
 * Individual message within a conversation
 * Maps to PocketBase collection: m_messages
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: MessageType,
    val status: MessageStatus,
    val sentAt: String,
    val deliveredAt: String? = null,
    val readAt: String? = null,
    val editedAt: String? = null,
    val deletedAt: String? = null,
    val created: String,
    val updated: String
) {
    /**
     * Check if message was sent by current user
     */
    fun isSentByUser(userId: String): Boolean = senderId == userId
    
    /**
     * Check if message is unread by receiver
     */
    fun isUnread(): Boolean = status != MessageStatus.READ
    
    /**
     * Check if message is deleted
     */
    fun isDeleted(): Boolean = deletedAt != null
    
    /**
     * Check if message is edited
     */
    fun isEdited(): Boolean = editedAt != null
}

/**
 * Message type enumeration
 */
@Serializable
enum class MessageType {
    TEXT,     // Regular text message
    IMAGE,    // Image message
    SYSTEM    // System message (user joined, etc)
}

/**
 * Message delivery status
 */
@Serializable
enum class MessageStatus {
    SENDING,    // Being sent (local only)
    SENT,       // Sent to server
    DELIVERED,  // Delivered to receiver
    READ,       // Read by receiver
    FAILED      // Failed to send
}

/**
 * Typing indicator for real-time typing status
 * Maps to PocketBase collection: m_typing_indicators
 */
@Serializable
data class TypingIndicator(
    val id: String,
    val conversationId: String,
    val userId: String,
    val isTyping: Boolean,
    val lastUpdated: String,
    val created: String,
    val updated: String
)

/**
 * Read receipt for message read tracking
 * Maps to PocketBase collection: m_read_receipts
 */
@Serializable
data class ReadReceipt(
    val id: String,
    val messageId: String,
    val userId: String,
    val readAt: String,
    val created: String,
    val updated: String
)

/**
 * Real-time event types for messaging
 */
sealed class MessageEvent {
    data class NewMessage(val message: Message) : MessageEvent()
    data class MessageUpdated(val message: Message) : MessageEvent()
    data class MessageDeleted(val messageId: String) : MessageEvent()
    data class MessageRead(val messageId: String, val readAt: String) : MessageEvent()
}

/**
 * Real-time typing event
 */
sealed class TypingEvent {
    data class UserTyping(val userId: String, val isTyping: Boolean) : TypingEvent()
}

/**
 * Conversation event for real-time updates
 */
sealed class ConversationEvent {
    data class Updated(val conversation: Conversation) : ConversationEvent()
    data class NewMessage(val conversationId: String) : ConversationEvent()
}
