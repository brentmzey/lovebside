package love.bside.app.domain.usecase

import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.repository.AuthRepository

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(email: String, password: String, passwordConfirm: String): Result<AuthDetails> {
        logDebug("SignUpUseCase invoked for email: $email")
        return authRepository.signUp(email, password, passwordConfirm)
    }
}
