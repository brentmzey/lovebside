package love.bside.app.platform

/**
 * Platform-specific image picker interface
 * Implementations for Android, iOS, Web, and JVM
 */
interface ImagePicker {
    /**
     * Pick a single image from gallery/camera
     * @param source Source to pick from (gallery, camera, or both)
     * @return ImagePickerResult with image data or error
     */
    suspend fun pickImage(source: ImageSource = ImageSource.GALLERY): ImagePickerResult
    
    /**
     * Pick multiple images from gallery
     * @param maxImages Maximum number of images to select
     * @return List of ImagePickerResult
     */
    suspend fun pickMultipleImages(maxImages: Int = 10): List<ImagePickerResult>
}

/**
 * Source for image selection
 */
enum class ImageSource {
    GALLERY,    // Pick from photo library/gallery
    CAMERA,     // Take new photo with camera
    BOTH        // Show options for both
}

/**
 * Result from image picker
 */
sealed class ImagePickerResult {
    data class Success(
        val data: ByteArray,
        val filename: String,
        val mimeType: String = "image/jpeg",
        val width: Int? = null,
        val height: Int? = null
    ) : ImagePickerResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as Success

            if (!data.contentEquals(other.data)) return false
            if (filename != other.filename) return false
            if (mimeType != other.mimeType) return false
            if (width != other.width) return false
            if (height != other.height) return false

            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + filename.hashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + (width ?: 0)
            result = 31 * result + (height ?: 0)
            return result
        }
    }
    
    data class Error(val message: String) : ImagePickerResult()
    object Cancelled : ImagePickerResult()
}

/**
 * Image compression settings
 */
data class ImageCompressionOptions(
    val maxWidth: Int = 2048,
    val maxHeight: Int = 2048,
    val quality: Int = 85,  // 0-100
    val format: ImageFormat = ImageFormat.JPEG
)

enum class ImageFormat {
    JPEG,
    PNG,
    WEBP
}

/**
 * Helper to get MIME type from format
 */
fun ImageFormat.toMimeType(): String = when (this) {
    ImageFormat.JPEG -> "image/jpeg"
    ImageFormat.PNG -> "image/png"
    ImageFormat.WEBP -> "image/webp"
}
