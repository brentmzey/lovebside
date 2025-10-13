package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.Match as DomainMatch

fun Match.toDomain(): DomainMatch {
    return DomainMatch(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        userOneId = this.userOneId,
        userTwoId = this.userTwoId,
        matchScore = this.matchScore,
        matchStatus = this.matchStatus,
        generatedAt = Instant.parse(this.generatedAt)
    )
}
