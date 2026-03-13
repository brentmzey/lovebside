# Database Collections Refactoring Example

This file shows how to refactor existing code to use the new `DatabaseCollections` constants.

## Before (Hardcoded Strings)

```kotlin
// ❌ Error-prone: Typos won't be caught at compile time
class PocketBaseMessagingRepository {
    suspend fun getMessages(conversationId: String) {
        val records = pocketBase.collection("m_messages")  // Typo: "m_mesages" would cause runtime error
            .getList(QueryOptions(filter = "conversationId = '${conversationId}'"))
    }
    
    suspend fun getConversations(userId: String) {
        val records = pocketBase.collection("m_conversations")
            .getList(QueryOptions(filter = "participant1Id = '${userId}'"))
    }
    
    suspend fun getProfile(userId: String) {
        val profile = pocketBase.collection("s_profiles")
            .getOne(userId)
    }
}
```

## After (Using DatabaseCollections)

```kotlin
import love.bside.app.data.DatabaseCollections

// ✅ Type-safe: Autocomplete and compile-time checks
class PocketBaseMessagingRepository {
    suspend fun getMessages(conversationId: String) {
        val records = pocketBase.collection(DatabaseCollections.M_MESSAGES)
            .getList(QueryOptions(filter = "conversationId = '${conversationId}'"))
    }
    
    suspend fun getConversations(userId: String) {
        val records = pocketBase.collection(DatabaseCollections.M_CONVERSATIONS)
            .getList(QueryOptions(filter = "participant1Id = '${userId}'"))
    }
    
    suspend fun getProfile(userId: String) {
        val profile = pocketBase.collection(DatabaseCollections.S_PROFILES)
            .getOne(userId)
    }
}
```

## Migration Patterns

### Pattern 1: Simple Find & Replace

```bash
# Find all instances of hardcoded collection names
grep -r 'collection("m_messages")' shared/src/

# Replace with constant (using sed or your IDE)
# "m_messages" → DatabaseCollections.M_MESSAGES
```

### Pattern 2: Programmatic Access

```kotlin
// Before
fun getCollectionByDomain(domain: String, entity: String): String {
    return when(domain) {
        "messaging" -> when(entity) {
            "messages" -> "m_messages"
            "conversations" -> "m_conversations"
            else -> throw IllegalArgumentException()
        }
        else -> throw IllegalArgumentException()
    }
}

// After
fun getCollectionByDomain(domain: DatabaseCollections.Domain, entity: DatabaseCollections.Entity): String {
    return DatabaseCollections.get(domain, entity)
}
```

### Pattern 3: Testing

```kotlin
// Before
@Test
fun testMessagingRepository() {
    val mockPb = mockk<PocketbaseClient>()
    every { mockPb.collection("m_messages") } returns mockk()
    // Test code...
}

// After  
@Test
fun testMessagingRepository() {
    val mockPb = mockk<PocketbaseClient>()
    every { mockPb.collection(DatabaseCollections.M_MESSAGES) } returns mockk()
    // Test code... now refactor-safe!
}
```

## Benefits

1. **IDE Autocomplete**: Type `DatabaseCollections.` and see all available collections
2. **Compile-Time Safety**: Typos in collection names caught immediately
3. **Refactoring**: Change collection name in one place, affects entire codebase
4. **Documentation**: Self-documenting code with clear constants
5. **Migration Ready**: Switch backends by updating constants, not hunting through code

## Real-World Example: Database Migration

### Scenario: Migrating from PocketBase to Supabase (Postgres)

```kotlin
// DatabaseCollections.kt - Before (PocketBase)
object DatabaseCollections {
    const val M_MESSAGES = "m_messages"
    const val M_CONVERSATIONS = "m_conversations"
}

// DatabaseCollections.kt - After (Supabase/Postgres)  
object DatabaseCollections {
    // Option 1: Use Postgres naming convention
    const val M_MESSAGES = "messaging.messages"
    const val M_CONVERSATIONS = "messaging.conversations"
    
    // Option 2: Use environment variable
    private val usePostgres = System.getenv("USE_POSTGRES") == "true"
    val M_MESSAGES = if (usePostgres) "messaging.messages" else "m_messages"
}
```

**Result**: Zero changes needed in repository code! 🎉

## Validation Example

```kotlin
// Validate collection access at runtime
fun safeCollectionAccess(collectionName: String) {
    require(DatabaseCollections.isValid(collectionName)) {
        "Attempted to access unknown collection: $collectionName. " +
        "Available collections: ${DatabaseCollections.all().joinToString()}"
    }
    
    return pocketBase.collection(collectionName)
}
```

## Testing All Collections

```kotlin
@Test
fun `all collection constants are valid`() {
    val allCollections = DatabaseCollections.all()
    
    // Verify no duplicates
    assertEquals(allCollections.size, allCollections.toSet().size)
    
    // Verify all are non-empty
    allCollections.forEach { collection ->
        assertTrue(collection.isNotBlank(), "Collection name should not be blank")
    }
    
    // Verify naming conventions
    allCollections.forEach { collection ->
        assertTrue(
            collection.matches(Regex("[a-z_]+")),
            "Collection $collection should use snake_case"
        )
    }
}

@Test  
fun `domain helper functions return correct collections`() {
    val messagingCollections = DatabaseCollections.messagingCollections()
    assertTrue(DatabaseCollections.M_MESSAGES in messagingCollections)
    assertTrue(DatabaseCollections.M_CONVERSATIONS in messagingCollections)
    
    val socialCollections = DatabaseCollections.socialCollections()
    assertTrue(DatabaseCollections.S_PROFILES in socialCollections)
}
```

## Advanced: Multi-Tenant with Collection Prefixes

```kotlin
object DatabaseCollections {
    private val tenantId = System.getenv("TENANT_ID") ?: "default"
    
    // Add tenant prefix to collection names
    fun withTenant(collectionName: String): String {
        return "${tenantId}_${collectionName}"
    }
    
    val M_MESSAGES get() = withTenant("m_messages")
    val M_CONVERSATIONS get() = withTenant("m_conversations")
}

// Usage: Tenant "acme" would use "acme_m_messages"
```

## Redis Caching Pattern

```kotlin
object CacheKeys {
    // Build cache keys using collection constants
    fun messageKey(messageId: String) = 
        "cache:${DatabaseCollections.M_MESSAGES}:$messageId"
    
    fun conversationKey(convId: String) = 
        "cache:${DatabaseCollections.M_CONVERSATIONS}:$convId"
    
    fun profileKey(userId: String) = 
        "cache:${DatabaseCollections.S_PROFILES}:$userId"
}

// Usage
val cachedMessage = redis.get(CacheKeys.messageKey(messageId))
```

## Summary

By using `DatabaseCollections`, you get:

- ✅ Type safety
- ✅ IDE support  
- ✅ Easy refactoring
- ✅ Clear documentation
- ✅ Migration flexibility
- ✅ Reduced bugs

**Action Items**:

1. Import `DatabaseCollections` in all repository files
2. Replace hardcoded strings with constants
3. Add validation where appropriate
4. Update tests to use constants
5. Document any new collections
