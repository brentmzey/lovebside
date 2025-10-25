package love.bside.server.repositories

import love.bside.app.data.api.PocketBaseClient
import love.bside.app.core.Result
import love.bside.server.models.domain.User
import love.bside.server.models.db.PBUser
import love.bside.server.utils.toDomain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import love.bside.app.core.AppException

/**
 * Repository interface for user operations
 */
interface UserRepository {
    suspend fun createUser(email: String, password: String): Result<User>
    suspend fun getUserById(id: String): Result<User>
    suspend fun getUserByEmail(email: String): Result<User?>
    suspend fun updateUser(id: String, updates: Map<String, Any>): Result<User>
    suspend fun deleteUser(id: String): Result<Unit>
    suspend fun authenticate(email: String, password: String): Result<Pair<String, User>>
}

/**
 * Implementation using PocketBase
 */
class UserRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : UserRepository {
    
    private val collection = "users"
    
    override suspend fun createUser(email: String, password: String): Result<User> {
        val body = buildJsonObject {
            put("email", email)
            put("password", password)
            put("passwordConfirm", password)
        }
        
        return when (val result = pocketBase.create<JsonObject, PBUser>(collection, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getUserById(id: String): Result<User> {
        return when (val result = pocketBase.getOne<PBUser>(collection, id)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun getUserByEmail(email: String): Result<User?> {
        val filter = "email = '$email'"
        
        return when (val result = pocketBase.getList<PBUser>(collection, filter = filter)) {
            is Result.Success -> {
                val user = result.data.items.firstOrNull()
                Result.Success(user?.toDomain())
            }
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun updateUser(id: String, updates: Map<String, Any>): Result<User> {
        val body = buildJsonObject {
            updates.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Boolean -> put(key, value)
                    is Number -> put(key, value.toLong())
                }
            }
        }
        
        return when (val result = pocketBase.update<JsonObject, PBUser>(collection, id, body)) {
            is Result.Success -> Result.Success(result.data.toDomain())
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
    
    override suspend fun deleteUser(id: String): Result<Unit> {
        return pocketBase.delete(collection, id)
    }
    
    override suspend fun authenticate(email: String, password: String): Result<Pair<String, User>> {
        return when (val result = pocketBase.authWithPassword<PBUser>(collection, email, password)) {
            is Result.Success -> {
                // PocketBase returns token and user record
                val token = result.data.token
                val user = result.data.record.toDomain()
                Result.Success(Pair(token, user))
            }
            is Result.Error -> result
            is Result.Loading -> result
        }
    }
}
