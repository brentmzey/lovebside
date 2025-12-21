package love.bside.api.models

import kotlinx.serialization.Serializable

/**
 * A pure data representation of a User in the B-Side system.
 * Decoupled from specific backend implementations (e.g. PocketBase RecordModel).
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val username: String,
    val avatarUrl: String? = null,
    val lastActive: String? = null
)
