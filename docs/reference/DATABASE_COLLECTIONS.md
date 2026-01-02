# Database Collections Reference

## Overview

The `DatabaseCollections` object provides a centralized, static enumeration of all database tables/collections used throughout the BSide application. This design makes it easier to maintain consistency and facilitates potential migration to different database backends.

## Current Backend: PocketBase

The app currently uses **PocketBase** as its backend, but the abstraction layer makes it database-agnostic for future migrations.

## Collection Naming Convention

Collections follow a prefix-based naming convention:

| Prefix | Domain | Description |
|--------|--------|-------------|
| `m_` | Messaging | Real-time messaging and conversations |
| `s_` | Social | User profiles and social features |
| `p_` | Proust | Questionnaire and personality assessment |
| `t_` | Technical | System-level and infrastructure tables |

## Collections by Domain

### 🔐 System Collections

| Collection | Constant | Purpose |
|-----------|----------|---------|
| `users` | `USERS` | PocketBase built-in auth/user management |
| `t_user` | `T_USER` | Alternative user table reference |
| `t_user_property` | `T_USER_PROPERTY` | User-specific key-value cache (distributed caching support) |
| `t_tenant_property` | `T_TENANT_PROPERTY` | Tenant-specific properties (multi-tenant support) |
| `t_typing_status` | `T_TYPING_STATUS` | Real-time typing indicators |

### 💬 Messaging Collections

| Collection | Constant | Purpose |
|-----------|----------|---------|
| `m_conversations` | `M_CONVERSATIONS` | Conversation metadata and participants |
| `m_messages` | `M_MESSAGES` | Individual messages with threading support |
| `m_conversation_participants` | `M_CONVERSATION_PARTICIPANTS` | Many-to-many: users ↔ conversations |

### 👤 Social Collections

| Collection | Constant | Purpose |
|-----------|----------|---------|
| `s_profiles` | `S_PROFILES` | User profiles (bio, photos, preferences) |
| `m_matches` | `M_MATCHES` | Match relationships with scores and status |

### 📝 Questionnaire Collections

| Collection | Constant | Purpose |
|-----------|----------|---------|
| `p_questionnaires` | `P_QUESTIONNAIRES` | Questionnaire questions |
| `t_proust_question` | `T_PROUST_QUESTION` | Alternative name for Proust questions |
| `t_user_questionnaire_responses` | `T_USER_QUESTIONNAIRE_RESPONSES` | User answers to questions |

## Usage Examples

### Basic Usage

```kotlin
import love.bside.app.data.DatabaseCollections

// In a repository
val messages = pocketBase.collection(DatabaseCollections.M_MESSAGES)
    .getList(QueryOptions(filter = "conversationId = '${convId}'"))
```

### Using Helper Functions

```kotlin
// Get all collections
val allCollections = DatabaseCollections.all()

// Get domain-specific collections
val messagingCollections = DatabaseCollections.messagingCollections()
val socialCollections = DatabaseCollections.socialCollections()

// Validate collection names
if (DatabaseCollections.isValid("m_messages")) {
    // Safe to use
}
```

### Programmatic Access

```kotlin
// Get collection by domain and entity
val collectionName = DatabaseCollections.get(
    Domain.MESSAGING, 
    Entity.MESSAGES
)
// Returns: "m_messages"
```

### Migration Example

When migrating to PostgreSQL, you only need to update table names in one place:

```kotlin
// PostgreSQL migration
object DatabaseCollections {
    // Change from PocketBase collection names to SQL table names
    const val M_MESSAGES = "public.messaging_messages"
    const val M_CONVERSATIONS = "public.messaging_conversations"
    // ... etc
}
```

Or use environment-based configuration:

```kotlin
object DatabaseCollections {
    private val backend = System.getenv("DB_BACKEND") ?: "pocketbase"
    
    val M_MESSAGES = when(backend) {
        "postgres" -> "public.messaging_messages"
        "mongodb" -> "messages"
        "dynamodb" -> "bside-messages-prod"
        else -> "m_messages"  // PocketBase default
    }
}
```

## Distributed Caching Strategy

### User Properties (`t_user_property`)

Used for user-specific cached data that should persist across sessions and sync across devices:

```kotlin
// Cache user preferences
setUserProperty(userId, "theme_preference", "dark")
setUserProperty(userId, "notification_settings", jsonSettings)

// Offline mode: Read from cache
val cachedTheme = getUserProperty(userId, "theme_preference")
```

### Tenant Properties (`t_tenant_property`)

Used for application-level configuration and feature flags:

```kotlin
// Feature flags
setTenantProperty("feature_messaging_v2", "true")
setTenantProperty("max_conversation_size", "100")
```

### Offline/Airplane Mode Support

The property tables enable offline functionality:

1. **Before going offline**: App caches essential data to `t_user_property`
2. **While offline**: App reads from local cache
3. **When online restored**: Sync queue processes pending operations
4. **Conflict resolution**: Last-write-wins with timestamp-based merging

## Database Migration Roadmap

### Phase 1: Current (PocketBase)

- ✅ Real-time subscriptions
- ✅ Built-in auth
- ✅ File storage
- ✅ Simple deployment

### Phase 2: Enhanced Caching (Redis)

- Add Redis layer for frequently accessed data
- Keep PocketBase for primary storage
- Use `DatabaseCollections` constants with Redis keys: `redis.get("cache:${DatabaseCollections.M_MESSAGES}:$id")`

### Phase 3: PostgreSQL Migration

- Migrate to Postgres for better scalability
- Use Supabase for real-time features
- Table names already abstracted via `DatabaseCollections`

### Phase 4: Multi-Database (Polyglot Persistence)

- Postgres: Relational data (profiles, matches)
- MongoDB: Messages and conversations (document model)
- Redis: Real-time caching
- S3: File storage

## Schema Documentation

Detailed schemas for each collection are available in:

- `/pocketbase/full_schema.json` - Messaging collections
- `/pocketbase/matching_schema.json` - Social and matching collections  
- `/pocketbase/profile_full_schema.json` - Profile collections

## Best Practices

1. **Always use constants**: Never hardcode collection names

   ```kotlin
   ❌ pocketBase.collection("m_messages")
   ✅ pocketBase.collection(DatabaseCollections.M_MESSAGES)
   ```

2. **Use helper functions** for validation:

   ```kotlin
   fun getMessa(collectionName: String) {
       require(DatabaseCollections.isValid(collectionName)) {
           "Invalid collection: $collectionName"
       }
   }
   ```

3. **Document new collections** here and in `DatabaseCollections.kt`

4. **Keep naming consistent** with the established prefix convention

## Future Database Options

The abstraction layer supports migration to:

- **PostgreSQL** - Better for complex queries and analytics
- **MongoDB** - Document model for flexible schemas
- **DynamoDB** - AWS-native, serverless scaling
- **Firebase Firestore** - Real-time sync, mobile-first
- **CockroachDB** - Distributed SQL with global consistency
- **Redis** - High-performance caching layer

## Contributing

When adding new collections:

1. Add constant to `DatabaseCollections.kt`
2. Add to appropriate helper function (`all()`, domain-specific)
3. Document in this README
4. Update schema JSON files in `/pocketbase/`
5. Add migration notes if needed
