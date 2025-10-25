package love.bside.app.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String, // from PocketBase
    @SerialName("collectionId")
    val collectionId: String,
    @SerialName("collectionName")
    val collectionName: String,
    val created: String, // Can be mapped to Instant later
    val updated: String,
    @SerialName("userId")
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String, // Can be mapped to LocalDate later
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus,
    
    // Extended profile fields
    val profilePicture: String? = null,  // Filename from PocketBase
    val photos: List<String>? = null,    // List of filenames
    val aboutMe: String? = null,
    val height: Int? = null,             // Height in cm
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String>? = null
) {
    /**
     * Get full URL for profile picture thumbnail
     */
    fun getProfilePictureUrl(baseUrl: String, size: String = "512x512"): String? {
        return profilePicture?.let {
            "$baseUrl/api/files/$collectionId/$id/$it?thumb=$size"
        }
    }
    
    /**
     * Get full URLs for photo gallery
     */
    fun getPhotoUrls(baseUrl: String, size: String = "800x800"): List<String> {
        return photos?.map { filename ->
            "$baseUrl/api/files/$collectionId/$id/$filename?thumb=$size"
        } ?: emptyList()
    }
    
    /**
     * Get display name
     */
    fun getDisplayName(): String = "$firstName $lastName"
    
    /**
     * Get age from birth date (simplified - you may want to use proper date library)
     */
    fun getAge(): Int? {
        // TODO: Calculate age from birthDate using kotlinx-datetime
        return null
    }
}

@Serializable
enum class SeekingStatus {
    @SerialName("Friendship")
    FRIENDSHIP,

    @SerialName("Relationship")
    RELATIONSHIP,

    @SerialName("Both")
    BOTH
}
