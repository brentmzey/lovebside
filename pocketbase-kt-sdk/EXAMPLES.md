# PocketBase Kotlin SDK - Usage Examples

## Basic Setup

```kotlin
import io.pocketbase.PocketBase
import io.pocketbase.models.QueryOptions
import kotlinx.coroutines.launch

val pb = PocketBase("https://bside.pockethost.io")
```

## Authentication

### Email/Password Authentication

```kotlin
// Login
val authData = pb.collection("users").authWithPassword(
    usernameOrEmail = "test@example.com",
    password = "123456"
)

println("Token: ${authData.token}")
println("User: ${authData.record}")

// Check if authenticated
if (pb.authStore.isValid) {
    println("User is authenticated!")
}

// Logout
pb.authStore.clear()
```

### Password Reset

```kotlin
// Request password reset
pb.collection("users").requestPasswordReset("user@example.com")

// Confirm password reset
pb.collection("users").confirmPasswordReset(
    token = "reset_token_from_email",
    password = "newPassword123",
    passwordConfirm = "newPassword123"
)
```

### Email Verification

```kotlin
// Request verification
pb.collection("users").requestVerification("user@example.com")

// Confirm verification
pb.collection("users").confirmVerification("verification_token")
```

### Refresh Auth Token

```kotlin
val refreshed = pb.collection("users").authRefresh()
```

## CRUD Operations

### Create

```kotlin
val newMessage = pb.collection("t_message").create(
    body = mapOf(
        "from_user" to "user_id_1",
        "to_user" to "user_id_2",
        "message" to "Hello, World!",
        "conversation_id" to "conv_123"
    )
)

println("Created message: ${newMessage}")
```

### Read

```kotlin
// Get a single record
val message = pb.collection("t_message").getOne("RECORD_ID")

// Get a list with pagination
val result = pb.collection("t_message").getList(
    QueryOptions(
        page = 1,
        perPage = 20
    )
)

println("Total: ${result.totalItems}")
result.items.forEach { message ->
    println("Message: $message")
}

// Get all records (auto-paginate)
val allMessages = pb.collection("t_message").getFullList()

// Get first match
val firstMessage = pb.collection("t_message").getFirstListItem(
    filter = "conversation_id = 'conv_123'"
)
```

### Update

```kotlin
val updated = pb.collection("t_message").update(
    id = "RECORD_ID",
    body = mapOf(
        "message" to "Updated message content"
    )
)
```

### Delete

```kotlin
val success = pb.collection("t_message").delete("RECORD_ID")
if (success) {
    println("Record deleted successfully")
}
```

## Query Options

### Filtering

```kotlin
// Simple filter
val messages = pb.collection("t_message").getList(
    QueryOptions(
        filter = "conversation_id = 'conv_123'"
    )
)

// Multiple conditions
val filtered = pb.collection("t_message").getList(
    QueryOptions(
        filter = "conversation_id = 'conv_123' && created >= '2024-01-01'"
    )
)

// Using operators
val recent = pb.collection("t_message").getList(
    QueryOptions(
        filter = "created >= @now '-7d'"
    )
)
```

### Sorting

```kotlin
// Sort by created date (descending)
val sorted = pb.collection("t_message").getList(
    QueryOptions(
        sort = "-created"
    )
)

// Multiple sort fields
val multiSort = pb.collection("t_message").getList(
    QueryOptions(
        sort = "-created,message"
    )
)
```

### Expanding Relations

```kotlin
// Expand single relation
val withUser = pb.collection("t_message").getList(
    QueryOptions(
        expand = "from_user"
    )
)

// Expand multiple relations
val withUsers = pb.collection("t_message").getList(
    QueryOptions(
        expand = "from_user,to_user"
    )
)

// Access expanded data
withUsers.items.forEach { message ->
    val fromUser = message["expand"]?.jsonObject?.get("from_user")
    println("From: $fromUser")
}
```

### Field Selection

```kotlin
// Only fetch specific fields
val partial = pb.collection("t_message").getList(
    QueryOptions(
        fields = "id,message,created"
    )
)
```

### Combined Options

```kotlin
val result = pb.collection("t_message").getList(
    QueryOptions(
        filter = "conversation_id = 'conv_123'",
        sort = "-created",
        expand = "from_user,to_user",
        fields = "id,message,created,from_user,to_user",
        page = 1,
        perPage = 50,
        skipTotal = true // Skip counting total items for better performance
    )
)
```

## Realtime Subscriptions

### Subscribe to All Records

```kotlin
// Subscribe to all changes in a collection
val unsubscribe = pb.collection("t_message").subscribe("*") { event ->
    println("Action: ${event.action}") // create, update, or delete
    println("Record: ${event.record}")
}

// Later, unsubscribe
unsubscribe()
```

### Subscribe to Specific Record

```kotlin
val unsubscribe = pb.collection("t_message").subscribe("RECORD_ID") { event ->
    when (event.action) {
        RealtimeAction.update -> println("Record updated!")
        RealtimeAction.delete -> println("Record deleted!")
        else -> {}
    }
}
```

### Subscribe with Filters

```kotlin
// Only receive events for specific conversation
pb.collection("t_message").subscribe(
    recordId = "*",
    callback = { event ->
        println("New message in conversation: ${event.record}")
    },
    options = QueryOptions(
        filter = "conversation_id = 'conv_123'"
    )
)
```

### Subscribe with Expand

