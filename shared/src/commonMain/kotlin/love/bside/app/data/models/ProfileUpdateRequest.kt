package love.bside.app.data.models

import love.bside.app.domain.models.SeekingStatus
import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequest(
    val firstName: String? = null,
    val middle: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus? = null,
    val aboutMe: String? = null,
    val height: Double? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String>? = null
)
