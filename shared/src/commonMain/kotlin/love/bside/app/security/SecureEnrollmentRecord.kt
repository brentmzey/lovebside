package love.bside.app.security

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import love.bside.app.domain.models.AuthDetails

@Serializable
data class SecureEnrollmentRecord(
    val userId: String,
    val email: String,
    val factor: SecureAuthFactor,
    val authDetails: AuthDetails,
    val createdAt: Instant,
    val updatedAt: Instant,
    val label: String? = null
)
