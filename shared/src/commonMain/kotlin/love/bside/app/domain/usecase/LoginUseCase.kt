package love.bside.app.domain.usecase

import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.repository.AuthRepository

class LoginUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String): Result<AuthDetails> {
        logDebug("LoginUseCase invoked for email: $email")
        return authRepository.login(email, password)
    }
}
