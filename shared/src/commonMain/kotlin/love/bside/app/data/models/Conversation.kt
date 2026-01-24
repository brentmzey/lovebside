package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Conversation model for grouping messages.
 * Maps to `conversations` collection in PocketBase.
 */
@Serializable
data class Conversation(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,

    @SerialName("type")
    val type: ConversationType = ConversationType.DIRECT,
    
    // List of User IDs
    @SerialName("participants")
    val participants: List<String> = emptyList(),
    
    // Optional name for group chats
    @SerialName("name")
    val name: String? = null,
    
    @SerialName("last_message_at")
    val lastMessageAt: String? = null
)

@Serializable
enum class ConversationType {
    @SerialName("direct")
    DIRECT,
    @SerialName("group")
    GROUP
}
