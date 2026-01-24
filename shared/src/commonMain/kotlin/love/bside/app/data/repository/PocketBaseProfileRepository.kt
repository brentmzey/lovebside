package love.bside.app.data.repository

import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import io.ktor.client.request.forms.*
import io.ktor.http.*
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
import love.bside.app.data.DatabaseCollections
import love.bside.app.data.mapPocketBaseError

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
            val result = pocketBase.collection(DatabaseCollections.S_PROFILES)
                .getList(QueryOptions(filter = "user_id='$userId'"))
            
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
            Result.Error(mapPocketBaseError("fetch profile for userId: $userId", e))
        }
    }

    override suspend fun createProfile(profile: Profile): Result<Unit> {
        return Result.Error(AppException.Unknown("Not implemented yet"))
    }

    override suspend fun updateProfile(userId: String, request: ProfileUpdateRequest): Result<Profile> {
        return try {
            val result = pocketBase.collection(DatabaseCollections.S_PROFILES)
                .getList(QueryOptions(filter = "user_id='$userId'"))
            
            if (result.items.isEmpty()) {
                Result.Error(AppException.Business.ResourceNotFound("Profile", userId))
            } else {
                val recordId = result.items.first()["id"]?.jsonPrimitive?.content 
                    ?: return Result.Error(AppException.Unknown("Profile ID not found"))
                
                val body = mutableMapOf<String, Any>()
                
                request.firstName?.let { body["first_name"] = it }
                request.lastName?.let { body["last_name"] = it }
                request.birthDate?.let { body["birth_date"] = it }
                request.bio?.let { body["bio"] = it }
                request.location?.let { body["location"] = it }
                request.aboutMe?.let { body["about_me"] = it }
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
                
                val updated = pocketBase.collection(DatabaseCollections.S_PROFILES).update(recordId, body)
                val dataProfile = mapRecordToDataProfile(updated.jsonObject)
                val profile = dataProfile.toDomain()
                
                cache.put(CacheKeys.userProfile(userId), profile)
                Result.Success(profile)
            }
        } catch (e: Exception) {
            Result.Error(mapPocketBaseError("update profile", e))
        }
    }

    override suspend fun getDiscoveryProfiles(lat: Double?, lng: Double?): Result<List<Profile>> {
        return try {
            val result = pocketBase.collection(DatabaseCollections.S_PROFILES)
                .getList(QueryOptions(sort = "@random", perPage = 8))
            
            val profiles = result.items.map { record ->
                val dataProfile = mapRecordToDataProfile(record.jsonObject)
                dataProfile.toDomain()
            }
            
            Result.Success(profiles)
        } catch (e: Exception) {
            Result.Error(mapPocketBaseError("fetch discovery profiles", e))
        }
    }

    override suspend fun uploadProfilePicture(userId: String, data: ByteArray, filename: String): Result<Profile> {
        return uploadFile(userId, "profile_picture", data, filename)
    }

    override suspend fun uploadVideo(userId: String, data: ByteArray, filename: String): Result<Profile> {
        return uploadFile(userId, "videos", data, filename)
    }

    private suspend fun uploadFile(userId: String, field: String, data: ByteArray, filename: String): Result<Profile> {
        return try {
            val result = pocketBase.collection(DatabaseCollections.S_PROFILES)
                .getList(QueryOptions(filter = "user_id='$userId'"))
            
            // Safe JSON ID extraction
            val recordId = result.items.firstOrNull()?.get("id")?.jsonPrimitive?.content
                ?: return Result.Error(AppException.Business.ResourceNotFound("Profile", userId))

            val multipartBody = MultiPartFormDataContent(
                formData {
                    append(field, data, Headers.build {
                        append(HttpHeaders.ContentDisposition, "form-data; name=\"$field\"; filename=\"$filename\"")
                    })
                }
            )

            val updated = pocketBase.collection(DatabaseCollections.S_PROFILES)
                .update(recordId, multipartBody)

            val dataProfile = mapRecordToDataProfile(updated.jsonObject)
            Result.Success(dataProfile.toDomain())
        } catch (e: Exception) {
            Result.Error(mapPocketBaseError("upload $field for userId: $userId", e))
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
            created = getString("created").let { raw ->
                if (raw.isBlank()) kotlinx.datetime.Instant.fromEpochMilliseconds(0)
                else kotlinx.datetime.Instant.parse(raw.replace(" ", "T").let { if (!it.endsWith("Z")) "${it}Z" else it })
            },
            updated = getString("updated").let { raw ->
                if (raw.isBlank()) kotlinx.datetime.Instant.fromEpochMilliseconds(0)
                else kotlinx.datetime.Instant.parse(raw.replace(" ", "T").let { if (!it.endsWith("Z")) "${it}Z" else it })
            },
            userId = getString("user_id"),
            firstName = getString("first_name"),
            lastName = getString("last_name"),
            birthDate = getString("birth_date"),
            bio = getStringOrNull("bio"),
            location = getStringOrNull("location"),
            lat = getDouble("lat"),
            lng = getDouble("lng"),
            seeking = seeking,
            profilePicture = getStringOrNull("profile_picture"),
            photos = getList("photos"),
            videos = getList("videos"),
            aboutMe = getStringOrNull("about_me"),
            height = getInt("height"),
            occupation = getStringOrNull("occupation"),
            education = getStringOrNull("education"),
            interests = getList("interests")
        )
    }
}
