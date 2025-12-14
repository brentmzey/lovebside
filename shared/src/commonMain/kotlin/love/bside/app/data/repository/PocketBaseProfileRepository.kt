package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.functional.getListTyped
import io.pocketbase.models.QueryOptions
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.cache.CacheKeys
import love.bside.app.core.cache.InMemoryCache
import love.bside.app.core.logDebug
import love.bside.app.core.logInfo
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.repository.ProfileRepository
import io.pocketbase.types.Profile as SdkProfile

class PocketBaseProfileRepository(
    private val pocketBase: PocketBase,
    private val cache: InMemoryCache<String, Profile> = InMemoryCache()
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<Profile> {
        logDebug("Fetching profile for userId: $userId")
        
        cache.get(CacheKeys.userProfile(userId))?.let {
            logDebug("Profile found in cache for userId: $userId")
            return Result.Success(it)
        }

        val result = pocketBase.collection("s_profiles")
            .getListTyped<SdkProfile>(
                QueryOptions(filter = "userId='$userId'")
            )

        return result.fold(
            ifLeft = { error ->
                Result.Error(AppException.Network.ServerError(error.statusCode, error.message ?: "Unknown remote error"))
            },
            ifRight = { listResult ->
                if (listResult.items.isEmpty()) {
                    Result.Error(AppException.Business.ResourceNotFound("Profile", userId))
                } else {
                    val sdkProfile = listResult.items.first()
                    val profile = sdkProfile.toDomain()
                    cache.put(CacheKeys.userProfile(userId), profile)
                    logInfo("Profile fetched successfully for userId: $userId")
                    Result.Success(profile)
                }
            }
        )
    }

    override suspend fun createProfile(profile: Profile): Result<Unit> {
        return Result.Error(AppException.Unknown("Not implemented yet"))
    }



    override suspend fun updateProfile(userId: String, request: ProfileUpdateRequest): Result<Profile> {
        // Fetch first to get ID
        val result = pocketBase.collection("s_profiles")
            .getListTyped<SdkProfile>(
                QueryOptions(filter = "userId='$userId'")
            )
            
        return result.fold(
            ifLeft = { Result.Error(AppException.Network.ServerError(it.statusCode, it.message ?: "Unknown remote error")) },
            ifRight = { listResult ->
                if (listResult.items.isEmpty()) {
                    Result.Error(AppException.Business.ResourceNotFound("Profile", userId))
                } else {
                    val recordId = listResult.items.first().id
                    val body = mutableMapOf<String, Any>()
                    
                    request.firstName?.let { body["firstName"] = it }
                    request.middle?.let { body["middle"] = it }
                    request.lastName?.let { body["lastName"] = it }
                    request.bio?.let { body["bio"] = it }
                    request.location?.let { body["location"] = it }
                    request.seeking?.let { body["seeking"] = it.name }
                    
                    // New fields
                    request.aboutMe?.let { body["aboutMe"] = it }
                    request.height?.let { body["height"] = it }
                    request.occupation?.let { body["occupation"] = it }
                    request.education?.let { body["education"] = it }
                    request.interests?.let { body["interests"] = it }
                    
                    // Note: file uploads (profilePicture, photos) are usually handled separately or 
                    // need MultipartBody which requires more complex logic. 
                    // For now we assume text/numeric updates only in this method.
                    
                    try {
                        pocketBase.collection("s_profiles")
                            .update(recordId, body)
                            
                        // ideally we'd map jsonUpdate back to Profile
                        getProfile(userId) 
                    } catch (e: Exception) {
                        Result.Error(AppException.Network.ServerError(500, e.message ?: "Update failed"))
                    }
                }
            }
        )
    }


    private fun SdkProfile.toDomain(): Profile {
        return Profile(
            id = this.id,
            created = Instant.parse(this.created),
            updated = Instant.parse(this.updated),
            userId = this.userId,
            firstName = this.firstName,
            middle = this.middle,
            lastName = this.lastName,
            birthDate = this.birthDate.toLocalDateTime(TimeZone.UTC).date,
            bio = this.bio,
            location = this.location,
            seeking = try {
                SeekingStatus.valueOf(this.seeking.uppercase())
            } catch (e: Exception) {
                SeekingStatus.BOTH 
            },
            profilePicture = this.profilePicture,
            photos = this.photos,
            aboutMe = this.aboutMe,
            height = this.height,
            occupation = this.occupation,
            education = this.education,
            interests = this.interests
        )
    }
}
