package love.bside.app.data.models

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import love.bside.app.domain.models.Profile as DomainProfile
import love.bside.app.domain.models.SeekingStatus as DomainSeekingStatus

fun Profile.toDomain(): DomainProfile {
    return DomainProfile(
        id = this.id,
        created = Instant.parse(this.created),
        updated = Instant.parse(this.updated),
        userId = this.userId,
        firstName = this.firstName,
        lastName = this.lastName,
        birthDate = LocalDate.parse(this.birthDate.substring(0, 10)), // PocketBase date format is `YYYY-MM-DD HH:MM:SS.SSSZ`
        bio = this.bio,
        location = this.location,
        seeking = when (this.seeking) {
            SeekingStatus.FRIENDSHIP -> DomainSeekingStatus.FRIENDSHIP
            SeekingStatus.RELATIONSHIP -> DomainSeekingStatus.RELATIONSHIP
            SeekingStatus.BOTH -> DomainSeekingStatus.BOTH
        }
    )
}
