package love.bside.app.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Parses a PocketBase datetime string into a kotlinx.datetime.Instant.
 * PocketBase often returns dates with a space separator (e.g., "2023-01-01 12:00:00.000Z")
 * instead of the ISO-8601 'T' separator required by Instant.parse.
 *
 * @return Instant if valid, or null if parsing fails or string is empty.
 */
fun String.parsePocketBaseInstant(): Instant? {
    if (this.isBlank()) return null
    return try {
        // Normalize: "2023-01-01 12:00:00.123Z" -> "2023-01-01T12:00:00.123Z"
        val normalized = this.replace(" ", "T")
        Instant.parse(normalized)
    } catch (e: Exception) {
        null
    }
}

/**
 * Parses a PocketBase datetime string, returning a default value if parsing fails.
 */
fun String.parsePocketBaseInstantOr(defaultValue: Instant = Clock.System.now()): Instant {
    return parsePocketBaseInstant() ?: defaultValue
}
