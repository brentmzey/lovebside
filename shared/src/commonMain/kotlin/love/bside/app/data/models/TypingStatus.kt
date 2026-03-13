package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypingStatus(
    @SerialName("user_id") val userId: String,
    @SerialName("conversation_id") val conversationId: String,
    @SerialName("is_typing") val isTyping: Boolean,
    val updated: String
)
