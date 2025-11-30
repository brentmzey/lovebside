package love.bside.app.domain.models

import kotlinx.datetime.LocalDate

/**
 * Strongly typed payload for registration forms so that every platform
 * (Android, iOS, Desktop, Web) sends the same data the server expects.
 */
data class SignUpData(
    val email: String,
    val password: String,
    val passwordConfirm: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate,
    val seeking: SeekingStatus
)
