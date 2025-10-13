package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class Prompt(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val matchId: String,
    val promptText: String,
    val promptType: String
)
