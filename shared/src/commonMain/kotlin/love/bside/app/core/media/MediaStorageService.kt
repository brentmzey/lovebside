package love.bside.app.core.media

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Media Storage Service - Common interface for all platforms
 */
interface MediaStorageService {
    suspend fun uploadMedia(media: MediaUpload): Either<MediaError, MediaUrl>
    suspend fun uploadMediaBatch(mediaList: List<MediaUpload>): Either<MediaError, List<MediaUrl>>
    suspend fun getSignedUrl(mediaId: String, expirySeconds: Long = 3600): Either<MediaError, String>
    suspend fun deleteMedia(mediaId: String): Either<MediaError, Unit>
    suspend fun getMediaMetadata(mediaId: String): Either<MediaError, MediaMetadata>
    fun streamMedia(mediaId: String): Flow<ByteArray>
    suspend fun mediaExists(mediaId: String): Boolean
}

data class MediaUpload(
    val fileName: String,
    val content: ByteArray,
    val mimeType: String,
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as MediaUpload
        if (fileName != other.fileName) return false
        if (!content.contentEquals(other.content)) return false
        if (mimeType != other.mimeType) return false
        return metadata == other.metadata
    }
    
    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + content.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

data class MediaUrl(
    val url: String,
    val cdnUrl: String? = null,
    val mediaId: String,
    val expiresAt: Instant? = null
)

data class MediaMetadata(
    val id: String,
    val fileName: String,
    val mimeType: String,
    val size: Long,
    val uploadedAt: Instant,
    val etag: String? = null,
    val customMetadata: Map<String, String> = emptyMap()
)

sealed class MediaError {
    data class UploadFailed(val message: String, val cause: Throwable? = null) : MediaError()
    data class DownloadFailed(val message: String, val cause: Throwable? = null) : MediaError()
    data class NotFound(val mediaId: String) : MediaError()
    data class InvalidMedia(val message: String) : MediaError()
    data class StorageQuotaExceeded(val message: String) : MediaError()
    data class NetworkError(val message: String, val cause: Throwable? = null) : MediaError()
    data class Unauthorized(val message: String) : MediaError()
    data class Unknown(val message: String, val cause: Throwable? = null) : MediaError()
}

enum class MediaType(val mimeTypes: List<String>) {
    IMAGE(listOf("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/heic")),
    VIDEO(listOf("video/mp4", "video/quicktime", "video/webm", "video/x-matroska")),
    AUDIO(listOf("audio/mpeg", "audio/mp4", "audio/wav", "audio/webm"));
    
    companion object {
        fun fromMimeType(mimeType: String) = entries.firstOrNull { it.mimeTypes.any { m -> m.equals(mimeType, true) } }
        fun isSupported(mimeType: String) = fromMimeType(mimeType) != null
    }
}
