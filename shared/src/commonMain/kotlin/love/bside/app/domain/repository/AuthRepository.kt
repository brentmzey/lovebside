package love.bside.app.domain.repository

import love.bside.app.core.Result
import love.bside.app.domain.models.AuthDetails

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthDetails>
    suspend fun signUp(email: String, password: String, passwordConfirm: String): Result<AuthDetails>
    suspend fun logout()
}
