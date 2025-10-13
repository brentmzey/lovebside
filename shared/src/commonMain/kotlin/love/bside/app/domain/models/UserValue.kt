package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class UserValue(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val userId: String,
    val valueId: String
)
