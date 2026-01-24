package love.bside.app.domain.repository

import love.bside.app.core.Result
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.SignUpData

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<AuthDetails>
    suspend fun signUp(data: SignUpData): Result<AuthDetails>
    suspend fun logout()
    suspend fun getCurrentUserId(): String?
}
