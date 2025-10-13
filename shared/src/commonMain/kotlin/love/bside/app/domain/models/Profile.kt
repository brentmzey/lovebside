package love.bside.app.domain.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate


data class Profile(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus
)

enum class SeekingStatus {
    FRIENDSHIP,
    RELATIONSHIP,
    BOTH
}
