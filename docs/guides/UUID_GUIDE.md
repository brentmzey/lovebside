# UUID Usage Guide - Kotlin Multiplatform

## Overview

BSide uses **[benasher44/uuid](https://github.com/benasher44/uuid)** for cross-platform UUID generation and validation. This library works on all KMP targets: Android, iOS, Desktop (JVM), and Web (JS/Wasm).

## Why UUIDs?

✅ **Cross-platform** - Same UUID format everywhere  
✅ **Distributed** - No central ID server needed  
✅ **Collision-resistant** - Virtually impossible duplicates  
✅ **Type-safe** - Compile-time validation via `EntityId`  
✅ **Serializable** - Automatic JSON conversion  

## Basic Usage

### Generating IDs

```kotlin
import love.bside.app.core.UuidUtils
import love.bside.app.core.EntityId

// Generate new UUID v4  
val messageId: EntityId = UuidUtils.random()
val conversationId = UuidUtils.random()

// Generate as string directly
val userId = UuidUtils.randomString()
```

### Type-Safe IDs

Use the `EntityId` typealias for type-safe, serializable UUIDs:

```kotlin
@Serializable
data class Message(
    val id: EntityId,
    val conversationId: EntityId,
    val senderId: EntityId,
    val content: String,
    val sentAt: Instant
)

// Automatic serialization to JSON
val message = Message(
    id = UuidUtils.random(),
    conversationId = UuidUtils.random(),
    senderId = UuidUtils.random(),
    content = "Hello!",
    sentAt = Clock.System.now()
)

// Serializes to:
// {
//   "id": "550e8400-e29b-41d4-a716-446655440000",
//   "conversationId": "..." 
// }
```

### Validation

```kotlin
// Validate UUID string
val userInput = "550e8400-e29b-41d4-a716-446655440000"

if (UuidUtils.isValid(userInput)) {
    val uuid = UuidUtils.parse(userInput).getOrThrow()
    // Use valid UUID
}

// Extension function
if (userInput.isValidUuid()) {
    val uuid = userInput.toUuid()
}

// Null-safe parsing
val uuid: Uuid? = userInput.toUuidOrNull()
```

### Parsing from Database

```kotlin
// From PocketBase/backend
val record = collection.getOne(id)
val messageId = record["id"]?.toString()?.toUuidOrNull()
    ?: throw IllegalStateException("Invalid message ID")
```

## Integration Examples

### Repository Pattern

```kotlin
class MessageRepository(private val pb: PocketBase) {
    
    suspend fun getMessage(id: EntityId): Result<Message> = runCatching {
        // Convert UUID to string for database query
        val record = pb.collection(DatabaseCollections.M_MESSAGES)
            .getOne(id.toString())
        
        // Parse response with automatic UUID deserialization
        mapRecordToMessage(record)
    }
    
    suspend fun createMessage(
        conversationId: EntityId,
        senderId: EntityId,
        content: String
    ): Result<Message> = runCatching {
        val messageId = UuidUtils.random()
        
        pb.collection(DatabaseCollections.M_MESSAGES).create(mapOf(
            "id" to messageId.toString(),
            "conversationId" to conversationId.toString(),
            "senderId" to senderId.toString(),
            "content" to content
        ))
        
        // Return message with typed IDs
        Message(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            content = content,
            sentAt = Clock.System.now()
        )
    }
}
```

### Database Collections with UUIDs

Update `DatabaseCollections.kt` to support UUID validation:

```kotlin
object DatabaseCollections {
    // ... existing code
    
    /**
     * Validate that a collection record ID is a valid UUID.
     */
    fun validateRecordId(id: String, collection: String): Result<EntityId> =
        UuidUtils.parse(id).onFailure {
            throw IllegalArgumentException(
                "Invalid UUID for collection $collection: $id"
            )
        }
}
```

### ViewModel/UseCase

```kotlin
class SendMessageUseCase(
    private val repository: MessageRepository,
    private val authStore: AuthStore
) {
    suspend operator fun invoke(
        conversationId: EntityId,
        content: String
    ): Result<Message> {
        val senderId = authStore.currentUserId 
            ?: return Result.failure(Exception("Not authenticated"))
        
        return repository.createMessage(
            conversationId = conversationId,
            senderId = senderId.toUuid(),
            content = content
        )
    }
}
```

## Migration Strategy

### From String IDs to UUIDs

**Phase 1: Add UUID support (current)**

```kotlin
// Keep string IDs for now
data class Message(
    val id: String,  // Still string
    val content: String
)
```

**Phase 2: Dual support**

```kotlin
// Support both during migration
data class Message(
    val id: String,
    val content: String
) {
    val uuid: EntityId? get() = id.toUuidOrNull()
}
```

**Phase 3: Full UUID**

```kotlin
// Pure UUID
data class Message(
    val id: EntityId,
    val content: String
)
```

## Testing

```kotlin
class MessageRepositoryTest {
    
    @Test
    fun `should generate valid message IDs`() {
        val id = UuidUtils.random()
        
        // Validate format
        assertTrue(id.toString().matches(
            Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
        ))
    }
    
    @Test
    fun `should serialize and deserialize UUIDs`() {
        val original = Message(
            id = UuidUtils.random(),
            conversationId = UuidUtils.random(),
            content = "Test"
        )
        
        // Serialize to JSON
        val json = Json.encodeToString(original)
        
        // Deserialize back
        val deserialized = Json.decodeFromString<Message>(json)
        
        assertEquals(original.id, deserialized.id)
    }
    
    @Test
    fun `should validate UUID strings`() {
        assertTrue(UuidUtils.isValid("550e8400-e29b-41d4-a716-446655440000"))
        assertFalse(UuidUtils.isValid("not-a-uuid"))
        assertFalse(UuidUtils.isValid(""))
    }
}
```

## Performance Considerations

### UUID Generation

- **UUID v4**: Fast, cryptographically random
- **No network calls**: Generated locally
- **Cross-platform consistent**: Same algorithm everywhere

### Storage

UUIDs are stored as strings (36 characters):

```
550e8400-e29b-41d4-a716-446655440000
```

For databases supporting native UUID types (Postgres):

```kotlin
// Future optimization
fun Uuid.toBinary(): ByteArray = /* 16 bytes */
```

## Future: UUID v1 Support

The library currently supports UUID v4. For v1 (timestamp-based):

```kotlin
// TODO: Contribute to benasher44/uuid
// See: https://github.com/benasher44/uuid/issues/XX
fun uuid1(): Uuid {
    // Implementation with timestamp + MAC address
}
```

Benefits of v1:

- Chronologically sortable
- Contains timestamp information
- Useful for time-series data

## Best Practices

### DO ✅

```kotlin
// Use EntityId for type safety
fun sendMessage(conversationId: EntityId)

// Validate external input
val userId = userInput.toUuidOrNull() 
    ?: return Result.failure(Exception("Invalid ID"))

// Generate UUIDs for new entities
val message = Message(id = UuidUtils.random(), ...)
```

### DON'T ❌

```kotlin
// Don't use plain strings for IDs
fun sendMessage(conversationId: String)  // Risky!

// Don't skip validation
val uuid = Uuid.fromString(input)  // Can crash!

// Don't hardcode UUIDs
val testId = "550e8400..."  // Use UuidUtils.nil() for tests
```

## Cross-Platform Notes

### Android

```kotlin
// Works seamlessly
val uuid: EntityId = UuidUtils.random()
```

### iOS

```kotlin
// Same API
val uuid: EntityId = UuidUtils.random()
// Can interop with NSUUID if needed
```

### Desktop (JVM)

```kotlin
// Can convert to/from java.util.UUID
val javaUuid = java.util.UUID.fromString(uuid.toString())
```

### Web (JS/Wasm)

```kotlin
// Works in browser
val uuid: EntityId = UuidUtils.random()
// Uses crypto.getRandomValues() under the hood
```

## Resources

- [benasher44/uuid GitHub](https://github.com/benasher44/uuid)
- [UUID RFC 4122](https://www.rfc-editor.org/rfc/rfc4122)
- [KMP UUID Serialization](https://github.com/benasher44/uuid#serialization)

## Summary

✅ Added `com.benasher44:uuid:0.8.4` to project  
✅ Created `UuidUtils` for generation and validation  
✅ Type-safe `EntityId` typealias with auto-serialization  
✅ Extension functions for convenient usage  
✅ Works across all KMP platforms  

Ready to use UUIDs everywhere in BSide! 🎯
