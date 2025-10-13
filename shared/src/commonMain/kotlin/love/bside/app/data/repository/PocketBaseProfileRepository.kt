package love.bside.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.cache.CacheKeys
import love.bside.app.core.cache.InMemoryCache
import love.bside.app.core.logDebug
import love.bside.app.core.logInfo
import love.bside.app.core.network.retryable
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.data.models.toDomain
import love.bside.app.domain.models.Profile
import love.bside.app.domain.repository.ProfileRepository

@Serializable
data class ProfileListResponse(val items: List<love.bside.app.data.models.Profile>)

@Serializable
data class ProfileCreateRequest(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val seeking: String
)

class PocketBaseProfileRepository(
    private val client: HttpClient,
    private val cache: InMemoryCache<String, Profile> = InMemoryCache()
) : ProfileRepository {

    override suspend fun getProfile(userId: String): Result<Profile> {
        logDebug("Fetching profile for userId: $userId")
        
        // Check cache first
        cache.get(CacheKeys.userProfile(userId))?.let {
            logDebug("Profile found in cache for userId: $userId")
            return Result.Success(it)
        }

        return retryable {
            val response = client.get("collections/s_profiles/records?filter=(userId='$userId')")
                .body<ProfileListResponse>()
            
            if (response.items.isEmpty()) {
                throw AppException.Business.ResourceNotFound("Profile", userId)
            }
            
            val profile = response.items.first().toDomain()
            cache.put(CacheKeys.userProfile(userId), profile)
            logInfo("Profile fetched successfully for userId: $userId")
            profile
        }
    }

    override suspend fun createProfile(profile: Profile): Result<Unit> {
        logInfo("Creating profile for userId: ${profile.userId}")
        
        return retryable {
            val request = ProfileCreateRequest(
                userId = profile.userId,
                firstName = profile.firstName,
                lastName = profile.lastName,
                birthDate = profile.birthDate.toString(),
                seeking = profile.seeking.name
            )
            client.post("collections/s_profiles/records") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            cache.put(CacheKeys.userProfile(profile.userId), profile)
            logInfo("Profile created successfully for userId: ${profile.userId}")
        }
    }

    override suspend fun updateProfile(userId: String, request: ProfileUpdateRequest): Result<Profile> {
        logInfo("Updating profile for userId: $userId")
        
        return retryable {
            // First, get the record ID for the user's profile
            val records = client.get("collections/s_profiles/records?filter=(userId='$userId')")
                .body<ProfileListResponse>()
            
            if (records.items.isEmpty()) {
                throw AppException.Business.ResourceNotFound("Profile", userId)
            }
            
            val recordId = records.items.first().id

            val updatedDto = client.patch("collections/s_profiles/records/$recordId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body<love.bside.app.data.models.Profile>()
            
            val updatedProfile = updatedDto.toDomain()
            cache.put(CacheKeys.userProfile(userId), updatedProfile)
            logInfo("Profile updated successfully for userId: $userId")
            updatedProfile
        }
    }
}
