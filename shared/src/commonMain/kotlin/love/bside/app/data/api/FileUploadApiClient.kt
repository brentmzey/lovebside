package love.bside.app.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.utils.io.core.*
import love.bside.app.core.Result
import love.bside.app.core.appConfig
import love.bside.app.data.models.Profile
import love.bside.app.data.storage.TokenStorage

/**
 * API client for file upload operations
 * Handles profile pictures, photo galleries, and other file uploads to PocketBase
 */
class FileUploadApiClient(
    private val client: HttpClient,
    private val tokenStorage: TokenStorage
) {
    private val config = appConfig()
    private val baseUrl = when (config.environment) {
        love.bside.app.core.Environment.DEVELOPMENT -> "http://localhost:8090"
        love.bside.app.core.Environment.STAGING -> "https://staging.bside.love"
        love.bside.app.core.Environment.PRODUCTION -> "https://www.bside.love"
    }

    /**
     * Upload profile picture
     * @param profileId The profile ID to update
     * @param imageData Raw image bytes
     * @param filename Original filename (e.g., "avatar.jpg")
     * @param mimeType MIME type of the image (e.g., "image/jpeg")
     * @return Updated profile with new picture URL
     */
    suspend fun uploadProfilePicture(
        profileId: String,
        imageData: ByteArray,
        filename: String,
        mimeType: String = "image/jpeg"
    ): Result<Profile> = safeApiCall {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/api/collections/s_profiles/records/$profileId",
            formData = formData {
                // Add the file
                append("profilePicture", imageData, Headers.build {
                    append(HttpHeaders.ContentType, mimeType)
                    append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                })
            }
        ) {
            method = HttpMethod.Patch
            bearerAuth(tokenStorage.getToken() ?: "")
        }
        response.body<Profile>()
    }

    /**
     * Upload multiple photos to gallery
     * @param profileId The profile ID to update
     * @param images List of image data with metadata
     * @return Updated profile with new photo URLs
     */
    suspend fun uploadPhotos(
        profileId: String,
        images: List<ImageUpload>
    ): Result<Profile> = safeApiCall {
        val response = client.submitFormWithBinaryData(
            url = "$baseUrl/api/collections/s_profiles/records/$profileId",
            formData = formData {
                images.forEach { image ->
                    append("photos", image.data, Headers.build {
                        append(HttpHeaders.ContentType, image.mimeType)
                        append(HttpHeaders.ContentDisposition, "filename=\"${image.filename}\"")
                    })
                }
            }
        ) {
            method = HttpMethod.Patch
            bearerAuth(tokenStorage.getToken() ?: "")
        }
        response.body<Profile>()
    }

    /**
     * Delete a photo from gallery
     * @param profileId The profile ID
     * @param filename The filename to delete
     * @return Updated profile
     */
    suspend fun deletePhoto(
        profileId: String,
        filename: String
    ): Result<Profile> = safeApiCall {
        // To delete a specific file from a multi-file field, we need to:
        // 1. Get current profile
        // 2. Remove the filename from photos array
        // 3. Update with new array
        
        val currentProfile = client.get("$baseUrl/api/collections/s_profiles/records/$profileId") {
            bearerAuth(tokenStorage.getToken() ?: "")
        }.body<Profile>()
        
        val updatedPhotos = currentProfile.photos?.filter { it != filename } ?: emptyList()
        
        client.patch("$baseUrl/api/collections/s_profiles/records/$profileId") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("photos" to updatedPhotos))
        }.body<Profile>()
    }

    /**
     * Delete profile picture
     * @param profileId The profile ID
     * @return Updated profile
     */
    suspend fun deleteProfilePicture(profileId: String): Result<Profile> = safeApiCall {
        client.patch("$baseUrl/api/collections/s_profiles/records/$profileId") {
            bearerAuth(tokenStorage.getToken() ?: "")
            contentType(ContentType.Application.Json)
            setBody(mapOf("profilePicture" to null))
        }.body<Profile>()
    }

    /**
     * Get file URL for display
     * @param collectionId Collection ID
     * @param recordId Record ID
     * @param filename Filename
     * @param thumb Optional thumbnail size (e.g., "100x100", "512x512")
     * @return Full URL to the file
     */
    fun getFileUrl(
        collectionId: String,
        recordId: String,
        filename: String,
        thumb: String? = null
    ): String {
        val thumbParam = thumb?.let { "?thumb=$it" } ?: ""
        return "$baseUrl/api/files/$collectionId/$recordId/$filename$thumbParam"
    }

    private suspend inline fun <reified T> safeApiCall(crossinline call: suspend () -> T): Result<T> {
        return try {
            Result.Success(call())
        } catch (e: Exception) {
            Result.Error(love.bside.app.core.AppException.Network.ServerError(
                statusCode = 500,
                message = e.message ?: "File upload failed"
            ))
        }
    }
}

/**
 * Represents an image to be uploaded
 */
data class ImageUpload(
    val data: ByteArray,
    val filename: String,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageUpload

        if (!data.contentEquals(other.data)) return false
        if (filename != other.filename) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + filename.hashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}
