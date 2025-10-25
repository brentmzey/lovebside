# B-Side App - Next Session Roadmap

**Created**: October 13, 2024  
**For**: Continuing productionalization work  
**Status**: Ready to resume - Clear path forward  

---

## 🎯 Current State Summary

### What Works ✅
- **Enterprise Infrastructure**: Result<T> monad, AppException hierarchy, multi-platform logging
- **Architecture**: Clean Architecture with Repository pattern, Use Cases, MVVM, Koin DI
- **KMP Configuration**: Default Hierarchy Template properly applied, no manual dependsOn calls
- **PocketBase Integration**: Full client wrapper with authentication, CRUD operations, retry logic
- **Documentation**: Comprehensive guides for development, testing, and KMP best practices

### What Needs Fixing 🔧
1. **Compilation Errors** (CRITICAL - Must fix first)
   - Missing Koin imports in commonMain source sets
   - Missing Material Icons Extended imports
   - Logger interface not fully implemented
   - Several UI component issues

2. **Platform Build Issues**
   - WebAssembly target disabled (Koin compatibility issues)
   - Need to verify all platform targets compile individually

3. **Server-Side API** (NOT STARTED)
   - Need internal API layer at `www.bside.love/api/v1`
   - Backend should be sole broker to PocketBase
   - Server-side business logic, jobs, data management

### Build Status
```bash
# Current command fails on:
./gradlew clean build

# Specific failure:
Task :shared:compileCommonMainKotlinMetadata FAILED
```

---

## 🚀 Next Steps - Prioritized

### PHASE 1: Fix Compilation Errors (IMMEDIATE - 2-3 hours)

#### Step 1.1: Fix Koin Dependencies (30 mins)
**File**: `shared/build.gradle.kts`

**Problem**: Koin dependencies are not properly added to commonMain
```kotlin
commonMain.dependencies {
    // Missing explicit Koin dependency
    implementation(libs.koin.core)
}
```

**Action**:
1. Verify `libs.koin.core` is in `gradle/libs.versions.toml`
2. Ensure Koin version supports KMP (3.5.0+)
3. Check that Material Icons Extended is included

**Files to check**:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`

#### Step 1.2: Fix Logger Implementation (45 mins)
**File**: `shared/src/commonMain/kotlin/love/bside/app/core/Logger.kt`

**Problem**: `AppLogger` object doesn't implement all abstract methods from Logger interface

**Current signature**:
```kotlin
interface Logger {
    fun debug(tag: String, message: String, throwable: Throwable? = null)
    fun info(tag: String, message: String, throwable: Throwable? = null)
    fun warn(tag: String, message: String, throwable: Throwable? = null)
    fun error(tag: String, message: String, throwable: Throwable? = null)
}

expect object AppLogger : Logger
```

**Issue**: AppLogger is declared as an object but needs to implement all methods

**Solution**:
```kotlin
// Option 1: Make AppLogger an interface
expect interface AppLogger : Logger

// Option 2: Provide default implementation
object AppLogger : Logger {
    private val platformLogger: Logger = getPlatformLogger()
    
    override fun debug(tag: String, message: String, throwable: Throwable?) {
        platformLogger.debug(tag, message, throwable)
    }
    // ... implement all methods
}

expect fun getPlatformLogger(): Logger
```

#### Step 1.3: Fix Missing Imports in Components (1 hour)
**Files with missing imports**:
- `LoginScreenComponent.kt` - Missing `org.koin.core.component.KoinComponent`, `org.koin.core.component.inject`
- `MatchScreenComponent.kt` - Missing same Koin imports
- `ProfileScreenComponent.kt` - Missing same Koin imports
- `PromptScreenComponent.kt` - Missing same Koin imports
- `QuestionnaireScreenComponent.kt` - Missing same Koin imports
- `ValuesScreenComponent.kt` - Missing same Koin imports

**Pattern for each file**:
```kotlin
// Add these imports
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
```

#### Step 1.4: Fix Material Icons (30 mins)
**File**: `shared/src/commonMain/kotlin/love/bside/app/presentation/main/MainScreen.kt`

**Problem**: Missing imports for Material Icons
```kotlin
// Missing:
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
```

**Action**:
1. Add imports
2. Replace deprecated `Icons.Filled.List` with `Icons.AutoMirrored.Filled.List`

**Build Verification After Phase 1**:
```bash
# Should pass after fixes:
./gradlew :shared:compileCommonMainKotlinMetadata
./gradlew :shared:compileDebugKotlinAndroid
./gradlew :shared:compileKotlinJvm
```

---

### PHASE 2: Platform Build Verification (1-2 hours)

#### Step 2.1: Test Each Platform Individually
```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :shared:compileDebugKotlinAndroid

