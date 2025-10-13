package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class ProustQuestionnaire(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val questionText: String,
    val questionOrder: Int,
    val isActive: Boolean
)
