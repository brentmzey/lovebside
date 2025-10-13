package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String, // from PocketBase
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String, // Can be mapped to Instant later
    val updated: String,
    @SerialName("userId")
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String, // Can be mapped to LocalDate later
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus
)

@Serializable
enum class SeekingStatus {
    @SerialName("Friendship")
    FRIENDSHIP,

    @SerialName("Relationship")
    RELATIONSHIP,

    @SerialName("Both")
    BOTH
}