# JVM/Desktop
./gradlew :composeApp:run
./gradlew :shared:compileKotlinJvm

# iOS (requires Mac)
./gradlew :shared:compileKotlinIosArm64
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:compileKotlinIosX64

# Web JS
./gradlew :composeApp:jsBrowserDevelopmentRun
./gradlew :shared:compileKotlinJs

# Web WASM (currently disabled - revisit later)
# Need to solve Koin compatibility first
```

#### Step 2.2: Fix Platform-Specific Issues
For each platform that fails:
1. Document the error
2. Check if platform-specific `actual` implementations exist
3. Verify dependencies are available for that platform
4. Add missing platform-specific code

**Expected Issues**:
- iOS: May need additional Ktor engine
- Web: May need browser-specific implementations
- Desktop: Should work with JVM implementations

---

### PHASE 3: Backend Server API Design (4-6 hours)

#### Architecture Overview
```
┌─────────────────────────────────────────────────────┐
│                  Client Apps                        │
│  (Android, iOS, Web, Desktop)                       │
└──────────────────┬──────────────────────────────────┘
                   │
                   │ HTTPS
                   │
┌──────────────────▼──────────────────────────────────┐
│           Internal API Server                       │
│         (www.bside.love/api/v1)                     │
│                                                      │
│  ┌─────────────────────────────────────────┐       │
│  │        API Endpoints Layer              │       │
│  │  - Authentication (/auth/*)             │       │
│  │  - Users (/users/*)                     │       │
│  │  - Values (/values/*)                   │       │
│  │  - Matches (/matches/*)                 │       │
│  │  - Prompts (/prompts/*)                 │       │
│  └────────────────┬────────────────────────┘       │
│                   │                                 │
│  ┌────────────────▼────────────────────────┐       │
│  │      Middleware Layer                   │       │
│  │  - Request Validation                   │       │
│  │  - Authentication/Authorization         │       │
│  │  - Rate Limiting                        │       │
│  │  - Logging/Monitoring                   │       │
│  └────────────────┬────────────────────────┘       │
│                   │                                 │
│  ┌────────────────▼────────────────────────┐       │
│  │       Service Layer                     │       │
│  │  - Business Logic                       │       │
│  │  - Data Transformation                  │       │
│  │  - Background Jobs                      │       │
│  │  - Caching                              │       │
│  └────────────────┬────────────────────────┘       │
│                   │                                 │
│  ┌────────────────▼────────────────────────┐       │
│  │      Repository Layer                   │       │
│  │  - Data Access                          │       │
│  │  - Query Building                       │       │
│  │  - Transaction Management               │       │
│  └────────────────┬────────────────────────┘       │
│                   │                                 │
└───────────────────┼─────────────────────────────────┘
                    │ PocketBase SDK
                    │
┌───────────────────▼─────────────────────────────────┐
│            PocketBase Database                      │
│       (https://bside.pockethost.io/)                │
│                                                      │
│  Collections:                                       │
│  - users                                            │
│  - key_values                                       │
│  - user_values                                      │
│  - matches                                          │
│  - prompts                                          │
│  - user_answers                                     │
└─────────────────────────────────────────────────────┘
```

#### Step 3.1: Create Server Module Structure
```bash
server/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── kotlin/
│       │   └── love/
│       │       └── bside/
│       │           └── server/
│       │               ├── Application.kt          # Main entry point
│       │               ├── config/
│       │               │   ├── AppConfig.kt        # Configuration
│       │               │   ├── SecurityConfig.kt   # Security settings
│       │               │   └── DatabaseConfig.kt   # DB connection
│       │               ├── routes/
│       │               │   ├── AuthRoutes.kt       # /api/v1/auth/*
│       │               │   ├── UserRoutes.kt       # /api/v1/users/*
│       │               │   ├── ValueRoutes.kt      # /api/v1/values/*
│       │               │   ├── MatchRoutes.kt      # /api/v1/matches/*
│       │               │   └── PromptRoutes.kt     # /api/v1/prompts/*
│       │               ├── middleware/
│       │               │   ├── AuthMiddleware.kt   # JWT validation
│       │               │   ├── RateLimitMiddleware.kt
│       │               │   ├── ValidationMiddleware.kt
│       │               │   └── LoggingMiddleware.kt
│       │               ├── services/
│       │               │   ├── AuthService.kt      # Auth business logic
│       │               │   ├── UserService.kt      # User operations
│       │               │   ├── ValueService.kt     # Value operations
│       │               │   ├── MatchService.kt     # Matching algorithm
│       │               │   └── PromptService.kt    # Prompt handling
│       │               ├── repositories/
│       │               │   ├── UserRepository.kt   # User data access
│       │               │   ├── ValueRepository.kt  # Value data access
│       │               │   ├── MatchRepository.kt  # Match data access
│       │               │   └── PromptRepository.kt # Prompt data access
│       │               ├── models/
│       │               │   ├── api/                # API models (DTOs)
│       │               │   │   ├── AuthRequest.kt
│       │               │   │   ├── AuthResponse.kt
│       │               │   │   ├── UserResponse.kt
│       │               │   │   └── ErrorResponse.kt
│       │               │   ├── domain/             # Domain models
│       │               │   │   ├── User.kt
│       │               │   │   ├── Value.kt
│       │               │   │   ├── Match.kt
│       │               │   │   └── Prompt.kt
│       │               │   └── db/                 # DB models
│       │               │       └── (mirrors PocketBase schema)
│       │               ├── jobs/
│       │               │   ├── MatchingJob.kt      # Background matching
│       │               │   └── CleanupJob.kt       # Data cleanup
│       │               ├── util/
│       │               │   ├── JwtUtil.kt          # JWT utilities
│       │               │   ├── ValidationUtil.kt   # Validation helpers
│       │               │   └── LoggerUtil.kt       # Logging helpers
│       │               └── di/
│       │                   └── ServerModule.kt     # Koin DI config
│       └── resources/
│           ├── application.conf                    # Server config
│           └── logback.xml                         # Logging config
└── Dockerfile                                       # Container config
```

#### Step 3.2: Technology Stack for Server
```kotlin
// build.gradle.kts dependencies
dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core:2.3.7")
    implementation("io.ktor:ktor-server-netty:2.3.7")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-server-auth:2.3.7")
    implementation("io.ktor:ktor-server-auth-jwt:2.3.7")
    implementation("io.ktor:ktor-server-cors:2.3.7")
    implementation("io.ktor:ktor-server-call-logging:2.3.7")
    implementation("io.ktor:ktor-server-status-pages:2.3.7")
    implementation("io.ktor:ktor-server-rate-limit:2.3.7")
    
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:1.4.14")
    
    // DI
    implementation("io.insert-koin:koin-ktor:3.5.3")
    implementation("io.insert-koin:koin-logger-slf4j:3.5.3")
    
    // PocketBase Client (from shared module)
    implementation(project(":shared"))
    
    // Configuration
    implementation("com.typesafe:config:1.4.3")
    
    // Testing
    testImplementation("io.ktor:ktor-server-test-host:2.3.7")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}
```

#### Step 3.3: Server Implementation Priorities

**Priority 1: Authentication Flow**
```kotlin
// routes/AuthRoutes.kt
fun Route.authRoutes(authService: AuthService) {
    route("/api/v1/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            when (val result = authService.login(request)) {
                is Result.Success -> call.respond(HttpStatusCode.OK, result.data)
                is Result.Error -> call.respond(
                    HttpStatusCode.Unauthorized,
                    ErrorResponse(result.exception.message)
                )
            }
        }
        
        post("/register") { /* ... */ }
        post("/refresh") { /* ... */ }
        post("/logout") { /* ... */ }
    }
}
```

**Priority 2: User Management**
```kotlin
// routes/UserRoutes.kt
fun Route.userRoutes(userService: UserService) {
    route("/api/v1/users") {
        // Requires authentication
        authenticate("jwt") {
            get("/me") {
                val userId = call.principal<UserPrincipal>()?.userId
                // ... fetch user profile
            }
            
            put("/me") {
                // ... update user profile
            }
        }
    }
}
```

**Priority 3: Values & Questionnaire**
**Priority 4: Matching System**
**Priority 5: Prompts & Responses**

---

### PHASE 4: Error Handling & Logging Enhancement (2-3 hours)

#### Step 4.1: Structured Logging
```kotlin
// Add correlation IDs to all requests
install(CallLogging) {
    level = Level.INFO
    format { call ->
        val correlationId = call.request.headers["X-Correlation-ID"] 
            ?: UUID.randomUUID().toString()
        call.attributes.put(CorrelationIdKey, correlationId)
        
        val status = call.response.status()
        val method = call.request.httpMethod.value
        val uri = call.request.uri
        val duration = call.processingTimeMillis()
        
        "[$correlationId] $method $uri - $status (${duration}ms)"
    }
}
```

#### Step 4.2: Error Response Standardization
```kotlin
@Serializable
data class ErrorResponse(
    val error: ErrorDetail,
    val correlationId: String,
    val timestamp: String = Clock.System.now().toString()
)

