package love.bside.app.data.models

/**
 * Platform-agnostic wrapper for file attachments.
 * Used to pass file data from UI/Platform code to the Repository.
 */
data class Attachment(
    val fileName: String,
    val data: ByteArray,
    val mimeType: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as Attachment

        if (fileName != other.fileName) return false
        if (!data.contentEquals(other.data)) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (mimeType?.hashCode() ?: 0)
        return result
    }
}
