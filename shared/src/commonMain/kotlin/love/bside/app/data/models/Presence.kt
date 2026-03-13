package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Presence(
    val id: String,
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String,
    val updated: String,
    
    @SerialName("user_id")
    val userId: String,
    val status: PresenceStatus,
    @SerialName("activity_message")
    val activityMessage: String? = null,
    @SerialName("last_active")
    val lastActive: String
)

@Serializable
enum class PresenceStatus {
    @SerialName("online")
    ONLINE,
    @SerialName("away")
    AWAY,
    @SerialName("busy")
    BUSY,
    @SerialName("in_call")
    IN_CALL,
    @SerialName("in_meeting")
    IN_MEETING,
    @SerialName("driving")
    DRIVING,
    @SerialName("offline")
    OFFLINE
}
