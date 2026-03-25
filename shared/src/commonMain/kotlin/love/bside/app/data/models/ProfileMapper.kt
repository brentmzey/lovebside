package love.bside.app.data.models

import arrow.core.Option
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import love.bside.app.domain.models.Profile as DomainProfile
import love.bside.app.domain.models.SeekingStatus as DomainSeekingStatus

fun Profile.toDomain(): DomainProfile {
    return DomainProfile(
        id = this.id,
        // Date parsing is now handled by the Serializer
        created = this.created ?: Instant.fromEpochMilliseconds(0),
        updated = this.updated ?: Instant.fromEpochMilliseconds(0),
        userId = this.userId,
        firstName = this.firstName,
        lastName = this.lastName,
        birthDate = LocalDate.parse(this.birthDate.substring(0, 10)), // PocketBase date format is `YYYY-MM-DD HH:MM:SS.SSSZ`
        bio = Option.fromNullable(this.bio),
        education = this.education ?: "",
        interests = this.interests ?: emptyList(),
        videos = this.videos ?: emptyList(),
        profilePicture = this.profilePicture ?: "",
        photos = this.photos ?: emptyList(),
        location = Option.fromNullable(this.location),
        lat = this.lat,
        lng = this.lng,
        seeking = when (this.seeking) {
            SeekingStatus.FRIENDSHIP -> DomainSeekingStatus.FRIENDSHIP
            SeekingStatus.RELATIONSHIP -> DomainSeekingStatus.RELATIONSHIP
            SeekingStatus.BOTH -> DomainSeekingStatus.BOTH
        }
    )
}
