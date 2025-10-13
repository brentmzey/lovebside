package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class Match(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val userOneId: String,
    val userTwoId: String,
    val matchScore: Double,
    val matchStatus: String,
    val generatedAt: Instant
)
