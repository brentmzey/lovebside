package love.bside.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class AuthRecord(
    val token: String,
    val record: Profile
)
