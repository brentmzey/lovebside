package love.bside.app.security

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class InMemorySecureCredentialStore : SecureCredentialStore {
    private val records = LinkedHashMap<String, SecureEnrollmentRecord>()
    private val state = MutableStateFlow<List<SecureEnrollmentRecord>>(emptyList())
    private var defaultId: String? = null

    override suspend fun upsert(record: SecureEnrollmentRecord) {
        records[record.userId] = record
        defaultId = record.userId
        publish()
    }

    override suspend fun get(userId: String): SecureEnrollmentRecord? = records[userId]

    override suspend fun getDefault(): SecureEnrollmentRecord? =
        defaultId?.let { records[it] } ?: records.values.lastOrNull()

    override suspend fun list(): List<SecureEnrollmentRecord> = state.value

    override suspend fun remove(userId: String) {
        records.remove(userId)
        if (defaultId == userId) {
            defaultId = records.keys.lastOrNull()
        }
        publish()
    }

    override suspend fun clear() {
        records.clear()
        defaultId = null
        publish()
    }

    override fun updates(): Flow<List<SecureEnrollmentRecord>> = state.asStateFlow()

    private fun publish() {
        state.value = records.values.sortedBy { it.updatedAt }
    }
}
