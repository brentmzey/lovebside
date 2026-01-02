# 📦 Shared Types Architecture Guide

Complete guide to strongly-typed data models shared across all platforms.

---

## 🎯 Overview

The B-Side app uses a **shared module** to ensure type safety across all platforms:
- ✅ Android, iOS, Desktop, Web clients
- ✅ Ktor backend server
- ✅ PocketBase database

All platforms share the same data models, ensuring consistency and reducing bugs.

---

## 📁 Directory Structure

```
shared/src/commonMain/kotlin/love/bside/app/
├── data/
│   ├── models/              # Data transfer objects (DTOs)
│   │   ├── Profile.kt       # User profile
│   │   ├── Match.kt         # Match between users
│   │   ├── Message.kt       # Chat message
│   │   ├── UserAnswer.kt    # Questionnaire answer
│   │   ├── Prompt.kt        # Question prompt
│   │   └── ...
│   ├── mappers/             # Convert between DTOs and domain models
│   └── repository/          # Repository interfaces
├── domain/
│   ├── models/              # Domain/business models
│   │   ├── Profile.kt       # Business logic profile
│   │   ├── AuthDetails.kt   # Authentication details
│   │   └── ...
│   ├── repository/          # Repository contracts
│   └── usecase/             # Business logic use cases
└── core/                    # Core utilities

server/src/main/kotlin/
└── love/bside/server/
    ├── routes/              # API endpoints (use shared models)
    ├── service/             # Business logic (use shared models)
    └── plugins/             # Server configuration
```

---

## 🔑 Core Shared Models

### Profile
Complete user profile with personal information.

```kotlin
@Serializable
data class Profile(
    val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val bio: String? = null,
    val location: String? = null,
    val seeking: SeekingStatus,
    val profilePicture: String? = null,
    val photos: List<String>? = null,
    val aboutMe: String? = null,
    val height: Int? = null,
    val occupation: String? = null,
    val education: String? = null,
    val interests: List<String>? = null
) {
    fun getDisplayName(): String = "$firstName $lastName"
    fun getProfilePictureUrl(baseUrl: String, size: String = "512x512"): String?
    fun getPhotoUrls(baseUrl: String, size: String = "800x800"): List<String>
}

@Serializable
enum class SeekingStatus {
    @SerialName("Friendship") FRIENDSHIP,
    @SerialName("Relationship") RELATIONSHIP,
    @SerialName("Both") BOTH
}
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/Profile.kt`

### Match
Represents a match between two users.

```kotlin
@Serializable
data class Match(
    val id: String,
    val user1Id: String,
    val user2Id: String,
    val matchScore: Double,
    val status: MatchStatus,
    val created: String,
    val updated: String
)

@Serializable
enum class MatchStatus {
    @SerialName("pending") PENDING,
    @SerialName("accepted") ACCEPTED,
    @SerialName("rejected") REJECTED
}
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/Match.kt`

### Message
Chat message between users.

```kotlin
@Serializable
data class Message(
    val id: String,
    val matchId: String,
    val senderId: String,
    val content: String,
    val created: String,
    val read: Boolean = false
)
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/Message.kt`

### UserAnswer
User's answer to a questionnaire prompt.

```kotlin
@Serializable
data class UserAnswer(
    val id: String,
    val userId: String,
    val promptId: String,
    val answer: String,
    val created: String
)
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/UserAnswer.kt`

### Prompt
Question prompt for users to answer.

```kotlin
@Serializable
data class Prompt(
    val id: String,
    val question: String,
    val category: PromptCategory,
    val order: Int
)

@Serializable
enum class PromptCategory {
    @SerialName("personality") PERSONALITY,
    @SerialName("values") VALUES,
    @SerialName("lifestyle") LIFESTYLE,
    @SerialName("interests") INTERESTS
}
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/Prompt.kt`

### AuthRecord
Authentication response from PocketBase.

```kotlin
@Serializable
data class AuthRecord(
    val token: String,
    val record: UserRecord
)

@Serializable
data class UserRecord(
    val id: String,
    val email: String,
    val username: String,
    val verified: Boolean
)
```

**Location**: `shared/src/commonMain/kotlin/love/bside/app/data/models/AuthRecord.kt`

---

### Emotion Graph Models
Captures the graph-based emotion schema introduced in `pocketbase/migrations/20251130000000_add_emotion_graph.js`.

```
@Serializable
data class EmotionTerm(...)

@Serializable
data class ExpressionModifier(...)

@Serializable
data class GraphItem(...)

@Serializable
data class EmotionEdge(...)

@Serializable
data class EdgeModifier(...)
```

**Data DTOs**: `shared/src/commonMain/kotlin/love/bside/app/data/models/EmotionGraphModels.kt`

**Domain models**: `shared/src/commonMain/kotlin/love/bside/app/domain/models/EmotionGraph.kt`

**Mappers**: `shared/src/commonMain/kotlin/love/bside/app/data/models/EmotionGraphMapper.kt`

