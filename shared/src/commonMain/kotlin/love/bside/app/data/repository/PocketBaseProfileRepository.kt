package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray
import love.bside.app.core.AppException
import love.bside.app.core.Result
import love.bside.app.core.cache.CacheKeys
import love.bside.app.core.cache.InMemoryCache
import love.bside.app.core.logDebug
import love.bside.app.core.logInfo
import love.bside.app.data.models.toDomain
import love.bside.app.data.models.ProfileUpdateRequest
import love.bside.app.data.models.SeekingStatus as DataSeekingStatus
import love.bside.app.domain.models.Profile
import love.bside.app.domain.models.SeekingStatus
import love.bside.app.domain.repository.ProfileRepository
import love.bside.app.data.models.Profile as DataProfile

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

        return try {
            val result = pocketBase.collection("s_profiles")
                .getList(QueryOptions(filter = "userId='$userId'"))
            
            if (result.items.isEmpty()) {
                Result.Error(AppException.Business.ResourceNotFound("Profile", userId))
            } else {
                val record = result.items.first()
                val dataProfile = mapRecordToDataProfile(record.jsonObject)
                val profile = dataProfile.toDomain()
                cache.put(CacheKeys.userProfile(userId), profile)
                logInfo("Profile fetched successfully for userId: $userId")
                Result.Success(profile)
            }
        } catch (e: Exception) {
            Result.Error(AppException.Unknown("Failed to fetch profile: ${e.message}", e))
        }
    }

    override suspend fun createProfile(profile: Profile): Result<Unit> {
        return Result.Error(AppException.Unknown("Not implemented yet"))
    }

    override suspend fun updateProfile(userId: String, request: ProfileUpdateRequest): Result<Profile> {
        return try {
            val result = pocketBase.collection("s_profiles")
                .getList(QueryOptions(filter = "userId='$userId'"))
            
            if (result.items.isEmpty()) {
                Result.Error(AppException.Business.ResourceNotFound("Profile", userId))
            } else {
                val recordId = result.items.first()["id"]?.jsonPrimitive?.content 
                    ?: return Result.Error(AppException.Unknown("Profile ID not found"))
                
                val body = mutableMapOf<String, Any>()
                
                request.firstName?.let { body["firstName"] = it }
                request.lastName?.let { body["lastName"] = it }
                request.birthDate?.let { body["birthDate"] = it }
                request.bio?.let { body["bio"] = it }
                request.location?.let { body["location"] = it }
                request.aboutMe?.let { body["aboutMe"] = it }
                request.height?.let { body["height"] = it }
                request.occupation?.let { body["occupation"] = it }
                request.education?.let { body["education"] = it }
                request.interests?.let { body["interests"] = it }
                request.seeking?.let { 
                    body["seeking"] = when (it) {
                        SeekingStatus.FRIENDSHIP -> "Friendship"
                        SeekingStatus.RELATIONSHIP -> "Relationship"
                        SeekingStatus.BOTH -> "Both"
                    }
                }
                
                val updated = pocketBase.collection("s_profiles").update(recordId, body)
                val dataProfile = mapRecordToDataProfile(updated.jsonObject)
                val profile = dataProfile.toDomain()
                
                cache.put(CacheKeys.userProfile(userId), profile)
                Result.Success(profile)
            }
        } catch (e: Exception) {
            Result.Error(AppException.Unknown("Failed to update profile: ${e.message}", e))
        }
    }

    override suspend fun getDiscoveryProfiles(lat: Double?, lng: Double?): Result<List<Profile>> {
        return try {
            val result = pocketBase.collection("s_profiles")
                .getList(QueryOptions(sort = "@random", perPage = 8))
            
            val profiles = result.items.map { record ->
                val dataProfile = mapRecordToDataProfile(record.jsonObject)
                dataProfile.toDomain()
            }
            
            Result.Success(profiles)
        } catch (e: Exception) {
            Result.Error(AppException.Unknown("Failed to fetch discovery profiles: ${e.message}", e))
        }
    }
    
    private fun mapRecordToDataProfile(json: kotlinx.serialization.json.JsonObject): DataProfile {
        fun getString(key: String): String = json[key]?.jsonPrimitive?.content ?: ""
        fun getStringOrNull(key: String): String? = json[key]?.jsonPrimitive?.content
        fun getDouble(key: String): Double? = json[key]?.jsonPrimitive?.content?.toDoubleOrNull()
        fun getInt(key: String): Int? = json[key]?.jsonPrimitive?.content?.toIntOrNull()
        fun getList(key: String): List<String>? = json[key]?.jsonArray?.mapNotNull { it.jsonPrimitive.content }
        
        val seekingStr = getString("seeking")
        val seeking = when (seekingStr) {
            "Friendship" -> DataSeekingStatus.FRIENDSHIP
            "Relationship" -> DataSeekingStatus.RELATIONSHIP
            else -> DataSeekingStatus.BOTH
        }
        
        return DataProfile(
            id = getString("id"),
            collectionId = getString("collectionId"),
            collectionName = getString("collectionName"),
            created = getString("created"),
            updated = getString("updated"),
            userId = getString("userId"),
            firstName = getString("firstName"),
            lastName = getString("lastName"),
            birthDate = getString("birthDate"),
            bio = getStringOrNull("bio"),
            location = getStringOrNull("location"),
            lat = getDouble("lat"),
            lng = getDouble("lng"),
            seeking = seeking,
            profilePicture = getStringOrNull("profilePicture"),
            photos = getList("photos"),
            aboutMe = getStringOrNull("aboutMe"),
            height = getInt("height"),
            occupation = getStringOrNull("occupation"),
            education = getStringOrNull("education"),
            interests = getList("interests")
        )
    }
}
