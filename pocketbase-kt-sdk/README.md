# PocketBase Kotlin Multiplatform SDK

[![Maven Central](https://img.shields.io/maven-central/v/io.pocketbase/pocketbase-kt-sdk?label=Maven%20Central)](https://search.maven.org/artifact/io.pocketbase/pocketbase-kt-sdk)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![Build](https://github.com/yourusername/pocketbase-kt-sdk/workflows/Build%20and%20Test/badge.svg)](https://github.com/yourusername/pocketbase-kt-sdk/actions)

A pure Kotlin implementation of the PocketBase client that works across **all platforms**: JVM (Android, Desktop), iOS, and JavaScript (Browser, Node.js).

## Features

✅ **100% Kotlin** - No platform-specific code, pure kotlinx libraries  
✅ **Multiplatform** - Works on Android, iOS, JVM, and JS  
✅ **Realtime Support** - Full SSE (Server-Sent Events) implementation  
✅ **Type-Safe** - Leverage Kotlin's type system  
✅ **Coroutines First** - Built with Kotlin Coroutines for async operations  
✅ **Standalone** - Can be extracted as an independent library  

## Installation

### Maven Central (Recommended)

```kotlin
// Gradle Kotlin DSL (build.gradle.kts)
dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0")
}
```

```groovy
// Gradle Groovy DSL (build.gradle)
dependencies {
    implementation 'io.pocketbase:pocketbase-kt-sdk:0.1.0'
}
```

### Using Version Catalog

Add to `gradle/libs.versions.toml`:

```toml
[versions]
pocketbase-sdk = "0.1.0"

[libraries]
pocketbase-sdk = { module = "io.pocketbase:pocketbase-kt-sdk", version.ref = "pocketbase-sdk" }
```

Then use in your module:

```kotlin
dependencies {
    implementation(libs.pocketbase.sdk)
}
```

### As a Project Module

If using as a module in your project:

```kotlin
// settings.gradle.kts
include(":pocketbase-kt-sdk")

// In your module's build.gradle.kts
dependencies {
    implementation(project(":pocketbase-kt-sdk"))
}
```

### Snapshots

To use snapshot versions, add the snapshot repository:

```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    implementation("io.pocketbase:pocketbase-kt-sdk:0.1.0-SNAPSHOT")
}
```

## Quick Start

```kotlin
import io.pocketbase.PocketBase

// Initialize the client
val pb = PocketBase("https://your-instance.pockethost.io")

// Authenticate
val authData = pb.collection("users").authWithPassword(
    usernameOrEmail = "user@example.com",
    password = "password123"
)

// Get records
val messages = pb.collection("t_message").getList()

// Create a record
val newMessage = pb.collection("t_message").create(
    mapOf(
        "from_user" to "user_id_1",
        "to_user" to "user_id_2",
        "message" to "Hello from Kotlin!",
        "conversation_id" to "conv_123"
    )
)

// Update a record
pb.collection("t_message").update(
    id = "record_id",
    body = mapOf("message" to "Updated message")
)

// Delete a record
pb.collection("t_message").delete("record_id")
```

## Realtime / SSE Subscriptions

The SDK includes full support for PocketBase's realtime subscriptions via Server-Sent Events (SSE):

```kotlin
// Subscribe to all changes in a collection
pb.collection("t_message").subscribe("*") { event ->
    when (event.action) {
        RealtimeAction.create -> println("New message created!")
        RealtimeAction.update -> println("Message updated!")
        RealtimeAction.delete -> println("Message deleted!")
    }
    println("Record data: ${event.record}")
}

// Subscribe to a specific record
val unsubscribe = pb.collection("t_message").subscribe("RECORD_ID") { event ->
    println("Record ${event.record} was ${event.action}")
}

// Unsubscribe
unsubscribe()

// Or unsubscribe from all subscriptions in a collection
pb.collection("t_message").unsubscribe()
```

### With Query Options

```kotlin
pb.collection("t_message").subscribe(
    recordId = "*",
    callback = { event ->
        println("Filtered message: ${event.record}")
    },
    options = QueryOptions(
        filter = "conversation_id = 'conv_123'",
        expand = "from_user,to_user"
    )
)
```

## Advanced Usage

### Custom Auth Store

By default, the SDK uses an in-memory auth store. For persistence, implement the `AuthStore` interface:

```kotlin
class PersistentAuthStore : AuthStore {
    override var token: String = loadFromStorage()
    override var model: JsonObject? = loadModelFromStorage()
    
    override fun save(token: String, model: JsonObject?) {
        this.token = token
        this.model = model
        saveToStorage(token, model)
    }
    
    override fun clear() {
        token = ""
        model = null
        clearStorage()
    }
    
    // ... implement other methods
}

val pb = PocketBase(
    baseURL = "https://your-instance.pockethost.io",
    authStore = PersistentAuthStore()
)
```

### Request Hooks

```kotlin
// Modify requests before sending
pb.beforeSend = { url, options ->
    options.headers["X-Custom-Header"] = "value"
    RequestModification(url = url, headers = options.headers)
}

// Process responses
pb.afterSend = { response, data ->
    println("Response received: ${response.status}")
    data // return modified data if needed
}
```

### Query Options

```kotlin
val records = pb.collection("t_message").getList(
    QueryOptions(
        filter = "created >= '2024-01-01'",
        sort = "-created",
        expand = "from_user,to_user",
        fields = "id,message,created",
        page = 1,
        perPage = 50
    )
)
```

### Full List (Auto-pagination)

```kotlin
// Automatically fetches all pages
val allMessages = pb.collection("t_message").getFullList(
    QueryOptions(filter = "conversation_id = 'conv_123'")
)
```

### Get First Match

```kotlin
val message = pb.collection("t_message").getFirstListItem(
    filter = "id = 'specific_id'"
)
```

## Architecture

The SDK is structured to mirror the official JavaScript SDK:

```
io.pocketbase/
├── PocketBase.kt           # Main client class
├── models/                 # Data models
│   ├── RecordModels.kt
│   ├── RealtimeModels.kt
│   └── ClientResponseError.kt
├── services/               # Service classes
│   ├── BaseService.kt
│   ├── RecordService.kt
│   └── RealtimeService.kt
├── stores/                 # Auth storage
│   └── AuthStore.kt
└── tools/                  # Utilities
    └── SSEClient.kt        # SSE implementation
```

## Key Technologies

- **Ktor Client** - For HTTP requests (works on all platforms)
- **Kotlinx Serialization** - For JSON parsing
- **Kotlinx Coroutines** - For async operations
- **Kotlinx Coroutines Flow** - For SSE streaming

## Platform Support

| Platform | Status | HTTP Client |
|----------|--------|-------------|
| Android  | ✅ Supported | OkHttp |
| JVM      | ✅ Supported | OkHttp |
| iOS      | ✅ Supported | Darwin (NSURLSession) |
| JS (Browser) | ✅ Supported | JS Fetch |
| JS (Node.js) | ✅ Supported | JS |

## Extracting as Standalone Library

This SDK is designed to be extracted into its own repository. To do so:

1. Copy the `pocketbase-kt-sdk` directory
2. Create a new Git repository
3. Add publishing configuration to `build.gradle.kts`:

```kotlin
plugins {
    `maven-publish`
    signing
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "io.pocketbase"
            artifactId = "pocketbase-kt-sdk"
            version = "0.1.0"
            
            from(components["kotlin"])
        }
    }
}
```

## Testing

```kotlin
// In commonTest
class PocketBaseTest {
    @Test
    fun testConnection() = runTest {
        val pb = PocketBase("https://example.com")
        // Your tests here
    }
}
```

## Comparison with JavaScript SDK

This SDK aims for 100% API compatibility with the official JavaScript SDK:

| Feature | JS SDK | Kotlin SDK |
|---------|--------|------------|
| CRUD Operations | ✅ | ✅ |
| Authentication | ✅ | ✅ |
| Realtime (SSE) | ✅ | ✅ |
| File Upload | ✅ | 🚧 Coming soon |
| OAuth2 | ✅ | 🚧 Coming soon |
| Admin API | ✅ | 🚧 Coming soon |

## Contributing

Contributions are welcome! This SDK is designed to be:
- Pure Kotlin (no platform-specific code)
- Well-documented
- Tested across all platforms

## License

Same as your parent project license.

## Roadmap

- [ ] File upload support
- [ ] OAuth2 authentication
- [ ] Admin API support
- [ ] Collections management
- [ ] Logs API
- [ ] Settings API
- [ ] Health check
- [ ] Backups API
- [ ] Publish to Maven Central

## Example Projects

See the parent `bside` app for real-world usage examples.
