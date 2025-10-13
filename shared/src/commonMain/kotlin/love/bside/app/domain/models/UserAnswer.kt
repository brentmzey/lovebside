package love.bside.app.domain.models

import kotlinx.datetime.Instant

data class UserAnswer(
    val id: String,
    val created: Instant,
    val updated: Instant,
    val userId: String,
    val questionId: String,
    val answerText: String
)
