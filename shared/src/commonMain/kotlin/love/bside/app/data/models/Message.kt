package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Message data model for chat messages
 * Maps to m_messages collection in PocketBase
 */
@Serializable
data class Message(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    
    // Message fields
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val content: String,
    @SerialName("sent_at")
    val sentAt: String, // ISO 8601 timestamp
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("read_at")
    val readAt: String? = null,
    @SerialName("message_type")
    val messageType: MessageType = MessageType.TEXT,
    
    // Optional fields
    val attachmentUrl: String? = null,
    @SerialName("attachment_type")
    val attachmentType: String? = null,
    
    // Threading
    @SerialName("reply_to_message_id") val replyToMessageId: String? = null,
    @SerialName("thread_root_id") val threadRootId: String? = null,
    @SerialName("thread_depth") val threadDepth: Int = 0,
    @SerialName("attachments") val attachments: List<String> = emptyList(),
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
enum class MessageType {
    @SerialName("text")
    TEXT,
    @SerialName("image")
    IMAGE,
    @SerialName("video")
    VIDEO,
    @SerialName("audio")
    AUDIO,
    @SerialName("file")
    FILE
}

/**
 * Message status for local state management
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
