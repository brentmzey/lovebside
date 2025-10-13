package love.bside.app.domain.repository

import love.bside.app.core.Result
import love.bside.app.domain.models.KeyValue
import love.bside.app.domain.models.UserValue

interface ValuesRepository {
    suspend fun getAllKeyValues(): Result<List<KeyValue>>
    suspend fun getKeyValuesByCategory(category: String): Result<List<KeyValue>>
    suspend fun addUserValue(userValue: UserValue): Result<Unit>
    suspend fun removeUserValue(userValue: UserValue): Result<Unit>
    suspend fun getUserValues(userId: String): Result<List<UserValue>>
}
