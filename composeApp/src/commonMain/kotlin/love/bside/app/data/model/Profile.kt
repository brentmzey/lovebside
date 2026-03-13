package love.bside.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String = "",
    val user: String,
    val displayName: String? = null,
    val bio: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    val location: String? = null,
    val seekingRelationship: String? = null, // "friendship", "romantic", "both"
    val avatar: String? = null,
    val photos: List<String>? = null,
    val proustCompleted: Boolean = false,
    val profileComplete: Boolean = false,
    val matchingEnabled: Boolean = false,
    val version: Int = 0,
    val created: String = "",
    val updated: String = ""
)

@Serializable
data class ProfileUpdate(
    val displayName: String? = null,
    val bio: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val pronouns: String? = null,
    val location: String? = null,
    val seekingRelationship: String? = null,
    val avatar: String? = null,
    val photos: List<String>? = null,
    val proustCompleted: Boolean? = null,
    val profileComplete: Boolean? = null,
    val matchingEnabled: Boolean? = null,
    val version: Int? = null
)
