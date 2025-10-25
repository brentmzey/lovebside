# B-Side Quick Reference Cheat Sheet 📋

## Essential Commands

### Start/Stop Services
```bash
./start-all.sh          # Start PocketBase + API Server
./stop-all.sh           # Stop all services
./watch-logs.sh         # Monitor logs in split view (requires tmux)
```

### Development Modes (NEW!)
```bash
# Backend development with auto-reload
./dev-loop.sh backend

# Frontend development (pick platform)
./dev-loop.sh frontend android
./dev-loop.sh frontend ios
./dev-loop.sh frontend desktop
./dev-loop.sh frontend web

# Full-stack development (backend + frontend)
./dev-loop.sh fullstack android
./dev-loop.sh fullstack desktop

# Test-driven development
./dev-loop.sh test
```

### Run Platforms
```bash
# Android
./debug-platform.sh android
# or
./gradlew :composeApp:installDebug && adb logcat -s "B-Side:*"

# iOS
./debug-platform.sh ios
# or
open iosApp/iosApp.xcodeproj

# Desktop
./debug-platform.sh desktop
# or
./gradlew :composeApp:run

# Web
./debug-platform.sh web
# or
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Debug Specific Components
```bash
./debug-platform.sh server      # Debug API server
./debug-platform.sh pocketbase  # Debug PocketBase
./debug-platform.sh android     # Debug Android app
./debug-platform.sh ios         # Debug iOS app
./debug-platform.sh desktop     # Debug desktop app
./debug-platform.sh web         # Debug web app
```

## Service URLs

| Service | URL | Purpose |
|---------|-----|---------|
| PocketBase API | http://127.0.0.1:8090 | Database API |
| PocketBase Admin | http://127.0.0.1:8090/_/ | Admin dashboard |
| Internal API | http://localhost:8080 | Ktor API server |
| Health Check | http://localhost:8080/health | Server health |
| Web App | http://localhost:8080 | Web client (dev mode) |

## Logs

### View Logs
```bash
# All logs (split view)
./watch-logs.sh

# Individual logs
tail -f logs/pocketbase.log
tail -f logs/server.log

# Android logs
adb logcat -s "B-Side:*"

# iOS logs
xcrun simctl spawn booted log stream --predicate 'process == "iosApp"'

# Server logs only
tail -f logs/bside-server.log
```

### Clear Logs
```bash
# Android
adb logcat -c

