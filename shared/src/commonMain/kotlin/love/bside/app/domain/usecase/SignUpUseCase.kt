package love.bside.app.domain.usecase

import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.domain.models.AuthDetails
import love.bside.app.domain.models.SignUpData
import love.bside.app.domain.repository.AuthRepository

class SignUpUseCase(private val authRepository: AuthRepository) {
    suspend operator fun invoke(data: SignUpData): Result<AuthDetails> {
        logDebug("SignUpUseCase invoked for email: ${data.email}")
        return authRepository.signUp(data)
    }
}
