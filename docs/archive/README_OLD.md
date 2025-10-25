# B-Side - Kotlin Multiplatform Dating App

Enterprise-ready dating application built with Kotlin Multiplatform, featuring a secure backend server and native clients for Android, iOS, Web, and Desktop.

---

## Quick Start

```bash
# Build everything
./gradlew build

# Start server
./gradlew :server:run

# Test health endpoint
curl http://localhost:8080/health

# Run tests
./gradlew test
```

---

## Project Structure

```
bside/
├── composeApp/          # Multiplatform UI (Android, iOS, Web, Desktop)
├── shared/              # Shared business logic & data layer
├── server/              # Ktor backend API server
├── iosApp/              # iOS app wrapper
└── pocketbase/          # PocketBase database
```

---

## Build & Run

### All Platforms

```bash
# Build everything
./gradlew build

# Clean build
./gradlew clean build

# Run all tests
./gradlew test
```

### Server

```bash
# Run development server (port 8080)
./gradlew :server:run

# Build production JAR
./gradlew :server:shadowJar

# Run production JAR
java -jar server/build/libs/server-all.jar
```

### Android

```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Install on connected device
./gradlew :composeApp:installDebug

# Build release APK
./gradlew :composeApp:assembleRelease
```

### iOS

```bash
# Build iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64

# Open in Xcode
open iosApp/iosApp.xcodeproj

# Or use Kotlin/Native tasks
./gradlew :composeApp:compileKotlinIosArm64
```

### Desktop (JVM)

```bash
# Run desktop app
./gradlew :composeApp:run

# Build desktop JAR
./gradlew :composeApp:jvmJar

# Package for distribution
./gradlew :composeApp:packageDistributionForCurrentOS
```

### Web (JavaScript)

```bash
# Run development server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Build for production
./gradlew :composeApp:jsBrowserDistribution

# Output: composeApp/build/dist/js/productionExecutable/
```

---

## Testing

### Run Tests

```bash
# All tests
./gradlew test

# Shared module tests (JVM)
./gradlew :shared:jvmTest

# Server tests
./gradlew :server:test

# With coverage
./gradlew test jacocoTestReport
```

### Integration Tests

```bash
# Start server first
./gradlew :server:run &

# Wait for server to start
sleep 5

# Run integration tests
./gradlew :shared:jvmTest

# Stop server
pkill -f "server"
```

---

## Architecture

### Multi-Tier Design

```
┌─────────────────────────────────────┐
│   Clients (Android, iOS, Web, Desktop)
│           ↓ HTTPS + JWT             │
├─────────────────────────────────────┤
│   Server (Ktor on :8080)            │
│   • API Routes (/api/v1/*)          │
│   • Authentication                   │
│   • Business Logic                   │
│           ↓                          │
├─────────────────────────────────────┤
│   Database (PocketBase)             │
│   • Profiles, Matches, etc.         │
└─────────────────────────────────────┘
```

**Security**: Clients never access database directly. All requests go through authenticated server API.

### Key Technologies

- **Kotlin Multiplatform 2.0+** - Shared code across platforms
- **Compose Multiplatform** - Declarative UI framework
- **Ktor** - Backend server framework
- **PocketBase** - Database backend
- **Koin** - Dependency injection
- **Material Design 3** - UI design system

---

## Configuration

### Environment Variables

```bash
# Development
export APP_ENV=development
export JWT_SECRET="dev-secret-key"
export POCKETBASE_URL="http://localhost:8090"

# Production
export APP_ENV=production
export JWT_SECRET="$(openssl rand -base64 64)"
export POCKETBASE_URL="https://your-production-db.com"
```

### Application Config

Edit `server/src/main/resources/application.conf`:

```hocon
ktor {
    deployment {
        port = 8080
    }
}

app {
    environment = ${?APP_ENV}
    jwt {
        secret = ${?JWT_SECRET}
    }
    pocketbase {
        url = ${?POCKETBASE_URL}
    }
}
```

---

## Platform-Specific Notes

### Android

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Build**: `./gradlew :composeApp:assembleDebug`
- **APK**: `composeApp/build/outputs/apk/debug/`

### iOS

- **Min Version**: iOS 14.0
- **Requires**: Xcode 14+, macOS
- **Framework**: Auto-built by Gradle
- **Run**: Open `iosApp/iosApp.xcodeproj` in Xcode

### Web

