package love.bside.server.repositories

import love.bside.app.data.network.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.domain.Profile
import love.bside.server.models.db.PBProfile
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
            put("userId", userId)
            put("firstName", firstName)
            put("lastName", lastName)
            put("birthDate", birthDate)
            put("seeking", seeking)
        }
        
        return when (val result = pocketBase.create<PBProfile>(collection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> Result.Error(result.exception)
        }
    }
    
    override suspend fun getProfileByUserId(userId: String): Result<Profile?> {
        val filter = "userId = '$userId'"
        
        return when (val result = pocketBase.getList<PBProfile>(collection, filter = filter)) {
            is Result.Success -> {
                val profile = result.data.items.firstOrNull()
                Result.Success(profile?.toDomain())
            }
            is Result.Error -> Result.Error(result.exception)
        }
    }
    
    override suspend fun updateProfile(userId: String, updates: Map<String, Any>): Result<Profile> {
        // First, get the profile ID by userId
        val filter = "userId = '$userId'"
        val getResult = pocketBase.getList<PBProfile>(collection, filter = filter)
        
        return when (getResult) {
            is Result.Success -> {
                val profile = getResult.data.items.firstOrNull()
                if (profile == null) {
                    Result.Error(Exception("Profile not found"))
                } else {
                    val body = buildJsonObject {
                        updates.forEach { (key, value) ->
                            when (value) {
                                is String -> put(key, value)
                                is Boolean -> put(key, value)
                                is Number -> put(key, value.toLong())
                            }
                        }
                    }
                    
                    when (val updateResult = pocketBase.update<PBProfile>(collection, profile.id, body)) {
                        is Result.Success -> Result.Success(updateResult.data.toDomain())
                        is Result.Error -> Result.Error(updateResult.exception)
                    }
                }
            }
            is Result.Error -> Result.Error(getResult.exception)
        }
    }
    
    override suspend fun deleteProfile(userId: String): Result<Unit> {
        // First, get the profile ID by userId
        val filter = "userId = '$userId'"
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
            is Result.Error -> Result.Error(getResult.exception)
        }
    }
}
