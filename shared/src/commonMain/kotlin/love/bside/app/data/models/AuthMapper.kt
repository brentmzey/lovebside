package love.bside.app.data.models

import love.bside.app.domain.models.AuthDetails as DomainAuthDetails

fun AuthRecord.toDomain(): DomainAuthDetails {
    return DomainAuthDetails(
        token = this.token,
        profile = this.record.toDomain()
    )
}