- **Target**: ES2015+, Modern browsers
- **Dev Server**: `./gradlew :composeApp:jsBrowserDevelopmentRun`
- **Production**: `./gradlew :composeApp:jsBrowserDistribution`
- **Output**: `composeApp/build/dist/js/productionExecutable/`

### Desktop

- **Platforms**: Windows, macOS, Linux
- **JVM**: Requires Java 17+
- **Run**: `./gradlew :composeApp:run`
- **Package**: `./gradlew :composeApp:packageDistributionForCurrentOS`

---

## Database

### Schema

See [POCKETBASE_SCHEMA.md](./POCKETBASE_SCHEMA.md) for complete schema.

**Collections**:
- `users` - Authentication
- `s_profiles` - User profiles
- `s_personality_traits` - Trait values & importance
- `s_proust_answers` - Questionnaire responses
- `s_matches` - Match results
- `s_messages` - Chat messages
- `s_photos` - User photos

### Migrations

```bash
# Run migrations
./gradlew :server:runMigrations

# Validate schema
./gradlew :server:runSchemaValidator --args="validate"
```

---

## API Endpoints

### Authentication

```
POST   /api/v1/auth/register    - Register new user
POST   /api/v1/auth/login       - Login user
POST   /api/v1/auth/logout      - Logout user
GET    /api/v1/auth/refresh     - Refresh token
```

### Profile

```
GET    /api/v1/profile          - Get current user profile
PUT    /api/v1/profile          - Update profile
GET    /api/v1/profile/{id}     - Get user profile by ID
```

### Personality & Matching

```
GET    /api/v1/values           - Get personality traits
PUT    /api/v1/values           - Update trait values
GET    /api/v1/matches          - Get matches
POST   /api/v1/matches/{id}     - Create match
```

### Questionnaire

```
GET    /api/v1/prompts          - Get Proust prompts
GET    /api/v1/prompts/{id}     - Get prompt answers
POST   /api/v1/prompts/{id}     - Submit answer
```

---

## Development

### Code Style

```bash
# Format code
./gradlew ktlintFormat

# Check style
./gradlew ktlintCheck
```

### Debugging

```bash
# Run server with debug
./gradlew :server:run --debug-jvm

# Attach debugger to port 5005
```

### Logs

```bash
# Server logs in console
./gradlew :server:run

# PocketBase logs
tail -f pocketbase/pb_data/logs/
```

---

## Troubleshooting

### Port Already in Use

```bash
# Find process on port 8080
lsof -i :8080

# Kill process
kill -9 <PID>
```

### Build Failures

```bash
# Clean everything
./gradlew clean
rm -rf build */build
rm -rf ~/.gradle/caches

# Rebuild
./gradlew build
```

### iOS Framework Issues

```bash
# Clean Xcode derived data
rm -rf ~/Library/Developer/Xcode/DerivedData

# Rebuild framework
./gradlew :shared:clean
./gradlew :shared:linkDebugFrameworkIosArm64
```

### Tests Fail

```bash
# Make sure server is running
./gradlew :server:run &
sleep 5

# Then run tests
./gradlew :shared:jvmTest
```

---

## Deployment

### Server Deployment

```bash
# Build production JAR
./gradlew :server:shadowJar

# Run on server
export JWT_SECRET="your-production-secret"
export APP_ENV=production
java -jar server/build/libs/server-all.jar
```

### Android Release

```bash
# Build release bundle
./gradlew :composeApp:bundleRelease

# Sign and upload to Google Play
# Bundle: composeApp/build/outputs/bundle/release/
```

### iOS Release

```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Product → Archive
# Upload to App Store Connect
```

### Web Deployment

```bash
# Build production
./gradlew :composeApp:jsBrowserDistribution

# Deploy directory to hosting:
# composeApp/build/dist/js/productionExecutable/
```

---

## Documentation

- **[TODO.md](./TODO.md)** - Task list and roadmap
- **[POCKETBASE_SCHEMA.md](./POCKETBASE_SCHEMA.md)** - Database schema
- **[DESIGN_SYSTEM.md](./DESIGN_SYSTEM.md)** - UI/UX guidelines
- **[docs/archive/](./docs/archive/)** - Archived session notes and guides

---

## Security

- ✅ JWT authentication
- ✅ HTTPS in production
- ✅ Input validation
- ✅ API rate limiting
- ✅ CORS configuration
- ✅ Database access control

---

## License

MIT License - See [LICENSE](./LICENSE)

---

## Status

**Build**: ✅ All platforms building  
**Tests**: ✅ Integration tests passing  
**Production**: 🚧 In development

Last Updated: October 22, 2024
