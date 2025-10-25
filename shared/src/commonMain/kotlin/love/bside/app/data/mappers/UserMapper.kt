package love.bside.app.data.mappers

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import love.bside.app.data.api.ProfileDTO
import love.bside.app.data.api.UserDTO
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.SeekingStatus

/**
 * Mappers for converting between API DTOs and domain models for users and profiles
 */

/**
 * Convert ProfileDTO from API to domain Profile model
 * Note: API doesn't include timestamps and ID, so we generate defaults
 */
fun ProfileDTO.toDomain(userId: String, profileId: String = "temp-${userId}"): Profile {
    val now = Clock.System.now()
    return Profile(
        id = profileId,
        created = now,
        updated = now,
        userId = userId,
        firstName = firstName,
        lastName = lastName,
        birthDate = LocalDate.parse(calculateBirthDateFromAge(age)), // Approximate
        bio = bio,
        location = location,
        seeking = seeking.toSeekingStatus()
    )
}

/**
 * Convert domain Profile to API ProfileDTO
 */
fun Profile.toDTO(): ProfileDTO {
    return ProfileDTO(
        firstName = firstName,
        lastName = lastName,
        age = calculateAge(birthDate),
        bio = bio,
        location = location,
        seeking = seeking.name
    )
}

/**
 * Convert seeking string to SeekingStatus enum
 */
private fun String.toSeekingStatus(): SeekingStatus {
    return when (this.uppercase()) {
        "FRIENDSHIP" -> SeekingStatus.FRIENDSHIP
        "RELATIONSHIP" -> SeekingStatus.RELATIONSHIP
        "BOTH" -> SeekingStatus.BOTH
        else -> SeekingStatus.BOTH // Default fallback
    }
}

/**
 * Calculate age from birthDate
 */
private fun calculateAge(birthDate: LocalDate): Int {
    val today = kotlinx.datetime.Clock.System.now().toString().substring(0, 10)
    val todayDate = LocalDate.parse(today)
    var age = todayDate.year - birthDate.year
    
    // Adjust if birthday hasn't occurred yet this year
    if (todayDate.monthNumber < birthDate.monthNumber ||
        (todayDate.monthNumber == birthDate.monthNumber && todayDate.dayOfMonth < birthDate.dayOfMonth)) {
        age--
    }
    
    return age
}

/**
 * Approximate birthdate from age (uses Jan 1st of calculated year)
 */
private fun calculateBirthDateFromAge(age: Int): String {
    val today = kotlinx.datetime.Clock.System.now().toString().substring(0, 10)
    val todayDate = LocalDate.parse(today)
    val birthYear = todayDate.year - age
    return "$birthYear-01-01"
}
