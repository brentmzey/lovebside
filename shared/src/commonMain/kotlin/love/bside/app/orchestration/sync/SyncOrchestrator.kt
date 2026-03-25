package love.bside.app.orchestration.sync

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import love.bside.app.core.Result
import love.bside.app.orchestration.events.EventBus
import love.bside.app.orchestration.events.SyncCompleted
import love.bside.app.orchestration.events.SyncFailed
import love.bside.app.orchestration.events.SyncStarted

/**
 * Orchestrates synchronization between local and remote data.
 * Handles conflict resolution, offline queue, and sync strategies.
 */
class SyncOrchestrator(
    private val eventBus: EventBus,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val syncStrategies = mutableMapOf<String, SyncStrategy<*>>()
    private val pendingOperations = mutableListOf<SyncOperation>()

    fun registerStrategy(entityType: String, strategy: SyncStrategy<*>) {
        syncStrategies[entityType] = strategy
    }

    suspend fun sync(entityType: String, force: Boolean = false): Result<SyncResult> {
        val strategy = syncStrategies[entityType] 
            ?: return Result.Error(love.bside.app.core.AppException.Unknown("No sync strategy for $entityType"))

        _syncState.value = SyncState.Syncing(entityType)
        
        eventBus.publish(SyncStarted(
            aggregateId = entityType,
            syncType = entityType
        ))

        return try {
            val result = strategy.sync(force)
            
            when (result) {
                is Result.Success -> {
                    eventBus.publish(SyncCompleted(
                        aggregateId = entityType,
                        syncType = entityType,
                        itemsSynced = result.data.itemsSynced
                    ))
                    _syncState.value = SyncState.Idle
                }
                is Result.Error -> {
                    eventBus.publish(SyncFailed(
                        aggregateId = entityType,
                        syncType = entityType,
                        error = result.exception.message
                    ))
                    _syncState.value = SyncState.Failed(result.exception.message)
                }
                else -> {}
            }
            
            result
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Sync failed"
            _syncState.value = SyncState.Failed(errorMsg)
            eventBus.publish(SyncFailed(
                aggregateId = entityType,
                syncType = entityType,
                error = errorMsg
            ))
            Result.Error(love.bside.app.core.AppException.Unknown(errorMsg, e))
        }
    }

    suspend fun syncAll(): Result<List<SyncResult>> {
        val results = mutableListOf<SyncResult>()
        
        for ((entityType, _) in syncStrategies) {
            when (val result = sync(entityType)) {
                is Result.Success -> results.add(result.data)
                else -> {} // Continue with other syncs
            }
        }
        
        return Result.Success(results)
    }

    fun queueOperation(operation: SyncOperation) {
        pendingOperations.add(operation)
    }

    suspend fun processPendingOperations() {
        val operations = pendingOperations.toList()
        pendingOperations.clear()

        operations.forEach { operation ->
            // Process each operation
            val strategy = syncStrategies[operation.entityType]
            strategy?.applyOperation(operation)
        }
    }
}

sealed class SyncState {
    data object Idle : SyncState()
    data class Syncing(val entityType: String) : SyncState()
    data class Failed(val error: String) : SyncState()
}

data class SyncResult(
    val entityType: String,
    val itemsSynced: Int,
    val conflicts: Int = 0,
    val errors: Int = 0,
    val timestamp: Instant = Clock.System.now()
)

data class SyncOperation(
    val id: String,
    val entityType: String,
    val operation: OperationType,
    val entityId: String,
    val payload: SyncPayload,
    val timestamp: Instant = Clock.System.now()
)

enum class OperationType {
    CREATE,
    UPDATE,
    DELETE
}

interface SyncStrategy<T : Any> {
    suspend fun sync(force: Boolean = false): Result<SyncResult>
    suspend fun applyOperation(operation: SyncOperation): Result<Unit>
    suspend fun resolveConflict(local: T, remote: T): T
}

/**
 * Strongly-typed sync payload hierarchy.
 * Add a new subclass for each entity type that supports offline sync.
 */
sealed class SyncPayload {
    data class Profile(
        val firstName: String? = null,
        val lastName: String? = null,
        val bio: String? = null,
        val seeking: String? = null,
        val latitude: Double? = null,
        val longitude: Double? = null
    ) : SyncPayload()

    data class Message(
        val conversationId: String,
        val content: String,
        val type: String = "text",
        val clientMessageId: String
    ) : SyncPayload()

    data class SwipeAction(
        val targetUserId: String,
        val direction: String // "like" | "pass" | "superlike"
    ) : SyncPayload()

    data class KeyValues(
        val keyValueId: String,
        val importance: Int
    ) : SyncPayload()

    /** Escape hatch for future entity types during migration */
    data class Raw(val fields: Map<String, String>) : SyncPayload()
}
