package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.UserAnswer as DomainUserAnswer

fun UserAnswer.toDomain(): DomainUserAnswer {
    return DomainUserAnswer(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        userId = this.userId,
        questionId = this.questionId,
        answerText = this.answerText
    )
}
