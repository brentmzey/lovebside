package love.bside.app.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthDetails(
    val token: String,
    val profile: Profile
)
