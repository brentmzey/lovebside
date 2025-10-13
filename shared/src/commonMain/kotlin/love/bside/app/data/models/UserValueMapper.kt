package love.bside.app.data.models

import kotlinx.datetime.Instant
import love.bside.app.domain.models.UserValue as DomainUserValue

fun UserValue.toDomain(): DomainUserValue {
    return DomainUserValue(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        userId = this.userId,
        valueId = this.keyValueId
    )
}
