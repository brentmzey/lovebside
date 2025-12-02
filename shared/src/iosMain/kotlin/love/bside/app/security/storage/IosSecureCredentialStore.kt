package love.bside.app.security.storage

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import love.bside.app.security.SecureCredentialStore
import love.bside.app.security.SecureEnrollmentRecord
import love.bside.app.security.secureRecordJson
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.get
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlock
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
internal class IosSecureCredentialStore : SecureCredentialStore {
    private val state = MutableStateFlow<List<SecureEnrollmentRecord>>(emptyList())
    private val indexSerializer = ListSerializer(String.serializer())

    init {
        state.value = restoreRecords()
    }

    override suspend fun upsert(record: SecureEnrollmentRecord) {
        writeString(recordKey(record.userId), secureRecordJson.encodeToString(record))
        updateIndex { it.apply { add(record.userId) } }
        writeString(DEFAULT_KEY, record.userId)
        state.value = restoreRecords()
    }

    override suspend fun get(userId: String): SecureEnrollmentRecord? =
        readString(recordKey(userId))?.let { secureRecordJson.decodeFromString<SecureEnrollmentRecord>(it) }

    override suspend fun getDefault(): SecureEnrollmentRecord? {
        val defaultId = readString(DEFAULT_KEY)
        return defaultId?.let { get(it) } ?: state.value.lastOrNull()
    }

    override suspend fun list(): List<SecureEnrollmentRecord> = state.value

    override suspend fun remove(userId: String) {
        deleteKey(recordKey(userId))
        updateIndex { it.apply { remove(userId) } }
        if (readString(DEFAULT_KEY) == userId) {
            deleteKey(DEFAULT_KEY)
        }
        state.value = restoreRecords()
    }

    override suspend fun clear() {
        restoreIndex().forEach { deleteKey(recordKey(it)) }
        deleteKey(DEFAULT_KEY)
        deleteKey(INDEX_KEY)
        state.value = emptyList()
    }

    override fun updates(): Flow<List<SecureEnrollmentRecord>> = state.asStateFlow()

    private fun restoreRecords(): List<SecureEnrollmentRecord> = restoreIndex()
        .mapNotNull { id -> readString(recordKey(id)) }
        .mapNotNull { runCatching { secureRecordJson.decodeFromString<SecureEnrollmentRecord>(it) }.getOrNull() }
        .sortedBy { it.updatedAt }

    private fun updateIndex(transform: (MutableSet<String>) -> Unit) {
        val current = restoreIndex().toMutableSet()
        transform(current)
        val payload = secureRecordJson.encodeToString(indexSerializer, current.toList())
        writeString(INDEX_KEY, payload)
    }

    private fun restoreIndex(): List<String> {
        val raw = readString(INDEX_KEY) ?: return emptyList()
        return runCatching { secureRecordJson.decodeFromString(indexSerializer, raw) }.getOrDefault(emptyList())
    }

    private fun writeString(key: String, value: String) {
        val data = value.encodeToByteArray().toCFData() ?: return
        try {
            deleteKey(key)
            withBaseQuery(key) { query ->
                CFDictionarySetValue(query, kSecValueData, data)
                SecItemAdd(query, null)
            }
        } finally {
            data.safeRelease()
        }
    }

    private fun readString(key: String): String? = memScoped {
        withBaseQuery(key) { query ->
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            val result = alloc<CFTypeRefVar>()
            val status = SecItemCopyMatching(query, result.ptr)
            if (status != errSecSuccess) return@withBaseQuery null
            val valueRef = result.value
            try {
                valueRef.toUtf8String()
            } finally {
                valueRef.safeRelease()
            }
        }
    }

    private fun deleteKey(key: String) {
        withBaseQuery(key) { query ->
            SecItemDelete(query)
        }
    }

    private inline fun <T> withBaseQuery(key: String, block: (CFMutableDictionaryRef) -> T): T {
        val query = createBaseQuery(key)
        return try {
            block(query)
        } finally {
            query.safeRelease()
        }
    }

    private fun createBaseQuery(key: String): CFMutableDictionaryRef {
        val dict = CFDictionaryCreateMutable(
            kCFAllocatorDefault,
            0,
            kCFTypeDictionaryKeyCallBacks.ptr,
            kCFTypeDictionaryValueCallBacks.ptr
        ) ?: error("Unable to create keychain query")
        CFDictionarySetValue(dict, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(dict, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlock)
        dict.setString(kSecAttrService, SERVICE)
        dict.setString(kSecAttrAccount, key)
        return dict
    }

    private fun CFMutableDictionaryRef.setString(key: CFStringRef?, value: String) {
        val cfValue = value.toCFString() ?: return
        CFDictionarySetValue(this, key, cfValue)
        cfValue.safeRelease()
    }

    private fun String.toCFString(): CFStringRef? =
        CFStringCreateWithCString(kCFAllocatorDefault, this, kCFStringEncodingUTF8)

    private fun ByteArray.toCFData(): CFDataRef? =
        if (isEmpty()) {
            CFDataCreate(kCFAllocatorDefault, null, 0)
        } else {
            usePinned {
                CFDataCreate(kCFAllocatorDefault, it.addressOf(0).reinterpret(), size.convert())
            }
        }

    @Suppress("UNCHECKED_CAST")
    private fun CFTypeRef?.toUtf8String(): String? = (this as? CFDataRef)?.toUtf8String()

    private fun CFDataRef?.toUtf8String(): String? {
        val dataRef = this ?: return null
        val length = CFDataGetLength(dataRef).toInt()
        if (length == 0) return ""
        val source = CFDataGetBytePtr(dataRef) ?: return null
        val bytes = ByteArray(length)
        for (index in 0 until length) {
            bytes[index] = source[index].toByte()
        }
        return bytes.decodeToString()
    }

    private fun CFTypeRef?.safeRelease() {
        if (this != null) {
            CFRelease(this)
        }
    }

    private fun recordKey(userId: String) = "$RECORD_PREFIX$userId"

    companion object {
        private const val SERVICE = "love.bside.secure-auth"
        private const val RECORD_PREFIX = "secure_record_"
        private const val DEFAULT_KEY = "secure_record_default"
        private const val INDEX_KEY = "secure_record_index"
    }
}
