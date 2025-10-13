package love.bside.app.domain.models

data class AuthDetails(
    val token: String,
    val profile: Profile
)