# Server
rm logs/*.log

# PocketBase
rm pocketbase/pb_data/logs/*.log
```

## Testing

### All Tests
```bash
./gradlew test                  # All unit tests
./gradlew :shared:test          # Shared module tests
./gradlew :server:test          # Server tests
./gradlew :pocketbase-kt-sdk:test  # SDK tests
```

### Platform Tests
```bash
./gradlew :shared:testDebugUnitTest          # Android tests
./gradlew :shared:iosSimulatorArm64Test      # iOS tests
./gradlew :shared:jvmTest                    # JVM tests
```

### Integration Tests
```bash
# Start services first
./start-all.sh

# Then run tests
./gradlew :server:integrationTest
```

## Build Commands

### Build All
```bash
./gradlew build                 # Build everything
./gradlew clean build           # Clean + build
```

### Build Specific Platforms
```bash
# Android APK
./gradlew :composeApp:assembleDebug
# Output: composeApp/build/outputs/apk/debug/composeApp-debug.apk

# iOS
./gradlew :composeApp:iosSimulatorArm64Run

# Desktop distributable
./gradlew :composeApp:createDistributable
# Output: composeApp/build/compose/binaries/main/app/

# Web production bundle
./gradlew :composeApp:jsBrowserProductionWebpack
# Output: composeApp/build/dist/js/productionExecutable/
```

### SDK Build
```bash
# Compile SDK for all platforms
./gradlew :pocketbase-kt-sdk:build

# Publish to local Maven
./gradlew :pocketbase-kt-sdk:publishToMavenLocal
```

## Troubleshooting

### Kill Stuck Processes
```bash
# Kill by port
lsof -ti:8080 | xargs kill -9   # Server
lsof -ti:8090 | xargs kill -9   # PocketBase

# Or use stop script
./stop-all.sh
```

### Clean Everything
```bash
./gradlew clean
rm -rf build/
rm -rf ~/.gradle/caches/
./gradlew --stop
```

### Reset Database
```bash
cd pocketbase
mv pb_data pb_data.backup
./pocketbase serve
```

### Android Issues
```bash
adb kill-server && adb start-server
adb uninstall love.bside.app
./gradlew :composeApp:cleanDebug
```

### iOS Issues
```bash
xcrun simctl erase all                                    # Reset simulators
rm -rf ~/Library/Developer/Xcode/DerivedData/*           # Clear build cache
./gradlew :composeApp:cleanIosSimulatorArm64
```

## Health Checks

### Quick Status Check
```bash
curl http://localhost:8080/health        # Server
curl http://127.0.0.1:8090/api/health   # PocketBase

# All at once
curl http://localhost:8080/health && \
curl http://127.0.0.1:8090/api/health && \
echo "✅ All services healthy"
```

### Check Running Services
```bash
# View processes
ps aux | grep -E "pocketbase|server" | grep -v grep

# View PIDs
cat logs/pocketbase.pid
cat logs/server.pid

# Check ports
lsof -i :8080  # Server
lsof -i :8090  # PocketBase
```

## Development Workflow

### Typical Session
```bash
# 1. Start backend services
./start-all.sh

# 2. Monitor logs (in separate terminal)
./watch-logs.sh

# 3. Run your platform
./debug-platform.sh android   # or ios, desktop, web

# 4. Make changes, test
# ... edit code ...
./gradlew :shared:test

# 5. End session
./stop-all.sh
```

### Hot Reload
```bash
# Server (auto-restart on changes)
./gradlew :server:run -t --continuous

# Web (hot reload)
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
```

## Git Workflow

### Before Commit
```bash
# Run tests
./gradlew test

# Check build
./gradlew build

# Format code (if using ktlint)
./gradlew ktlintFormat
```

## Environment Variables

Create `.env` file in root:
```bash
POCKETBASE_URL=http://127.0.0.1:8090
POCKETBASE_ADMIN_EMAIL=admin@bside.local
POCKETBASE_ADMIN_PASSWORD=your_password
JWT_SECRET=your_jwt_secret_min_32_chars
SERVER_PORT=8080
ENVIRONMENT=development
```

## Useful Aliases

Add to your `~/.bashrc` or `~/.zshrc`:
```bash
alias bside-start='cd ~/bside && ./start-all.sh'
alias bside-stop='cd ~/bside && ./stop-all.sh'
alias bside-logs='cd ~/bside && ./watch-logs.sh'
alias bside-android='cd ~/bside && ./debug-platform.sh android'
alias bside-ios='cd ~/bside && ./debug-platform.sh ios'
alias bside-test='cd ~/bside && ./gradlew test'
```

## Performance

### Optimize Gradle
```bash
# In ~/.gradle/gradle.properties
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configureondemand=true
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m

# Use with:
./gradlew build --parallel --max-workers=8
```

### Build Faster
```bash
# Skip tests
./gradlew build -x test

# Build specific module only
./gradlew :shared:build

# Use build cache
./gradlew build --build-cache
```

## Documentation

- Full guide: [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md)
- Status: [STATUS_SUMMARY.md](STATUS_SUMMARY.md)
- SDK docs: [pocketbase-kt-sdk/README.md](pocketbase-kt-sdk/README.md)
- Design system: [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)
- iOS fixes: [iOS_BUILD_SUCCESS.md](iOS_BUILD_SUCCESS.md)

## Need Help?

1. Check logs: `./watch-logs.sh`
2. Verify services: `curl http://localhost:8080/health`
3. Clean build: `./gradlew clean build`
4. Read detailed guide: `DEVELOPMENT_GUIDE.md`
