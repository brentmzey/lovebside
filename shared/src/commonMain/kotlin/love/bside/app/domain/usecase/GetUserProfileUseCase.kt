package love.bside.app.domain.usecase

import love.bside.app.core.Result
import love.bside.app.core.logDebug
import love.bside.app.domain.models.Profile
import love.bside.app.domain.repository.ProfileRepository

class GetUserProfileUseCase(private val profileRepository: ProfileRepository) {
    suspend operator fun invoke(userId: String): Result<Profile> {
        logDebug("GetUserProfileUseCase invoked for userId: $userId")
        return profileRepository.getProfile(userId)
    }
}
