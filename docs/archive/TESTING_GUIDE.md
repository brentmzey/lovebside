# Testing Guide - B-Side Multiplatform App

## Quick Start

### 1. Start the Server
```bash
# Terminal 1
cd /Users/brentzey/bside
./gradlew :server:run

# Wait for: "Application started"
# Server will be at: http://localhost:8080
```

### 2. Test Server Health
```bash
# Terminal 2
curl http://localhost:8080/health
# Expected: {"status":"healthy",...}
```

### 3. Run Integration Tests
```bash
# JVM tests (fastest)
./gradlew :shared:jvmTest

# Android tests
./gradlew :shared:testDebugUnitTest

# All tests
./gradlew :shared:test
```

## Platform Testing

### Android
```bash
./gradlew :composeApp:installDebug
# App launches on device/emulator
```

### Desktop
```bash
./gradlew :composeApp:run
# App window opens
```

### Web
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
# Opens at http://localhost:8080
```

### iOS
```bash
./gradlew :composeApp:compileKotlinIosArm64
open iosApp/iosApp.xcodeproj
# Then run from Xcode
```

## Test Coverage

- **AuthIntegrationTest**: Login, signup, logout
- **ValuesIntegrationTest**: Personality traits
- **MatchIntegrationTest**: Match discovery
- **ProfileIntegrationTest**: Profile management
- **EndToEndIntegrationTest**: Complete user journey

Run specific test:
```bash
./gradlew :shared:jvmTest --tests "*.EndToEndIntegrationTest"
```

## Manual API Testing

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!",
       "passwordConfirm":"Test1234!","firstName":"Test",
       "lastName":"User","birthDate":"1990-01-01","seeking":"BOTH"}'

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!"}'

# Get Profile (save token from login first)
curl http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Troubleshooting

### Server won't start
```bash
# Check port 8080
lsof -i :8080

# Kill process if needed
kill -9 <PID>
```

### Tests fail with "Connection Refused"
Make sure server is running first!

### Android can't connect
Use `10.0.2.2:8080` for Android emulator instead of `localhost:8080`

## Pre-Release Checklist

- [ ] All tests pass: `./gradlew test`
- [ ] Server builds: `./gradlew :server:build`
- [ ] All platforms compile
- [ ] Health check works: `curl http://localhost:8080/health`
- [ ] Migrations applied: `./gradlew :server:runMigrations`
- [ ] Schema valid: `./gradlew :server:runSchemaValidator --args="validate"`

---

For detailed testing info, see `MULTIPLATFORM_INTEGRATION_COMPLETE.md`
