package love.bside.app.domain.repository

import love.bside.app.core.Result
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.domain.models.Profile

interface ProfileRepository {
    suspend fun getProfile(userId: String): Result<Profile>
    suspend fun createProfile(profile: Profile): Result<Unit>
    suspend fun updateProfile(userId: String, request: ProfileUpdateRequest): Result<Profile>
}