```kotlin
// Receive expanded relation data in realtime events
pb.collection("t_message").subscribe(
    recordId = "*",
    callback = { event ->
        val record = event.record.jsonObject
        val expandedUser = record["expand"]?.jsonObject?.get("from_user")
        println("Message from: $expandedUser")
    },
    options = QueryOptions(
        expand = "from_user,to_user"
    )
)
```

### Connection Status

```kotlin
// Check if realtime is connected
if (pb.realtime.isConnected.value) {
    println("Realtime connected!")
}

// Observe connection status
scope.launch {
    pb.realtime.isConnected.collect { connected ->
        println("Realtime connection: $connected")
    }
}
```

### Handle Disconnections

```kotlin
pb.realtime.onDisconnect = { activeSubscriptions ->
    if (activeSubscriptions.isNotEmpty()) {
        println("Lost connection with active subscriptions: $activeSubscriptions")
        // Handle reconnection logic
    } else {
        println("Disconnected cleanly")
    }
}
```

### Unsubscribe Patterns

```kotlin
// Unsubscribe from specific record
pb.collection("t_message").unsubscribe("RECORD_ID")

// Unsubscribe from all records in collection
pb.collection("t_message").unsubscribe("*")

// Unsubscribe from entire collection
pb.collection("t_message").unsubscribe()

// Unsubscribe by prefix
pb.realtime.unsubscribeByPrefix("t_message/")
```

## Android Integration Example

```kotlin
class MessageViewModel : ViewModel() {
    private val pb = PocketBase("https://bside.pockethost.io")
    private val _messages = MutableStateFlow<List<JsonObject>>(emptyList())
    val messages: StateFlow<List<JsonObject>> = _messages
    
    init {
        // Load initial messages
        viewModelScope.launch {
            val result = pb.collection("t_message").getList()
            _messages.value = result.items
            
            // Subscribe to realtime updates
            pb.collection("t_message").subscribe("*") { event ->
                when (event.action) {
                    RealtimeAction.create -> {
                        _messages.value = _messages.value + event.record.jsonObject
                    }
                    RealtimeAction.update -> {
                        _messages.value = _messages.value.map {
                            if (it["id"]?.jsonPrimitive?.content == 
                                event.record.jsonObject["id"]?.jsonPrimitive?.content) {
                                event.record.jsonObject
                            } else {
                                it
                            }
                        }
                    }
                    RealtimeAction.delete -> {
                        _messages.value = _messages.value.filter {
                            it["id"]?.jsonPrimitive?.content != 
                                event.record.jsonObject["id"]?.jsonPrimitive?.content
                        }
                    }
                }
            }
        }
    }
    
    fun sendMessage(text: String, conversationId: String) {
        viewModelScope.launch {
            pb.collection("t_message").create(
                mapOf(
                    "message" to text,
                    "conversation_id" to conversationId,
                    "from_user" to pb.authStore.model?.get("id")?.jsonPrimitive?.content
                )
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        pb.close()
    }
}
```

## Compose Multiplatform Example

```kotlin
@Composable
fun MessageList(pb: PocketBase, conversationId: String) {
    val messages = remember { mutableStateListOf<JsonObject>() }
    
    LaunchedEffect(conversationId) {
        // Load messages
        val result = pb.collection("t_message").getList(
            QueryOptions(
                filter = "conversation_id = '$conversationId'",
                sort = "created",
                expand = "from_user"
            )
        )
        messages.addAll(result.items)
        
        // Subscribe to updates
        pb.collection("t_message").subscribe(
            recordId = "*",
            callback = { event ->
                when (event.action) {
                    RealtimeAction.create -> messages.add(event.record.jsonObject)
                    RealtimeAction.update -> {
                        val index = messages.indexOfFirst { 
                            it["id"] == event.record.jsonObject["id"] 
                        }
                        if (index != -1) messages[index] = event.record.jsonObject
                    }
                    RealtimeAction.delete -> {
                        messages.removeAll { 
                            it["id"] == event.record.jsonObject["id"] 
                        }
                    }
                }
            },
            options = QueryOptions(filter = "conversation_id = '$conversationId'")
        )
    }
    
    LazyColumn {
        items(messages) { message ->
            MessageItem(message)
        }
    }
}
```

## Error Handling

```kotlin
try {
    val message = pb.collection("t_message").getOne("invalid_id")
} catch (e: ClientResponseException) {
    when (e.statusCode) {
        404 -> println("Message not found")
        401 -> println("Unauthorized")
        else -> println("Error: ${e.response.message}")
    }
}
```

## Custom Headers

```kotlin
// Global headers via beforeSend hook
pb.beforeSend = { url, options ->
    options.headers["X-Custom-Header"] = "value"
    RequestModification(headers = options.headers)
}

// Per-request headers
val result = pb.collection("t_message").getList(
    QueryOptions(
        headers = mapOf("X-Request-ID" to "12345")
    )
)
```

## Type-Safe Models (Recommended Pattern)

```kotlin
@Serializable
data class Message(
    val id: String,
    val from_user: String,
    val to_user: String,
    val message: String,
    val conversation_id: String,
    val created: String,
    val updated: String
)

// Extension function for type safety
suspend fun RecordService.getTypedList(): ListResult<Message> {
    val result = this.getList()
    val json = Json { ignoreUnknownKeys = true }
    return ListResult(
        page = result.page,
        perPage = result.perPage,
        totalItems = result.totalItems,
        totalPages = result.totalPages,
        items = result.items.map { json.decodeFromJsonElement<Message>(it) }
    )
}

// Usage
val messages = pb.collection("t_message").getTypedList()
messages.items.forEach { message: Message ->
    println(message.message)
}
```
