package love.bside.services

/**
 * CDN Media Service - S3 + CloudFront Integration
 * See SCALABILITY_ARCHITECTURE.md for full details
 */

data class UploadUrlResponse(
    val uploadUrl: String,
    val cdnUrl: String,
    val key: String,
    val expiresAt: Long
)

enum class MediaType(val prefix: String, val maxSize: Long) {
    PROFILE_PHOTO("users/profiles", 10 * 1024 * 1024),
    MESSAGE_IMAGE("messages/media", 20 * 1024 * 1024),
    MESSAGE_VIDEO("messages/media", 100 * 1024 * 1024)
}

// Full implementation in SCALABILITY_ARCHITECTURE.md
