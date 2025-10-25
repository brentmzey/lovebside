# 🚀 B-Side Quick Reference Card

## Common Tasks

### Build & Run

```bash
# Build everything
./gradlew build

# Build server only
./gradlew :server:build

# Run server (development)
./gradlew :server:run

# Build production JAR
./gradlew :server:shadowJar

# Run production JAR
java -jar server/build/libs/server-all.jar
```

### Database Migrations

```bash
# Run pending migrations
./gradlew :server:runMigrations

# Generate new migration
./gradlew :server:runMigrations --args="generate add_feature_name"

# Check migration status
./gradlew :server:runMigrations --args="status"

# Rollback last migration
./gradlew :server:runMigrations --args="rollback"
```

### Schema Management

```bash
# Validate all schemas
./gradlew :server:runSchemaValidator --args="validate"

# Generate schema documentation
./gradlew :server:runSchemaValidator --args="docs" > SCHEMA_DOCS.md

# Generate DDL statements
./gradlew :server:runSchemaValidator --args="ddl"
```

### Testing Endpoints

```bash
# Health check
curl http://localhost:8080/health

# Register user
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "Test1234!",
    "firstName": "Test",
    "lastName": "User",
    "birthDate": "1990-01-01"
  }'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'

# Get current user (needs token)
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN_HERE"
```

### Platform Builds

```bash
# Android
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug

# Desktop
./gradlew :composeApp:run

# Web
./gradlew :composeApp:jsBrowserDevelopmentRun

# iOS (compile only - use Xcode to build)
./gradlew :composeApp:compileKotlinIosArm64
```

### Environment Setup

```bash
# Development (default)
export APP_ENV=development
export JWT_SECRET="dev-secret-change-me"
export POCKETBASE_URL="https://bside.pockethost.io"

# Production
export APP_ENV=production
export JWT_SECRET="$(openssl rand -base64 64)"
export POCKETBASE_URL="https://bside.pockethost.io"
export ALLOWED_ORIGINS="https://www.bside.love"
```

## File Locations

### Configuration
- `server/src/main/resources/application.conf` - Server config
- `server/build.gradle.kts` - Build config

### Schema & Migrations
- `server/src/main/kotlin/love/bside/server/schema/SchemaDefinition.kt`
- `server/src/main/kotlin/love/bside/server/migrations/versions/`

### Server Core
- `server/src/main/kotlin/love/bside/app/Application.kt` - Entry point
- `server/src/main/kotlin/love/bside/server/routes/` - API routes
- `server/src/main/kotlin/love/bside/server/services/` - Business logic

### Client
- `shared/src/commonMain/kotlin/love/bside/app/data/api/InternalApiClient.kt`

## Documentation

- `ENTERPRISE_IMPLEMENTATION_COMPLETE.md` - What was built
- `ENTERPRISE_DATABASE_GUIDE.md` - Database guide
- `POCKETBASE_SCHEMA.md` - Schema reference
- `DEVELOPER_GUIDE.md` - Development guide
- `README.md` - Project overview

## Troubleshooting

### Server won't start
```bash
# Check configuration
cat server/src/main/resources/application.conf

# Check environment variables
echo $JWT_SECRET
echo $POCKETBASE_URL

# Clean build
./gradlew clean :server:build
```

### Migration issues
```bash
# Check status
./gradlew :server:runMigrations --args="status"

# Validate schema
./gradlew :server:runSchemaValidator --args="validate"
```

### Build cache issues
```bash
# Clean everything
./gradlew clean

# Clear Gradle caches
rm -rf ~/.gradle/caches

# Rebuild
./gradlew build
```

## Ports

- **Server**: 8080 (configurable via PORT env var)
- **Web Dev**: 8080 (Kotlin/JS dev server)
- **PocketBase**: 443 (HTTPS at bside.pockethost.io)

## API Endpoints

### Public
- `GET /health` - Health check

### Auth (Public)
- `POST /api/v1/auth/register` - Register new user
- `POST /api/v1/auth/login` - Login
- `POST /api/v1/auth/refresh` - Refresh token

### User (Protected)
- `GET /api/v1/users/me` - Get current user
- `PUT /api/v1/users/me` - Update profile
- `DELETE /api/v1/users/me` - Delete account

### Values (Protected)
- `GET /api/v1/values` - Get all key values
- `GET /api/v1/users/me/values` - Get user's values
- `POST /api/v1/users/me/values` - Save user's values

### Matches (Protected)
- `GET /api/v1/matches` - Get matches
- `GET /api/v1/matches/discover` - Discover new matches
- `POST /api/v1/matches/:id/like` - Like a match
- `POST /api/v1/matches/:id/pass` - Pass on a match

### Prompts (Protected)
- `GET /api/v1/prompts` - Get all prompts
- `GET /api/v1/users/me/answers` - Get user's answers
- `POST /api/v1/users/me/answers` - Submit answer

---

**Keep this file handy for quick reference!**