These keep Android/iOS/Desktop/JS compiler targets in sync with the PocketBase schema for emotion terms, modifiers, graph items, edges, and edge modifiers.

---

## 🔄 Data Flow

```
┌─────────────────────────────────────────────────────────┐
│                    Client UI Layer                       │
│         (Android/iOS/Desktop/Web Views)                  │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│              ViewModel / Presentation                    │
│           (Uses Domain Models)                           │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                  Use Cases                               │
│           (Business Logic Layer)                         │
└────────────────────┬────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────┐
│                 Repositories                             │
│      (Converts between DTOs and Domain Models)           │
└────────────────────┬────────────────────────────────────┘
                     │
        ┌────────────┴────────────┐
        ▼                         ▼
┌──────────────┐          ┌──────────────┐
│  API Client  │          │ Local Cache  │
│ (Uses DTOs)  │          │ (Uses DTOs)  │
└──────┬───────┘          └──────┬───────┘
       │                         │
       ▼                         ▼
┌─────────────────────────────────────────┐
│       Ktor Server API                   │
│    (Receives/Returns DTOs)              │
└────────────────┬────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────┐
│          PocketBase DB                  │
│      (Stores JSON Data)                 │
└─────────────────────────────────────────┘
```

---

## 💡 How to Use Shared Types

### In Client Code (Compose UI)

```kotlin
// ViewModel using domain models
class ProfileViewModel : ViewModel() {
    private val repository: ProfileRepository by inject()
    
    val profile: StateFlow<Profile?> = repository
        .getProfile(userId)
        .stateIn(viewModelScope, SharingStarted.Lazily, null)
    
    fun updateProfile(profile: Profile) {
        viewModelScope.launch {
            repository.updateProfile(profile)
        }
    }
}

// In Compose UI
@Composable
fun ProfileScreen(viewModel: ProfileViewModel) {
    val profile by viewModel.profile.collectAsState()
    
    profile?.let {
        Text(text = it.getDisplayName())
        Text(text = "Age: ${it.getAge() ?: "Unknown"}")
        Text(text = "Location: ${it.location ?: "Not specified"}")
    }
}
```

### In Server Code (Ktor)

```kotlin
// API endpoint using shared models
fun Route.profileRoutes() {
    route("/api/profile") {
        get("/{userId}") {
            val userId = call.parameters["userId"]!!
            val profile: Profile = profileService.getProfile(userId)
            call.respond(HttpStatusCode.OK, profile)
        }
        
        put("/{userId}") {
            val userId = call.parameters["userId"]!!
            val update = call.receive<ProfileUpdateRequest>()
            val profile = profileService.updateProfile(userId, update)
            call.respond(HttpStatusCode.OK, profile)
        }
    }
}
```

### Repository Implementation

```kotlin
class ProfileRepositoryImpl(
    private val api: ApiClient,
    private val cache: CacheStorage
) : ProfileRepository {
    override suspend fun getProfile(userId: String): Profile {
        // Try cache first
        val cached = cache.get<Profile>("profile_$userId")
        if (cached != null) return cached
        
        // Fetch from API
        val profile = api.get<Profile>("/profile/$userId")
        
        // Cache result
        cache.put("profile_$userId", profile)
        
        return profile
    }
    
    override suspend fun updateProfile(profile: Profile): Profile {
        // Update via API
        val updated = api.put<Profile>("/profile/${profile.userId}", profile)
        
        // Update cache
        cache.put("profile_${profile.userId}", updated)
        
        return updated
    }
}
```

---

## ✨ Benefits of Shared Types

### 1. Type Safety
Compile-time verification that all platforms use the same data structures.

```kotlin
// This will fail to compile if Profile structure changes
val profile = Profile(
    id = "123",
    firstName = "John",
    // Missing required field? Compiler error!
)
```

### 2. Code Reuse
Write validation logic once, use everywhere.

```kotlin
fun Profile.isComplete(): Boolean {
    return firstName.isNotBlank() &&
           lastName.isNotBlank() &&
           birthDate.isNotBlank() &&
           bio?.isNotBlank() == true
}

// Use in Android, iOS, Desktop, Web, Server
```

### 3. Refactoring Safety
Change a model in one place, update everywhere.

```kotlin
// Add new field to Profile
data class Profile(
    // ... existing fields ...
    val verified: Boolean = false  // New field
)

// Compiler will show errors everywhere this needs handling
```

### 4. API Contract
Server and clients automatically stay in sync.

```kotlin
// Server defines response
fun profileRoute() {
    get("/profile/{id}") {
        val profile: Profile = getProfile(id)
        call.respond(profile)  // Serialized to JSON
    }
}

// Client knows exact structure
suspend fun fetchProfile(id: String): Profile {
    return api.get("/profile/$id")  // Deserialized from JSON
}
```

---

## 🔨 Adding New Shared Models

