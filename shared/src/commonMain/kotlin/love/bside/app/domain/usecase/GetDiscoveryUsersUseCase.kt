package love.bside.app.domain.usecase

import love.bside.app.core.Result
import love.bside.app.domain.models.Profile
import love.bside.app.domain.repository.ProfileRepository

class GetDiscoveryUsersUseCase(
    private val profileRepository: ProfileRepository
) {
    suspend operator fun invoke(lat: Double? = null, lng: Double? = null): Result<List<Profile>> {
        return profileRepository.getDiscoveryProfiles(lat, lng)
    }
}
