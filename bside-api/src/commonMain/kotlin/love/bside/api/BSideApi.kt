package love.bside.api

import love.bside.api.models.User
import kotlinx.coroutines.flow.Flow

/**
 * The single entry point for the B-Side API.
 * This interface is implementation-agnostic (no PocketBase specifics).
 */
interface BSideApi {
    val auth: AuthApi
    val messaging: MessagingApi
}

interface AuthApi {
    val currentUser: Flow<User?>
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout()
}

interface MessagingApi {
    // To be populated with generic messaging methods
}