### Step 1: Create Model in Shared Module

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/data/models/Event.kt
package love.bside.app.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val id: String,
    val title: String,
    val description: String,
    val date: String,
    val location: String,
    val attendees: List<String> = emptyList()
)
```

### Step 2: Create Repository Interface

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/domain/repository/EventRepository.kt
package love.bside.app.domain.repository

interface EventRepository {
    suspend fun getEvents(): List<Event>
    suspend fun getEvent(id: String): Event
    suspend fun createEvent(event: Event): Event
    suspend fun updateEvent(event: Event): Event
    suspend fun deleteEvent(id: String)
}
```

### Step 3: Implement in Platform Code

```kotlin
// Client implementation
class EventRepositoryImpl(
    private val api: ApiClient
) : EventRepository {
    override suspend fun getEvents(): List<Event> {
        return api.get("/events")
    }
    // ... other methods
}

// Server route
fun Route.eventRoutes() {
    route("/api/events") {
        get {
            val events: List<Event> = eventService.getAllEvents()
            call.respond(events)
        }
    }
}
```

### Step 4: Use in UI

```kotlin
@Composable
fun EventsScreen(viewModel: EventsViewModel) {
    val events by viewModel.events.collectAsState()
    
    LazyColumn {
        items(events) { event ->
            EventCard(event = event)
        }
    }
}
```

---

## 📝 Serialization

All shared models use `kotlinx.serialization` for JSON serialization.

### Basic Serialization

```kotlin
@Serializable
data class User(
    val id: String,
    val name: String,
    val email: String
)

// Serialize to JSON
val json = Json.encodeToString(user)
// {"id":"123","name":"John","email":"john@example.com"}

// Deserialize from JSON
val user = Json.decodeFromString<User>(json)
```

### Custom Serialization

```kotlin
@Serializable
data class Profile(
    val id: String,
    @SerialName("user_id")  // Map to different JSON field name
    val userId: String,
    @Contextual  // Custom serializer
    val createdAt: Instant
)
```

---

## 🧪 Testing Shared Models

```kotlin
class ProfileTest {
    @Test
    fun testProfileSerialization() {
        val profile = Profile(
            id = "123",
            userId = "user123",
            firstName = "John",
            lastName = "Doe",
            birthDate = "1990-01-01",
            seeking = SeekingStatus.BOTH
        )
        
        val json = Json.encodeToString(profile)
        val decoded = Json.decodeFromString<Profile>(json)
        
        assertEquals(profile, decoded)
    }
    
    @Test
    fun testProfileHelpers() {
        val profile = Profile(/* ... */)
        assertEquals("John Doe", profile.getDisplayName())
        assertNotNull(profile.getProfilePictureUrl("https://api.bside.love"))
    }
}
```

---

## 📚 Best Practices

### 1. Keep Models Simple
Data models should only contain data, not business logic.

✅ Good:
```kotlin
@Serializable
data class Profile(
    val firstName: String,
    val lastName: String
) {
    fun getDisplayName() = "$firstName $lastName"  // Simple helper
}
```

❌ Bad:
```kotlin
@Serializable
data class Profile(/* ... */) {
    suspend fun save() { /* Complex logic */ }  // Business logic
}
```

### 2. Use Default Values
Provide sensible defaults for optional fields.

```kotlin
@Serializable
data class Profile(
    val id: String,
    val bio: String? = null,  // Optional, defaults to null
    val photos: List<String> = emptyList(),  // Defaults to empty list
    val verified: Boolean = false  // Defaults to false
)
```

### 3. Validate at Boundaries
Validate data when crossing boundaries (API, user input).

```kotlin
fun Profile.validate(): Result<Unit> {
    if (firstName.isBlank()) return Result.failure(IllegalArgumentException("First name required"))
    if (lastName.isBlank()) return Result.failure(IllegalArgumentException("Last name required"))
    return Result.success(Unit)
}
```

### 4. Version Your API
Include version info to handle breaking changes.

```kotlin
@Serializable
data class ApiResponse<T>(
    val version: String = "1.0",
    val data: T
)
```

---

## 🔗 Integration Points

### Client → Server
```
Android/iOS/Desktop/Web
    ↓ (Shared Profile model)
HTTP Request (JSON)
    ↓
Ktor Server
    ↓ (Shared Profile model)
PocketBase
```

### Server → Client
```
PocketBase
    ↓ (JSON)
Ktor Server
    ↓ (Shared Profile model)
HTTP Response (JSON)
    ↓
Android/iOS/Desktop/Web
    ↓ (Shared Profile model)
UI Display
```

---

## 📖 Next Steps

1. **Explore Models**: Browse `shared/src/commonMain/kotlin/love/bside/app/data/models/`
2. **Check Repositories**: See `shared/src/commonMain/kotlin/love/bside/app/domain/repository/`
3. **Review Server Routes**: Look at `server/src/main/kotlin/love/bside/server/routes/`
4. **Study Client Usage**: Check `composeApp/src/commonMain/kotlin/`

---

## 🎓 Learning Resources

- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [kotlinx.serialization Guide](https://github.com/Kotlin/kotlinx.serialization)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Ktor Documentation](https://ktor.io/)

---

**Current Shared Models**: 15+ data models covering profiles, matches, messages, questionnaires, and more!
