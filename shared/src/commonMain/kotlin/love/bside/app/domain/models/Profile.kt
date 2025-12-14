package love.bside.app.domain.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable


@Serializable
data class Profile(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val userId: String,
    val firstName: String,
    val middle: String = "",
    val lastName: String,
    val birthDate: LocalDate,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus,
    val profilePicture: String = "",
    val photos: List<String> = emptyList(),
    val aboutMe: String = "",
    val height: Double? = null,
    val occupation: String = "",
    val education: String = "",
    val interests: List<String> = emptyList()
)

@Serializable
enum class SeekingStatus {
    FRIENDSHIP,
    RELATIONSHIP,
    BOTH
}
