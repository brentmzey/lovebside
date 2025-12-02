package love.bside.app.security.usecase

import kotlinx.coroutines.flow.Flow
import love.bside.app.security.SecureAuthManager
import love.bside.app.security.SecureEnrollmentRecord

class ObserveSecureEnrollmentsUseCase(
    private val secureAuthManager: SecureAuthManager
) {
    operator fun invoke(): Flow<List<SecureEnrollmentRecord>> = secureAuthManager.enrollments
}
