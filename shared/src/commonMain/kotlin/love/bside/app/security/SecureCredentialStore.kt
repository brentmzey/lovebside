package love.bside.app.security

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

interface SecureCredentialStore {
    suspend fun upsert(record: SecureEnrollmentRecord)
    suspend fun get(userId: String): SecureEnrollmentRecord?
    suspend fun getDefault(): SecureEnrollmentRecord?
    suspend fun list(): List<SecureEnrollmentRecord>
    suspend fun remove(userId: String)
    suspend fun clear()
    fun updates(): Flow<List<SecureEnrollmentRecord>>
}

expect class SecureCredentialStoreFactory {
    fun create(): SecureCredentialStore
}

internal val secureRecordJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}
