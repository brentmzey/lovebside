package love.bside.app.security.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import love.bside.app.security.SecureCredentialStore
import love.bside.app.security.SecureEnrollmentRecord
import love.bside.app.security.secureRecordJson

internal class AndroidSecureCredentialStore(
    context: Context
) : SecureCredentialStore {
    private val prefs: SharedPreferences
    private val state = MutableStateFlow<List<SecureEnrollmentRecord>>(emptyList())

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            STORE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        state.value = snapshot()
    }

    override suspend fun upsert(record: SecureEnrollmentRecord) {
        prefs.edit()
            .putString(recordKey(record.userId), secureRecordJson.encodeToString(record))
            .putString(DEFAULT_KEY, record.userId)
            .apply()
        state.value = snapshot()
    }

    override suspend fun get(userId: String): SecureEnrollmentRecord? = readRecord(userId)

    override suspend fun getDefault(): SecureEnrollmentRecord? {
        val userId = prefs.getString(DEFAULT_KEY, null)
        return userId?.let { readRecord(it) } ?: state.value.lastOrNull()
    }

    override suspend fun list(): List<SecureEnrollmentRecord> = state.value

    override suspend fun remove(userId: String) {
        prefs.edit().remove(recordKey(userId)).apply()
        if (prefs.getString(DEFAULT_KEY, null) == userId) {
            prefs.edit().remove(DEFAULT_KEY).apply()
        }
        state.value = snapshot()
    }

    override suspend fun clear() {
        prefs.edit().clear().apply()
        state.value = emptyList()
    }

    override fun updates(): Flow<List<SecureEnrollmentRecord>> = state.asStateFlow()

    private fun snapshot(): List<SecureEnrollmentRecord> = prefs.all
        .keys
        .filter { it.startsWith(RECORD_PREFIX) }
        .mapNotNull { key ->
            prefs.getString(key, null)?.let { secureRecordJson.decodeFromString<SecureEnrollmentRecord>(it) }
        }
        .sortedBy { it.updatedAt }

    private fun readRecord(userId: String): SecureEnrollmentRecord? =
        prefs.getString(recordKey(userId), null)?.let { secureRecordJson.decodeFromString(it) }

    private fun recordKey(userId: String) = "$RECORD_PREFIX$userId"

    companion object {
        private const val STORE_NAME = "secure_auth_records"
        private const val RECORD_PREFIX = "secure_record_"
        private const val DEFAULT_KEY = "secure_record_default"
    }
}
