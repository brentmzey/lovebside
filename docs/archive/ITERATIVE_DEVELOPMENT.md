# Iterative Development Workflow Guide 🔄

**How to develop, test, and improve the B-Side app iteratively**

---

## Table of Contents
1. [Development Cycle Overview](#development-cycle-overview)
2. [Backend Development](#backend-development)
3. [Frontend Development](#frontend-development)
4. [Testing Feedback Loop](#testing-feedback-loop)
5. [Common Workflows](#common-workflows)
6. [Debugging Techniques](#debugging-techniques)

---

## Development Cycle Overview

### The Iterative Flow

```
┌─────────────────────────────────────────────────────────┐
│ 1. Start Backend Services                               │
│    ./start-all.sh                                       │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 2. Run Frontend (pick platform)                         │
│    ./debug-platform.sh android/ios/desktop/web         │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 3. Test & Gather Feedback                               │
│    - Use the app                                        │
│    - Check logs for errors                             │
│    - Note what needs improvement                       │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 4. Make Changes                                          │
│    - Fix backend API                                    │
│    - Update frontend logic                             │
│    - Improve UI/UX                                      │
└────────────────┬────────────────────────────────────────┘
                 │
                 ▼
┌─────────────────────────────────────────────────────────┐
│ 5. Test Changes                                          │
│    - Restart backend (if needed)                        │
│    - Hot reload frontend (if supported)                │
│    - Verify fix works                                   │
└────────────────┬────────────────────────────────────────┘
                 │
                 └──────────────> Repeat from step 3
```

---

## Backend Development

### Starting the Backend

#### Method 1: Auto-restart on Changes (Recommended)
```bash
# Terminal 1: Start PocketBase
cd pocketbase
./pocketbase serve --dev

# Terminal 2: Start server with auto-reload
./gradlew :server:run -t --continuous

# Terminal 3: Watch logs
./watch-logs.sh
```

#### Method 2: Simple Start
```bash
./start-all.sh
```

### Making Backend Changes

#### 1. API Endpoint Changes

**Example: Add a new endpoint**

```kotlin
// server/src/main/kotlin/love/bside/server/routes/api/v1/UserRoutes.kt

fun Route.userRoutes(userRepository: UserRepository) {
    authenticate("auth-jwt") {
        
        // Add new endpoint
        get("/user/preferences") {
            val userId = call.principal<JWTPrincipal>()
                ?.getClaim("userId", String::class)
                ?: return@get call.respond(HttpStatusCode.Unauthorized)
            
            try {
                val preferences = userRepository.getUserPreferences(userId)
                call.respond(HttpStatusCode.OK, preferences)
            } catch (e: Exception) {
                call.application.environment.log.error("Failed to get preferences", e)
                call.respond(HttpStatusCode.InternalServerError, 
                    mapOf("error" to "Failed to fetch preferences"))
            }
        }
    }
}
```

**Test immediately:**
```bash
# In another terminal
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/api/v1/user/preferences
```

#### 2. Repository Changes

**Example: Add database operation**

```kotlin
// server/src/main/kotlin/love/bside/server/repositories/UserRepository.kt

interface UserRepository {
    suspend fun getUserPreferences(userId: String): UserPreferences
}

class UserRepositoryImpl(
    private val pocketBase: PocketBaseClient
) : UserRepository {
    
    override suspend fun getUserPreferences(userId: String): UserPreferences {
        val record = pocketBase.collection("user_preferences")
            .getOne(userId)
        
        return UserPreferences(
            userId = record["userId"] as String,
            notifications = record["notifications"] as Boolean,
            theme = record["theme"] as String
        )
    }
}
```

#### 3. Validation Changes

**Example: Add request validation**

```kotlin
// server/src/main/kotlin/love/bside/server/validation/RequestValidators.kt

fun validateUserPreferencesUpdate(
    notifications: Boolean?,
    theme: String?
): ValidationResult {
    val errors = mutableListOf<String>()
    
    theme?.let {
        if (it !in listOf("light", "dark", "auto")) {
            errors.add("Invalid theme. Must be: light, dark, or auto")
        }
    }
    
    return if (errors.isEmpty()) {
        ValidationResult.Valid
    } else {
        ValidationResult.Invalid(errors)
    }
}
```

### Backend Testing Workflow

```bash
# 1. Make backend change
# Edit file in server/src/main/kotlin/...

# 2. If using auto-reload, server restarts automatically
# If not, restart manually:
./stop-all.sh
./start-all.sh

# 3. Test the endpoint
curl -X POST http://localhost:8080/api/v1/your-endpoint \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer TOKEN" \
     -d '{"data": "value"}'

# 4. Check logs
tail -f logs/server.log

# 5. Run tests
./gradlew :server:test
```

---

## Frontend Development

### Starting Frontend Development

#### Android Development Loop
```bash
# Terminal 1: Backend running
./start-all.sh

# Terminal 2: Monitor logs
./watch-logs.sh

# Terminal 3: Run Android with live logs
./debug-platform.sh android

# After making changes:
./gradlew :composeApp:installDebug  # Rebuild & install
```

#### iOS Development Loop
```bash
# Terminal 1: Backend
./start-all.sh

# Terminal 2: Open Xcode
open iosApp/iosApp.xcodeproj

# In Xcode:
# 1. Make changes
# 2. Click Run (⌘R)
# 3. View logs in Debug Area (⌘⇧Y)
```

#### Desktop Development Loop
```bash
# Terminal 1: Backend
./start-all.sh

# Terminal 2: Run desktop (restarts on code changes)
./gradlew :composeApp:run --continuous

# Desktop app will restart when you save files
```

#### Web Development Loop
```bash
# Terminal 1: Backend
./start-all.sh

# Terminal 2: Web dev server (hot reload)
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# Browser will auto-reload on changes
# Access: http://localhost:8080
```

### Making Frontend Changes

#### 1. Shared Business Logic

**Example: Update API client**

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/data/api/InternalApiClient.kt

class InternalApiClient(
    private val httpClient: HttpClient,
    private val baseUrl: String
) {
    // Add new API call
    suspend fun getUserPreferences(): Result<UserPreferences> {
        return safeApiCall {
            httpClient.get("$baseUrl/api/v1/user/preferences") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${authToken}")
                }
            }.body()
        }
    }
}
```

**Test in platform app:**
```kotlin
// composeApp/src/commonMain/kotlin/...

@Composable
fun PreferencesScreen() {
    var preferences by remember { mutableStateOf<UserPreferences?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        when (val result = apiClient.getUserPreferences()) {
            is Result.Success -> preferences = result.data
            is Result.Error -> error = result.message
        }
    }
    
    // UI to display preferences
}
```

#### 2. UI Components

**Example: New composable**

```kotlin
// composeApp/src/commonMain/kotlin/love/bside/app/ui/components/PreferencesCard.kt

@Composable
fun PreferencesCard(
    preferences: UserPreferences,
    onUpdate: (UserPreferences) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Preferences",
                style = MaterialTheme.typography.headlineSmall
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Notifications toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Notifications")
                Switch(
                    checked = preferences.notifications,
                    onCheckedChange = { enabled ->
                        onUpdate(preferences.copy(notifications = enabled))
                    }
                )
            }
            
            // Theme selector
            // ... more UI
        }
    }
}
```

#### 3. ViewModel/State Management

**Example: Update view model**

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/viewmodels/PreferencesViewModel.kt

class PreferencesViewModel(
    private val apiClient: InternalApiClient
) {
    private val _state = MutableStateFlow<PreferencesState>(PreferencesState.Loading)
    val state: StateFlow<PreferencesState> = _state.asStateFlow()
    
    fun loadPreferences() {
        viewModelScope.launch {
            _state.value = PreferencesState.Loading
            
            when (val result = apiClient.getUserPreferences()) {
                is Result.Success -> {
                    _state.value = PreferencesState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = PreferencesState.Error(result.message)
                }
            }
        }
    }
    
    fun updatePreferences(preferences: UserPreferences) {
        viewModelScope.launch {
            when (val result = apiClient.updateUserPreferences(preferences)) {
                is Result.Success -> {
                    _state.value = PreferencesState.Success(result.data)
                }
                is Result.Error -> {
                    _state.value = PreferencesState.Error(result.message)
                }
            }
        }
    }
}

sealed class PreferencesState {
    object Loading : PreferencesState()
    data class Success(val preferences: UserPreferences) : PreferencesState()
    data class Error(val message: String) : PreferencesState()
}
```

---

## Testing Feedback Loop

### 1. Gather Feedback While Testing

#### Create a Feedback Template

```bash
# Create feedback.md for tracking issues
cat > feedback.md << 'EOF'
# Testing Feedback - [Date]

## Platform: [Android/iOS/Desktop/Web]

### Issues Found
- [ ] Issue 1: Description
  - Steps to reproduce:
  - Expected behavior:
  - Actual behavior:
  - Logs: [paste relevant logs]

- [ ] Issue 2: Description
  - ...

### Improvements Needed
- [ ] Improvement 1: Description
  - Why needed:
  - Suggested approach:

### Works Well
- ✅ Feature 1
- ✅ Feature 2

### Performance Notes
- API response time: Xms
- UI lag noticed: Yes/No
- Memory usage: Normal/High

### Next Steps
1. Fix issue 1
2. Implement improvement 1
3. ...
EOF
```

#### Monitor Logs While Testing

```bash
# Terminal setup for feedback gathering

# Terminal 1: Backend logs
tail -f logs/server.log | grep -E "ERROR|WARN|Exception"

# Terminal 2: PocketBase logs
tail -f pocketbase/pb_data/logs/*.log

# Terminal 3: App logs (example for Android)
adb logcat -s "B-Side:*" | tee android-session.log

# Terminal 4: Your working terminal
```

### 2. Debug Common Issues

#### API Not Responding

**Check:**
```bash
# Is server running?
curl http://localhost:8080/health

# Is PocketBase running?
curl http://127.0.0.1:8090/api/health

# Check server logs
tail -n 50 logs/server.log

# Check if port is blocked
lsof -i :8080
lsof -i :8090
```

**Fix:**
```bash
./stop-all.sh
./start-all.sh
```

#### Authentication Errors

**Debug:**
```bash
# Check JWT token in app
# Add logging in shared API client:

// shared/src/commonMain/kotlin/...
private fun getAuthHeader(): String {
    val token = tokenStorage.getToken()
    println("DEBUG: Using token: ${token?.take(20)}...")  // Don't log full token!
    return "Bearer $token"
}

# Check server JWT validation
# Add logging in Security.kt:

install(Authentication) {
    jwt("auth-jwt") {
        verifier(jwtVerifier)
        validate { credential ->
            val userId = credential.payload.getClaim("userId").asString()
            println("DEBUG: JWT validated for user: $userId")
            JWTPrincipal(credential.payload)
        }
    }
}
```

#### Data Not Updating

**Debug:**
```bash
# Check if API call succeeds
curl -X GET http://localhost:8080/api/v1/endpoint \
     -H "Authorization: Bearer TOKEN" \
     -v  # verbose mode

# Check PocketBase data directly
cd pocketbase
sqlite3 pb_data/data.db
sqlite> SELECT * FROM users LIMIT 5;

# Or use PocketBase admin UI
open http://127.0.0.1:8090/_/
```

---

## Common Workflows

### Workflow 1: Add New Feature (Full Stack)

```bash
# Example: Add "Favorite Users" feature

# Step 1: Plan the feature
# - What data structure?
# - What API endpoints?
# - What UI components?

# Step 2: Update PocketBase schema
open http://127.0.0.1:8090/_/
# Add "favorites" collection with fields:
# - user_id (relation to users)
# - favorite_user_id (relation to users)
# - created (auto)

# Step 3: Add backend repository method
cat >> server/src/main/kotlin/love/bside/server/repositories/UserRepository.kt << 'EOF'

suspend fun addFavorite(userId: String, favoriteUserId: String): Boolean {
    return pocketBase.collection("favorites").create(
        mapOf(
            "user_id" to userId,
            "favorite_user_id" to favoriteUserId
        )
    ) != null
}

suspend fun getFavorites(userId: String): List<User> {
    val favorites = pocketBase.collection("favorites")
        .getList(filter = "user_id='$userId'", expand = "favorite_user_id")
    
    return favorites.items.map { /* parse user data */ }
}
EOF

# Step 4: Add API endpoints
# Edit server/src/main/kotlin/love/bside/server/routes/api/v1/UserRoutes.kt

# Step 5: Add shared API client method
# Edit shared/src/commonMain/kotlin/love/bside/app/data/api/InternalApiClient.kt

# Step 6: Add UI component
# Edit composeApp/src/commonMain/kotlin/...

# Step 7: Test end-to-end
./gradlew :server:test  # Backend tests
./gradlew :shared:test  # Shared logic tests
./debug-platform.sh android  # Manual testing

# Step 8: Verify on all platforms
./debug-platform.sh ios
./debug-platform.sh desktop
./debug-platform.sh web
```

### Workflow 2: Fix Bug Found in Testing

```bash
# Example: Fix "User profile not loading"

# Step 1: Reproduce the bug
./start-all.sh
./debug-platform.sh android
# Try to load profile -> fails

# Step 2: Check logs
tail -f logs/server.log
# See: "ERROR: User not found: abc123"

# Step 3: Debug backend
# Add logging in UserRepository.kt:
println("DEBUG: Looking for user: $userId")

# Step 4: Restart server to see logs
./stop-all.sh && ./start-all.sh

# Step 5: Test again
# Check new logs

# Step 6: Found issue - userId format mismatch
# Fix in API client:
# Change: userId.trim() to userId.lowercase().trim()

# Step 7: Rebuild frontend
./gradlew :composeApp:installDebug

# Step 8: Test fix
# Profile loads correctly!

# Step 9: Add test to prevent regression
# Write test in shared/src/commonTest/...

# Step 10: Verify on other platforms
./debug-platform.sh ios
./debug-platform.sh desktop
```

### Workflow 3: Improve Performance

```bash
# Example: Speed up user list loading

# Step 1: Measure current performance
# Add timing in ViewModel:
val startTime = System.currentTimeMillis()
val users = apiClient.getUsers()
val duration = System.currentTimeMillis() - startTime
println("Loaded users in: ${duration}ms")

# Step 2: Check logs
# See: "Loaded users in: 2500ms" <- Too slow!

# Step 3: Profile API call
curl -w "@curl-format.txt" http://localhost:8080/api/v1/users
# Create curl-format.txt:
echo "time_total: %{time_total}s\n" > curl-format.txt

# Step 4: Found issue - Loading too much data
# Add pagination to backend:
get("/users") {
    val page = call.parameters["page"]?.toIntOrNull() ?: 1
    val perPage = call.parameters["perPage"]?.toIntOrNull() ?: 20
    // ... implement pagination
}

# Step 5: Update frontend to use pagination
# Implement infinite scroll or load more button

# Step 6: Test improvement
# See: "Loaded users in: 150ms" <- Much better!

# Step 7: Add caching for better UX
// In ViewModel:
private val cachedUsers = mutableMapOf<Int, List<User>>()

# Step 8: Verify on all platforms
```

---

## Debugging Techniques

### Backend Debugging

#### 1. Add Debug Endpoints

```kotlin
// server/src/main/kotlin/love/bside/server/routes/DebugRoutes.kt

fun Route.debugRoutes() {
    route("/debug") {
        get("/status") {
            call.respond(mapOf(
                "server" to "running",
                "pocketbase" to pocketBaseHealthCheck(),
                "memory" to Runtime.getRuntime().freeMemory(),
                "timestamp" to System.currentTimeMillis()
            ))
        }
        
        get("/logs") {
            val logs = File("logs/server.log")
                .readLines()
                .takeLast(100)
            call.respond(logs)
        }
    }
}
```

#### 2. Enhanced Logging

```kotlin
// Add to Monitoring.kt
install(CallLogging) {
    level = Level.DEBUG
    
    filter { call ->
        call.request.path().startsWith("/api")
    }
    
    format { call ->
        val status = call.response.status()
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val userAgent = call.request.headers["User-Agent"]
        val duration = call.processingTimeMillis()
        val body = call.receiveText()  // Be careful with this
        
        buildString {
            append("$method $uri -> $status (${duration}ms)")
            append("\n  User-Agent: $userAgent")
            if (body.isNotEmpty()) {
                append("\n  Body: $body")
            }
        }
    }
}
```

### Frontend Debugging

#### 1. Add Debug Panel

```kotlin
// composeApp/.../ui/components/DebugPanel.kt

@Composable
fun DebugPanel(apiClient: InternalApiClient) {
    var showPanel by remember { mutableStateOf(false) }
    
    if (BuildConfig.DEBUG) {  // Only in debug builds
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { showPanel = !showPanel },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.BugReport, "Debug")
            }
            
            if (showPanel) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(300.dp)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Debug Info", style = MaterialTheme.typography.headlineSmall)
                        
                        Button(onClick = { testApiConnection() }) {
                            Text("Test API")
                        }
                        
                        Button(onClick = { clearCache() }) {
                            Text("Clear Cache")
                        }
                        
                        Text("API URL: ${apiClient.baseUrl}")
                        Text("Auth Token: ${authToken?.take(20)}...")
                    }
                }
            }
        }
    }
}
```

#### 2. Network Request Logging

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/data/api/InternalApiClient.kt

private val httpClient = HttpClient {
    install(Logging) {
        logger = Logger.SIMPLE
        level = LogLevel.ALL  // In debug mode
    }
    
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
    }
    
    // Add request/response interceptor
    install("RequestLogger") {
        onRequest { request, content ->
            println("🌐 REQUEST: ${request.method.value} ${request.url}")
            println("   Headers: ${request.headers.entries()}")
        }
        
        onResponse { response ->
            println("✅ RESPONSE: ${response.status}")
            println("   Body: ${response.bodyAsText()}")
        }
    }
}
```

---

## Quick Reference

### Daily Development Commands

```bash
# Morning routine
./start-all.sh                    # Start backend
./watch-logs.sh                   # Monitor in separate terminal
./debug-platform.sh android       # Start developing

# Make changes -> Test immediately

# Android: Fast iteration
./gradlew :composeApp:installDebug  # Quick rebuild

# iOS: Use Xcode
# Just click Run after changes

# Desktop: Auto-restart
./gradlew :composeApp:run --continuous

# Web: Hot reload
# Just save file, browser auto-refreshes

# End of day
./stop-all.sh
```

### Testing Checklist

- [ ] Backend change made
- [ ] Server restarted (if needed)
- [ ] API tested with curl
- [ ] Logs checked for errors
- [ ] Frontend updated
- [ ] App rebuilt/reloaded
- [ ] Feature tested manually
- [ ] Works on target platform
- [ ] Tested on other platforms
- [ ] Unit tests added
- [ ] Documentation updated

---

**Pro Tip:** Keep a development journal! Track what you changed, why, and the result. This helps when you need to revisit decisions later.
