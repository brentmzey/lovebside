package love.bside.app.domain.models

import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import love.bside.app.utils.OptionStringSerializer

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
    @Serializable(with = OptionStringSerializer::class)
    val bio: Option<String> = Option.fromNullable(null),
    @Serializable(with = OptionStringSerializer::class)
    val location: Option<String> = Option.fromNullable(null),
    val lat: Double? = null,
    val lng: Double? = null,
    val seeking: SeekingStatus,
    val profilePicture: String = "",
    val photos: List<String> = emptyList(),
    val videos: List<String> = emptyList(),
    @Serializable(with = OptionStringSerializer::class)
    val aboutMe: Option<String> = Option.fromNullable(null),
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
