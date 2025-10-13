package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.ProustQuestionnaire as DomainProustQuestionnaire

fun ProustQuestionnaire.toDomain(): DomainProustQuestionnaire {
    return DomainProustQuestionnaire(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        questionText = this.questionText,
        questionOrder = this.questionOrder,
        isActive = this.isActive
    )
}
