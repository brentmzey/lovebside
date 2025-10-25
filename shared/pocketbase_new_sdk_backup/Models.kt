package love.bside.app.data.api.pocketbase

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Common models used across PocketBase SDK
 */

/**
 * Base record interface matching PocketBase schema
 */
interface Record {
    val id: String
    val collectionId: String
    val collectionName: String
    val created: String
    val updated: String
}

/**
 * Generic list result from PocketBase
 */
@Serializable
data class ListResult<T>(
    val page: Int = 1,
    val perPage: Int = 30,
    val totalItems: Int = 0,
    val totalPages: Int = 0,
    val items: List<T> = emptyList()
)

/**
 * Authentication response from PocketBase
 */
@Serializable
data class AuthResponse<T>(
    val token: String,
    val record: T,
    val meta: JsonObject? = null
)

/**
 * OAuth2 authentication response
 */
@Serializable
data class OAuth2AuthResponse<T>(
    val token: String,
    val record: T,
    val meta: OAuth2Meta
)

@Serializable
data class OAuth2Meta(
    val id: String,
    val name: String,
    val username: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val rawUser: JsonObject,
    val accessToken: String,
    val refreshToken: String? = null
)

/**
 * Error response from PocketBase
 */
@Serializable
data class PocketBaseError(
    val code: Int,
    val message: String,
    val data: Map<String, ValidationError>
)

@Serializable
data class ValidationError(
    val code: String,
    val message: String
)

/**
 * Health check response
 */
@Serializable
data class HealthCheck(
    val code: Int,
    val message: String,
    val data: JsonObject = JsonObject(emptyMap())
)

/**
 * Backup info
 */
@Serializable
data class BackupInfo(
    val key: String,
    val size: Long,
    val modified: String
)

/**
 * Log request
 */
@Serializable
data class LogRequest(
    val level: Int = 0,
    val data: JsonObject
)

/**
 * Settings response
 */
@Serializable
data class Settings(
    val meta: SettingsMeta,
    val logs: LogsSettings,
    val smtp: SmtpSettings,
    val s3: S3Settings,
    val backups: BackupsSettings,
    val adminAuthToken: AdminAuthTokenSettings,
    val adminPasswordResetToken: AdminPasswordResetTokenSettings,
    val recordAuthToken: RecordAuthTokenSettings,
    val recordPasswordResetToken: RecordPasswordResetTokenSettings,
    val recordEmailChangeToken: RecordEmailChangeTokenSettings,
    val recordVerificationToken: RecordVerificationTokenSettings,
    val emailAuth: EmailAuthSettings,
    val googleAuth: OAuth2Settings,
    val facebookAuth: OAuth2Settings,
    val githubAuth: OAuth2Settings,
    val gitlabAuth: OAuth2Settings,
    val discordAuth: OAuth2Settings,
    val twitterAuth: OAuth2Settings,
    val microsoftAuth: OAuth2Settings,
    val spotifyAuth: OAuth2Settings,
    val kakaoAuth: OAuth2Settings,
    val twitchAuth: OAuth2Settings,
    val stravaAuth: OAuth2Settings,
    val giteeAuth: OAuth2Settings,
    val livechatAuth: OAuth2Settings
)

@Serializable
data class SettingsMeta(
    val appName: String,
    val appUrl: String,
    val hideControls: Boolean,
    val senderName: String,
    val senderAddress: String,
    val verificationTemplate: EmailTemplate,
    val resetPasswordTemplate: EmailTemplate,
    val confirmEmailChangeTemplate: EmailTemplate
)

@Serializable
data class EmailTemplate(
    val body: String,
    val subject: String,
    val actionUrl: String
)

@Serializable
data class LogsSettings(
    val maxDays: Int
)

@Serializable
data class SmtpSettings(
    val enabled: Boolean,
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val tls: Boolean
)

@Serializable
data class S3Settings(
    val enabled: Boolean,
    val bucket: String,
    val region: String,
    val endpoint: String,
    val accessKey: String,
    val secret: String,
    val forcePathStyle: Boolean
)

@Serializable
data class BackupsSettings(
    val cron: String,
    val cronMaxKeep: Int,
    val s3: S3Settings
)

@Serializable
data class AdminAuthTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class AdminPasswordResetTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class RecordAuthTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class RecordPasswordResetTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class RecordEmailChangeTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class RecordVerificationTokenSettings(
    val secret: String,
    val duration: Long
)

@Serializable
data class EmailAuthSettings(
    val enabled: Boolean,
    val exceptDomains: List<String>,
    val onlyDomains: List<String>,
    val minPasswordLength: Int
)

@Serializable
data class OAuth2Settings(
    val enabled: Boolean,
    val clientId: String,
    val clientSecret: String,
    val authUrl: String? = null,
    val tokenUrl: String? = null,
    val userApiUrl: String? = null
)

/**
 * Realtime subscription event
 */
sealed class RealtimeEvent {
    data class Create<T>(val record: T) : RealtimeEvent()
    data class Update<T>(val record: T) : RealtimeEvent()
    data class Delete<T>(val record: T) : RealtimeEvent()
}

// Request models for internal use

@Serializable
internal data class AuthPasswordRequest(
    val identity: String,
    val password: String
)

@Serializable
internal data class OAuth2Request(
    val provider: String,
    val code: String,
    val codeVerifier: String,
    val redirectUrl: String
)

@Serializable
internal data class EmailRequest(
    val email: String
)

@Serializable
internal data class PasswordResetRequest(
    val token: String,
    val password: String,
    val passwordConfirm: String
)

@Serializable
internal data class EmailChangeRequest(
    val token: String
)
