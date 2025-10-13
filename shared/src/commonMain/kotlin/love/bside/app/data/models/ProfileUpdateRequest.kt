package love.bside.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ProfileUpdateRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val birthDate: String? = null,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus? = null
)
