package love.bside.server.repositories

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.app.core.AppException
import love.bside.server.models.domain.Profile
import love.bside.server.models.db.PBProfile
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import love.bside.app.utils.CompressionService
import arrow.core.toOption

/**
 * Repository interface for profile operations
 */
interface ProfileRepository {
    suspend fun createProfile(userId: String, firstName: String, lastName: String, birthDate: String, seeking: String): Result<Profile>
    suspend fun getProfileByUserId(userId: String): Result<Profile?>
    suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Profile>
    suspend fun deleteProfile(userId: String): Result<Unit>
}

/**
 * Implementation using PocketBase
 */
class ProfileRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : ProfileRepository {
    
    private val collection = "s_profiles"
    
    override suspend fun createProfile(
        userId: String,
        firstName: String,
        lastName: String,
        birthDate: String,
        seeking: String
    ): Result<Profile> {
        val body = buildJsonObject {
            put("user", userId)
            put("firstName", firstName)
            put("lastName", lastName)
            put("birthDate", birthDate)
            put("seeking", seeking)
        }
        
        return when (val result = pocketBase.create<JsonObject, PBProfile>(collection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getProfileByUserId(userId: String): Result<Profile?> {
        val filter = "user = '$userId'"
        
        return when (val result = pocketBase.getList<PBProfile>(collection, filter = filter)) {
            is Result.Success -> {
                val profile = result.data.items.firstOrNull()
                Result.Success(profile?.toDomain())
            }
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Profile> {
        // First, get the profile ID by userId
        val filter = "user = '$userId'"
        val getResult = pocketBase.getList<PBProfile>(collection, filter = filter)
        
        return when (getResult) {
            is Result.Success -> {
                val profile = getResult.data.items.firstOrNull()
                if (profile == null) {
                    Result.Error(AppException.Business.ResourceNotFound(collection, userId))
                } else {
                    val body = buildJsonObject {
                        for ((key, value) in updates) {
                            when (key) {
                                "bio" -> {
                                    val bioStr = value as? String ?: ""
                                    put("bio", bioStr)
                                    // Extreme compression
                                    val compressed = CompressionService.compressToBase64(bioStr.toOption())
                                    if (compressed.isSome()) {
                                        put("bioBrotliBase64", (compressed as arrow.core.Some).value)
                                    }
                                }
                                "location" -> {
                                    val locStr = value as? String ?: ""
                                    put("location", locStr)
                                    // Extreme compression
                                    val compressed = CompressionService.compressToBase64(locStr.toOption())
                                    if (compressed.isSome()) {
                                        put("locationBrotliBase64", (compressed as arrow.core.Some).value)
                                    }
                                }
                                else -> {
                                    when (value) {
                                        is String -> put(key, value)
                                        is Boolean -> put(key, value)
                                        is Number -> put(key, value.toLong())
                                    }
                                }
                            }
                        }
                    }
                    
                    when (val updateResult = pocketBase.update<JsonObject, PBProfile>(collection, profile.id, body)) {
                        is Result.Success -> Result.Success(updateResult.data.toDomain())
                        is Result.Error -> updateResult
                        is Result.Loading -> updateResult
                    }
                }
            }
            is Result.Error -> getResult
            is Result.Loading -> getResult
        }
    }
    
    override suspend fun deleteProfile(userId: String): Result<Unit> {
        // First, get the profile ID by userId
        val filter = "user = '$userId'"
        val getResult = pocketBase.getList<PBProfile>(collection, filter = filter)
        
        return when (getResult) {
            is Result.Success -> {
                val profile = getResult.data.items.firstOrNull()
                if (profile == null) {
                    Result.Success(Unit) // Already deleted
                } else {
                    pocketBase.delete(collection, profile.id)
                }
            }
            is Result.Error -> getResult
            is Result.Loading -> getResult
        }
    }
}
