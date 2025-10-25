# Migration Guide: PocketBase Kotlin SDK

## Overview

This SDK is a pure Kotlin Multiplatform implementation of the PocketBase JavaScript SDK. It provides a complete replacement for your current server-side PocketBase API calls with a type-safe, coroutine-based API that works across all Kotlin platforms.

## Status

**WORK IN PROGRESS** - The SDK structure is complete with the following components:

### ✅ Completed Components

1. **Core Client** (`PocketBase.kt`)
   - HTTP client with Ktor
   - Request/response handling
   - Auth token management
   - Hooks (beforeSend, afterSend)

2. **Services**
   - `RecordService` - Full CRUD operations
   - `RealtimeService` - SSE subscriptions
   - `BaseService` - Service foundation

3. **Models**
   - `RecordModel` - Base record type
   - `RealtimeEvent` - SSE event types
   - `AuthResponse` - Authentication data
   - `QueryOptions` - Query parameters
   - `ClientResponseException` - Error handling

4. **Stores**
   - `AuthStore` - Authentication storage interface
   - `MemoryAuthStore` - In-memory implementation

5. **Tools**
   - `SSEClient` - Cross-platform SSE implementation

### 🚧 Known Issues Being Fixed

- iOS target compilation (ktor-client-darwin dependency resolution)
- Some edge cases in type handling

### 🎯 Migration Path

#### Before (Current Code)
```kotlin
// Your current PocketBaseClient.kt
val response = httpClient.get("$baseUrl/api/collections/$collection/records") {
    header("Authorization", token)
}
```

#### After (New SDK)
```kotlin
val pb = PocketBase("https://your-instance.pockethost.io")
val records = pb.collection("t_message").getList()
```

## Key Features

### 1. Simplified API

**Old Way:**
```kotlin
class PocketBaseClient {
    fun getMessages(): List<Message> {
        // Manual HTTP request
        // Manual JSON parsing
        // Manual error handling
    }
}
```

**New Way:**
```kotlin
val messages = pb.collection("t_message").getFullList()
```

### 2. Realtime Support

**JavaScript SDK Style:**
```javascript
pb.collection('t_message').subscribe('*', function (e) {
    console.log(e.action);
    console.log(e.record);
});
```

**Kotlin SDK:**
```kotlin
pb.collection("t_message").subscribe("*") { event ->
    println("Action: ${event.action}")
    println("Record: ${event.record}")
}
```

### 3. Type Safety with Generics

```kotlin
@Serializable
data class Message(
    val id: String,
    val message: String,
    val from_user: String,
    val conversation_id: String
)

// Create type-safe extension
suspend fun RecordService.getMessages(): ListResult<Message> {
    val result = this.getList()
    return result.copy(
        items = result.items.map { Json.decodeFromJsonElement<Message>(it) }
    )
}
```

## Next Steps

1. **Fix Remaining Compilation Issues** - Primarily iOS target
2. **Add Tests** - Unit and integration tests
3. **Add File Upload Support** - For media handling
4. **Integrate into Your App** - Replace current PocketBaseClient
5. **Extract as Standalone Library** - Publish to Maven Central

## Integration Plan

Once the SDK is stable:

1. Add dependency in `shared/build.gradle.kts`:
```kotlin
commonMain.dependencies {
    implementation(project(":pocketbase-kt-sdk"))
}
```

2. Replace `PocketBaseClient.kt` with:
```kotlin
val pb = PocketBase(BuildConfig.POCKETBASE_URL)
```

3. Update all API calls to use new SDK methods

4. Migrate realtime subscriptions to use built-in SSE

## Benefits

-  **100% Kotlin** - No JavaScript interop or platform-specific code
- ⚡ **Coroutines** - Native async/await support
- 🔄 **Realtime** - Built-in SSE for live updates  
- 🎯 **Type-Safe** - Leverage Kotlin's type system
- 🧪 **Testable** - Easy to mock and test
- 📦 **Standalone** - Can be extracted and reused

## Questions?

See `README.md` for full API documentation and `EXAMPLES.md` for comprehensive usage examples.