@Serializable
data class ErrorDetail(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

// Usage in error handlers
install(StatusPages) {
    exception<ValidationException> { call, cause ->
        call.respond(
            HttpStatusCode.BadRequest,
            ErrorResponse(
                error = ErrorDetail(
                    code = "VALIDATION_ERROR",
                    message = cause.message ?: "Validation failed",
                    details = cause.errors
                ),
                correlationId = call.correlationId()
            )
        )
    }
}
```

#### Step 4.3: Client Error Handling
Update all clients to:
1. Display user-friendly error messages
2. Log technical details for debugging
3. Handle network errors gracefully
4. Implement retry logic with exponential backoff
5. Show appropriate loading/error states

---

### PHASE 5: Testing Strategy (3-4 hours)

#### Step 5.1: Unit Tests
```kotlin
// Test service layer
class AuthServiceTest {
    private val mockRepository = mockk<UserRepository>()
    private val authService = AuthService(mockRepository)
    
    @Test
    fun `login with valid credentials returns token`() = runTest {
        // Given
        val request = LoginRequest("test@example.com", "password")
        coEvery { mockRepository.findByEmail(any()) } returns Result.Success(testUser)
        
        // When
        val result = authService.login(request)
        
        // Then
        assertTrue(result is Result.Success)
        assertNotNull((result as Result.Success).data.token)
    }
}
```

#### Step 5.2: Integration Tests
```kotlin
// Test API endpoints
class AuthRoutesTest {
    @Test
    fun `POST auth login with valid credentials returns 200`() = testApplication {
        application {
            configureRouting()
        }
        
        val response = client.post("/api/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody("""{"email": "test@example.com", "password": "password"}""")
        }
        
        assertEquals(HttpStatusCode.OK, response.status)
    }
}
```

#### Step 5.3: E2E Tests
```kotlin
// Test complete user flows
@Test
fun `complete registration and login flow`() = runTest {
    // 1. Register new user
    // 2. Verify email (mock)
    // 3. Login
    // 4. Fetch profile
    // 5. Update profile
}
```

---

### PHASE 6: Performance & Scalability (2-3 hours)

#### Step 6.1: Caching Strategy
```kotlin
// In-memory cache with TTL
class CacheService {
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    
    suspend fun <T> getOrFetch(
        key: String,
        ttl: Duration = 5.minutes,
        fetch: suspend () -> T
    ): T {
        val entry = cache[key]
        if (entry != null && !entry.isExpired()) {
            return entry.value as T
        }
        
        val value = fetch()
        cache[key] = CacheEntry(value, Clock.System.now() + ttl)
        return value
    }
}
```

#### Step 6.2: Connection Pooling
```kotlin
// PocketBase client with connection pooling
val httpClient = HttpClient(CIO) {
    engine {
        maxConnectionsCount = 100
        endpoint {
            maxConnectionsPerRoute = 10
            keepAliveTime = 5000
            connectTimeout = 5000
            requestTimeout = 10000
        }
    }
}
```

#### Step 6.3: Rate Limiting
```kotlin
install(RateLimit) {
    register(RateLimitName("auth")) {
        rateLimiter(limit = 5, refillPeriod = 60.seconds)
        requestKey { call ->
            call.request.origin.remoteHost
        }
    }
}
```

---

### PHASE 7: Security Hardening (2-3 hours)

#### Step 7.1: Environment-Based Configuration
```kotlin
// application.conf
ktor {
    deployment {
        port = ${PORT}
    }
    application {
        modules = [ love.bside.server.ApplicationKt.module ]
    }
}

app {
    environment = ${APP_ENV}  // dev, staging, prod
    
    jwt {
        secret = ${JWT_SECRET}
        issuer = ${JWT_ISSUER}
        audience = ${JWT_AUDIENCE}
        realm = "bside"
        expiresIn = 3600000  // 1 hour
    }
    
    pocketbase {
        url = ${POCKETBASE_URL}
        adminEmail = ${POCKETBASE_ADMIN_EMAIL}
        adminPassword = ${POCKETBASE_ADMIN_PASSWORD}
    }
    
    cors {
        allowedHosts = ${?CORS_ALLOWED_HOSTS}
    }
}
```

#### Step 7.2: Security Headers
```kotlin
install(DefaultHeaders) {
    header("X-Content-Type-Options", "nosniff")
    header("X-Frame-Options", "DENY")
    header("X-XSS-Protection", "1; mode=block")
    header("Strict-Transport-Security", "max-age=31536000")
}
```

#### Step 7.3: Input Validation
```kotlin
object Validators {
    fun validateEmail(email: String): ValidationResult {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$".toRegex()
        return when {
            email.isBlank() -> ValidationResult.Error("Email is required")
            !email.matches(emailRegex) -> ValidationResult.Error("Invalid email format")
            else -> ValidationResult.Valid
        }
    }
}
```

---

### PHASE 8: Deployment & Operations (3-4 hours)

#### Step 8.1: Docker Configuration
```dockerfile
# Dockerfile
FROM gradle:8.5-jdk11 AS build
WORKDIR /app
COPY . .
RUN gradle :server:shadowJar --no-daemon

FROM openjdk:11-jre-slim
WORKDIR /app
COPY --from=build /app/server/build/libs/server-all.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

#### Step 8.2: Health Checks
```kotlin
routing {
    get("/health") {
        call.respond(
            HttpStatusCode.OK,
            mapOf(
                "status" to "UP",
                "timestamp" to Clock.System.now().toString(),
                "version" to BuildConfig.VERSION
            )
        )
    }
    
    get("/ready") {
        // Check dependencies (DB, cache, etc.)
        val dbStatus = checkDatabaseConnection()
        if (dbStatus) {
            call.respond(HttpStatusCode.OK, mapOf("status" to "READY"))
        } else {
            call.respond(HttpStatusCode.ServiceUnavailable, mapOf("status" to "NOT_READY"))
        }
    }
}
```

#### Step 8.3: Monitoring
```kotlin
// Metrics endpoint
get("/metrics") {
    call.respond(
        mapOf(
            "requests_total" to requestCounter.get(),
            "requests_active" to activeRequests.get(),
            "errors_total" to errorCounter.get(),
            "uptime_seconds" to uptimeSeconds.get()
        )
    )
}
```

---

## 📊 Success Criteria

### Build Success
- [ ] `./gradlew clean build` passes with zero errors
- [ ] All platform targets compile successfully
- [ ] All tests pass
- [ ] No warnings for critical issues

### Functionality
- [ ] User can register/login/logout
- [ ] User can complete questionnaire
- [ ] User can view their values
- [ ] User can see matches
- [ ] User can respond to prompts
- [ ] All data persists correctly

### Quality
- [ ] No unhandled exceptions in normal flows
- [ ] All errors have user-friendly messages
- [ ] Loading states show appropriately
- [ ] Offline mode works for cached data
- [ ] Performance is acceptable (< 1s for most operations)

### Security
- [ ] Authentication required for protected endpoints
- [ ] Authorization enforced (users see only their data)
- [ ] Input validation prevents injection attacks
- [ ] Sensitive data is encrypted
- [ ] HTTPS enforced in production

### Operations
- [ ] Health checks respond correctly
- [ ] Logging provides useful debugging info
- [ ] Metrics are collected
- [ ] Deployment is automated
- [ ] Rollback procedure documented

---

## 🛠️ Commands Reference

### Development
```bash
# Clean build (full rebuild)
./gradlew clean build

# Build specific modules
./gradlew :shared:build
./gradlew :composeApp:build
./gradlew :server:build

# Run specific platforms
./gradlew :composeApp:run                    # Desktop
./gradlew :composeApp:assembleDebug          # Android
./gradlew :composeApp:jsBrowserDevelopmentRun    # Web JS
./gradlew :server:run                        # Server

# Run tests
./gradlew test
./gradlew :shared:test
./gradlew :server:test
```

### Debugging
```bash
# Verbose build output
./gradlew build --info
./gradlew build --debug

# Stacktrace for errors
./gradlew build --stacktrace

# Dependency insight
./gradlew :shared:dependencies
./gradlew :shared:dependencies --configuration commonMainImplementation
```

### Code Quality
```bash
# Format code
./gradlew ktlintFormat

# Check code style
./gradlew ktlintCheck

# Run detekt (static analysis)
./gradlew detekt
```

---

## 📝 Notes for Next Session

### Quick Start Checklist
1. ✅ Read this document
2. 🔲 Fix compilation errors (Phase 1)
3. 🔲 Verify platform builds (Phase 2)
4. 🔲 Start server implementation (Phase 3)

### Important Files to Review
- `shared/build.gradle.kts` - Dependencies and platform configuration
- `shared/src/commonMain/kotlin/love/bside/app/core/Logger.kt` - Needs implementation
- `shared/src/commonMain/kotlin/love/bside/app/di/AppModule.kt` - Koin configuration
- `gradle/libs.versions.toml` - Version catalog

### Known Gotchas
- Koin doesn't support WebAssembly yet - WasmJS target disabled
- Material Icons need explicit import
- Logger expect/actual needs careful implementation
- iOS targets require Xcode on macOS

### Time Estimates
- **Phase 1** (Compilation): 2-3 hours
- **Phase 2** (Platform Verification): 1-2 hours
- **Phase 3** (Server API): 4-6 hours
- **Phase 4** (Error Handling): 2-3 hours
- **Phase 5** (Testing): 3-4 hours
- **Phase 6** (Performance): 2-3 hours
- **Phase 7** (Security): 2-3 hours
- **Phase 8** (Deployment): 3-4 hours

**Total**: ~20-30 hours of focused work

---

## 🎉 End Goal

A fully production-ready Kotlin Multiplatform app with:

✅ Clean, maintainable code following best practices  
✅ Comprehensive error handling and logging  
✅ Secure authentication and authorization  
✅ High performance and scalability  
✅ Great UX on all platforms  
✅ Excellent DX for developers  
✅ Full test coverage  
✅ Easy deployment and operations  
✅ Complete documentation  

---

**Ready to continue? Start with Phase 1, Step 1.1** 🚀
