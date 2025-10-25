# Enterprise-Level Database Management Guide

## Overview

This guide covers database management, migrations, schema validation, and type safety for the B-Side application. The system is designed for enterprise-level reliability with proper version control, rollback capabilities, and comprehensive validation.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Application                        │
│  - Multiplatform Clients (Android/iOS/Web/Desktop)  │
└──────────────────┬──────────────────────────────────┘
                   │ HTTPS + JWT
┌──────────────────▼──────────────────────────────────┐
│              Internal API (/api/v1/*)                │
│  - Authentication & Authorization                    │
│  - Request Validation                                │
│  - Type-safe DTOs                                    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│               Service Layer                          │
│  - Business Logic                                    │
│  - Data Transformation                               │
│  - Validation & Rules                                │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│            Repository Layer                          │
│  - Data Access                                       │
│  - Model Mapping (API ↔ Domain ↔ DB)                │
│  - Query Building                                    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│          PocketBase Client                           │
│  - HTTP Communication                                │
│  - Authentication                                    │
│  - Error Handling                                    │
└──────────────────┬──────────────────────────────────┘
                   │
┌──────────────────▼──────────────────────────────────┐
│         PocketBase Database                          │
│  - Collections & Schemas                             │
│  - Indexes & Constraints                             │
│  - API Rules & Permissions                           │
└─────────────────────────────────────────────────────┘
```

## Database Schema Management

### 1. Schema Definition

All schemas are defined in code at `server/src/main/kotlin/love/bside/server/schema/SchemaDefinition.kt`.

This provides:
- **Type safety**: Schemas are Kotlin objects, not strings
- **Version control**: Schema changes tracked in git
- **Validation**: Compile-time checks for schema correctness
- **Documentation**: Auto-generated from code

### 2. Collections

The B-Side app uses the following collections:

| Collection | Purpose | Records (est.) |
|------------|---------|---------------|
| `users` | User authentication (PocketBase system) | 1M+ |
| `s_profiles` | User profiles and bio | 1M+ |
| `s_key_values` | Master list of personality traits | ~100 |
| `s_user_values` | User's selected values & importance | 10M+ |
| `s_prompts` | Proust-style questionnaire prompts | ~50 |
| `s_user_answers` | User answers to prompts | 10M+ |
| `s_matches` | Calculated compatibility matches | 100M+ |
| `s_migrations` | Migration tracking | <100 |

### 3. Schema Validation

Validate your schema matches expected structure:

```bash
# Validate all schemas
./gradlew :server:runSchemaValidator --args="validate"

# Generate documentation
./gradlew :server:runSchemaValidator --args="docs" > SCHEMA_DOCS.md

# Generate DDL
./gradlew :server:runSchemaValidator --args="ddl"
```

## Database Migrations

### Overview

Migrations are versioned, ordered changes to the database schema. Each migration:
- Has a unique version number
- Can be applied (`up`) or rolled back (`down`)
- Is tracked to prevent duplicate application
- Includes execution timing for performance monitoring

### Creating a Migration

```bash
# Generate a new migration template
./gradlew :server:runMigrations --args="generate add_user_photos"
```

This creates a new migration file with template code. Edit the file to implement your changes:

```kotlin
package love.bside.server.migrations.versions

import love.bside.server.migrations.Migration
import love.bside.server.migrations.MigrationResult

class Migration2_AddUserPhotos : Migration {
    override val version = 2
    override val name = "add_user_photos"
    override val description = "Add photo support to user profiles"
    
    override suspend fun up(): MigrationResult {
        return try {
            // Add 'photos' field to s_profiles collection
            // - Type: FILE
            // - Multiple: true (up to 6 photos)
            // - Max size: 5MB per photo
            
            MigrationResult.Success("Added photos field to profiles")
        } catch (e: Exception) {
            MigrationResult.Failure("Failed: ${e.message}", e)
        }
    }
    
    override suspend fun down(): MigrationResult {
        return try {
            // Remove 'photos' field from s_profiles collection
            
            MigrationResult.Success("Removed photos field from profiles")
        } catch (e: Exception) {
            MigrationResult.Failure("Rollback failed: ${e.message}", e)
        }
    }
}
```

### Running Migrations

```bash
# Run all pending migrations
./gradlew :server:runMigrations

# or explicitly
./gradlew :server:runMigrations --args="run"

# Check migration status
./gradlew :server:runMigrations --args="status"

# Rollback last migration
./gradlew :server:runMigrations --args="rollback"
```

### Migration Best Practices

1. **Never modify applied migrations** - create a new one instead
2. **Test rollback** - ensure `down()` truly reverses `up()`
3. **Keep migrations small** - one logical change per migration
4. **Backup before running** - especially in production
5. **Document changes** - use clear names and descriptions

## Type Safety

### Three-Layer Model Architecture

The app uses three distinct model layers for type safety:

```
API Models (DTOs) ← → Domain Models ← → DB Models
      ↑                    ↑                 ↑
   External            Business          Database
  Interface             Logic            Storage
```

#### 1. API Models (DTOs)
- Location: `server/src/main/kotlin/love/bside/server/models/api/`
- Purpose: External API contract
- Serialized to/from JSON
- Validation on input

Example:
```kotlin
@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String
)
```

#### 2. Domain Models
- Location: `server/src/main/kotlin/love/bside/server/models/domain/`
- Purpose: Business logic
- Rich behavior and validation
- Platform-independent

Example:
```kotlin
data class User(
    val id: UserId,
    val email: Email,
    val profile: Profile,
    val createdAt: Instant
) {
    fun isAdult(): Boolean = profile.age >= 18
}
```

#### 3. DB Models
- Location: `server/src/main/kotlin/love/bside/server/models/db/`
- Purpose: Database representation
- Maps directly to PocketBase collections
- Optimized for storage

Example:
```kotlin
@Serializable
data class ProfileRecord(
    val id: String,
    val userId: String,
    val firstName: String,
    val lastName: String,
    val birthDate: String,
    val created: String,
    val updated: String
)
```

### Model Mapping

Mappers transform between layers:

```kotlin
// API → Domain
fun RegisterRequest.toDomain(): User {
    return User(
        id = UserId.generate(),
        email = Email(email),
        profile = Profile(
            firstName = firstName,
            lastName = lastName,
            birthDate = LocalDate.parse(birthDate)
        ),
        createdAt = Clock.System.now()
    )
}

// Domain → DB
fun User.toDbModel(): UserRecord {
    return UserRecord(
        id = id.value,
        email = email.value,
        // ... etc
    )
}

// DB → Domain
fun UserRecord.toDomain(): User {
    return User(
        id = UserId(id),
        email = Email(email),
        // ... etc
    )
}
```

## Authorization & Authentication

### JWT Token Flow

```
1. Client → POST /auth/login
   Body: { email, password }

2. Server validates credentials with PocketBase

3. Server generates JWT token
   - Payload: userId, email, roles
   - Signed with secret key
   - Expiration: 1 hour (configurable)

4. Server returns:
   {
     "token": "eyJ...",
     "refreshToken": "abc...",
     "user": { ... }
   }

5. Client stores tokens securely

6. Client includes token in subsequent requests:
   Authorization: Bearer eyJ...

7. Server validates token on protected routes

8. Token expires → Client uses refreshToken
   POST /auth/refresh
   Body: { refreshToken }
```

### Protected Routes

All routes under `/api/v1/` except auth endpoints require authentication:

```kotlin
route("/api/v1") {
    // Public routes
    authRoutes()  // /auth/login, /auth/register
    
    // Protected routes - require JWT
    authenticate("auth-jwt") {
        userRoutes()    // /users/me
        valuesRoutes()  // /values, /users/me/values
        matchRoutes()   // /matches, /matches/discover
        promptRoutes()  // /prompts, /users/me/answers
    }
}
```

### Permission Levels

1. **Public**: Anyone (e.g., health check)
2. **Authenticated**: Any logged-in user
3. **Owner**: User can only access their own data
4. **Admin**: Server administrators only
5. **System**: Server internal operations only

### PocketBase API Rules

Collections have fine-grained access control:

```javascript
// Example: s_profiles collection

listRule: "@request.auth.id != \"\""
// Authenticated users can list profiles

viewRule: "@request.auth.id != \"\" && (userId = @request.auth.id || @request.auth.id != \"\")"
// Users can view their own profile and others' profiles (for matching)

createRule: "@request.auth.id != \"\" && userId = @request.auth.id"
// Users can only create their own profile

updateRule: "@request.auth.id != \"\" && userId = @request.auth.id"
// Users can only update their own profile

deleteRule: "@request.auth.id != \"\" && userId = @request.auth.id"
// Users can only delete their own profile
```

## Configuration Management

### Environment-Based Configuration

Configuration is loaded from `server/src/main/resources/application.conf`:

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}  # Override with env var
    }
}

jwt {
    secret = "default-secret"
    secret = ${?JWT_SECRET}  # ALWAYS override in production
}
```

### Environment Variables (Production)

```bash
# Required
export JWT_SECRET="your-very-long-random-secret-key-here"
export POCKETBASE_URL="https://bside.pockethost.io"

# Optional
export APP_ENV="production"
export PORT="8080"
export JWT_EXPIRATION="3600000"  # 1 hour
export ALLOWED_ORIGINS="https://www.bside.love,https://app.bside.love"
```

### Docker Environment

```dockerfile
ENV APP_ENV=production
ENV JWT_SECRET=${JWT_SECRET}
ENV POCKETBASE_URL=${POCKETBASE_URL}
```

## Performance & Scalability

### Database Indexes

All high-traffic queries have indexes:

```sql
-- User lookups
CREATE INDEX idx_userId ON s_profiles(userId);

-- Match discovery
CREATE INDEX idx_compatibilityScore ON s_matches(compatibilityScore);

-- Value filtering
CREATE INDEX idx_category ON s_key_values(category);

-- Composite indexes for complex queries
CREATE UNIQUE INDEX idx_user_keyValue ON s_user_values(userId, keyValueId);
```

### Connection Pooling

PocketBase client uses connection pooling:

```kotlin
val client = HttpClient {
    engine {
        threadsCount = 4
        pipelining = true
    }
}
```

### Caching Strategy

```kotlin
// Cache frequently accessed data
private val keyValuesCache = ConcurrentHashMap<String, List<KeyValue>>()

suspend fun getKeyValues(): List<KeyValue> {
    return keyValuesCache.getOrPut("all") {
        repository.getAllKeyValues()
    }
}
```

### Rate Limiting

```kotlin
install(RateLimit) {
    global {
        rateLimiter(limit = 100, refillPeriod = 60.seconds)
    }
    register("api") {
        rateLimiter(limit = 30, refillPeriod = 60.seconds)
    }
}
```

## Monitoring & Logging

### Structured Logging

```kotlin
logInfo("User registration successful", mapOf(
    "userId" to userId,
    "email" to email,
    "timestamp" to Clock.System.now()
))

logError("Match calculation failed", mapOf(
    "userId" to userId,
    "error" to error.message
))
```

### Health Checks

```bash
curl http://localhost:8080/health

{
  "status": "healthy",
  "version": "1.0.0",
  "timestamp": "2024-10-17T12:00:00Z",
  "database": "connected",
  "migrations": "up-to-date"
}
```

### Metrics to Track

1. **API Response Times**: p50, p95, p99
2. **Error Rates**: 4xx, 5xx by endpoint
3. **Database Query Times**: slow query log
4. **Match Calculation Performance**: avg time per user
5. **Authentication**: success/failure rates
6. **Resource Usage**: memory, CPU, connections

## Deployment

### Pre-Deployment Checklist

- [ ] All migrations tested and run
- [ ] Schema validated
- [ ] Environment variables set
- [ ] JWT secret is strong and random
- [ ] CORS origins configured
- [ ] Rate limiting configured
- [ ] Logging configured
- [ ] Database backed up
- [ ] Health checks passing
- [ ] Load testing completed

### Production Deployment

```bash
# 1. Build production JAR
./gradlew :server:shadowJar

# 2. Run migrations
./gradlew :server:runMigrations

# 3. Start server
java -jar server/build/libs/server-all.jar
```

### Docker Deployment

```bash
# Build image
docker build -t bside-server:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -e JWT_SECRET=$JWT_SECRET \
  -e POCKETBASE_URL=$POCKETBASE_URL \
  --name bside-server \
  bside-server:latest
```

## Troubleshooting

### Common Issues

**Issue**: Migrations won't run
**Solution**: Check s_migrations collection exists and is accessible

**Issue**: Authentication fails
**Solution**: Verify JWT_SECRET matches between server instances

**Issue**: Schema validation fails
**Solution**: Run `./gradlew :server:runSchemaValidator --args="validate"` for details

**Issue**: Slow queries
**Solution**: Check indexes exist with `EXPLAIN` on slow queries

## Further Reading

- [PocketBase Documentation](https://pocketbase.io/docs/)
- [Ktor Server Documentation](https://ktor.io/docs/)
- [JWT Best Practices](https://tools.ietf.org/html/rfc8725)
- [API Security Checklist](https://github.com/shieldfy/API-Security-Checklist)

---

**Last Updated**: October 17, 2024  
**Maintainer**: Development Team  
**Version**: 1.0.0
