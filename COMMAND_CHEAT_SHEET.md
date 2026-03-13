# ⚡ B-Side Command Cheat Sheet

Quick reference for common commands.

---

## 🚀 Starting & Stopping

```bash
# Start backend only (recommended)
just backend

# Start everything (automated)
just start

# Interactive startup with choices
just dev

# Stop everything
just stop
```

---

## 🖥️ Running Apps

### Desktop
```bash
just desktop              # Standard mode
just desktop-hot          # With hot reload
```

### Web
```bash
just web                  # Dev server at localhost:8080
```

### Android
```bash
just android              # Install to device/emulator
just android-studio       # Open in Android Studio
```

### iOS (macOS only)
```bash
just ios                  # Open in Xcode
```

---

## 🐳 Docker Commands

```bash
# View running containers
docker-compose ps

# View logs
docker logs -f bside-pocketbase
docker logs -f bside-server

# Restart a service
docker-compose restart pocketbase
docker-compose restart server

# Clean restart (⚠️ deletes data)
docker-compose down -v
docker-compose up --build
```

---

## 🗄️ Database

```bash
# Run migrations
just migrate

# Check migration status
just migrate-status

# Rollback last batch
just migrate-down

# Create new migration
just migrate-create "add_new_feature"
```

---

## 🧪 Testing

```bash
# Run all tests
./gradlew test

# Run specific module tests
./gradlew :shared:test
./gradlew :composeApp:test

# Run specific test
./gradlew :composeApp:jvmTest --tests "*ChatViewModelTest*"

# Skip tests
./gradlew build -x test
```

---

## 🔨 Building

```bash
# Build everything
./gradlew build

# Build server JAR
./gradlew :server:shadowJar

# Build Android APK
./gradlew :composeApp:assembleDebug

# Build Android AAB (for Play Store)
./gradlew :composeApp:bundleRelease

# Build Desktop JAR
./gradlew :composeApp:jvmJar

# Build Web bundle
./gradlew :composeApp:jsBrowserProductionWebpack
```

---

## 🧹 Cleaning

```bash
# Clean Gradle build
./gradlew clean

# Clean all (including node_modules)
./gradlew clean
rm -rf node_modules kotlin-js-store
npm install

# Stop Gradle daemons
./gradlew --stop

# Clean Docker (⚠️ deletes data)
docker-compose down -v
docker system prune -a
```

---

## 🔍 Debugging

### Check Service Health
```bash
curl http://localhost:8092/api/health  # PocketBase
curl http://localhost:8081/health      # Ktor Server
```

### Find Process Using Port
```bash
lsof -i :8092   # PocketBase
lsof -i :8081   # Server
lsof -i :8080   # Web dev server

# Kill process
kill -9 <PID>
```

### View Build Dependencies
```bash
./gradlew :composeApp:dependencies
```

### Check Android Devices
```bash
adb devices
adb logcat  # View Android logs
```

### Check iOS Simulators
```bash
xcrun simctl list devices
xcrun simctl boot <UDID>
```

---

## 📦 Dependencies

```bash
# Refresh Gradle dependencies
./gradlew --refresh-dependencies

# Update npm packages
npm update

# Check for outdated packages
npm outdated
./gradlew dependencyUpdates  # If plugin configured
```

---

## 🌐 URLs (When Running)

```bash
# Backend
http://localhost:8092       # PocketBase API
http://localhost:8092/_/    # PocketBase Admin UI
http://localhost:8081       # Ktor Server API

# Frontend
http://localhost:8080       # Web app (when running)
http://localhost:8082       # Nginx proxy (optional)
```

---

## 🔑 Default Credentials

```
Email:    tester_admin@bside.love
Password: password123
```

---

## 📊 Gradle Tasks Reference

```bash
# List all tasks
./gradlew tasks

# List tasks for specific module
./gradlew :composeApp:tasks

# Run with info logging
./gradlew build --info

# Run with debug logging
./gradlew build --debug

# Run with stacktrace
./gradlew build --stacktrace
```

---

## 🎯 Common Workflows

### Start Development Session
```bash
just backend          # Start backend once
just desktop-hot      # Or just web, or open IDE
```

### Make a Change and Test
```bash
# Edit code
# App auto-reloads (Desktop hot mode, Web)
# Or rebuild for Android/iOS

./gradlew test        # Run tests
```

### End Development Session
```bash
just stop             # Stop all services
```

### Clean Build from Scratch
```bash
just stop
./gradlew clean
rm -rf build */build
just backend
./gradlew build
```

---

## 🚨 Emergency Fixes

### Everything is Broken
```bash
just stop
./gradlew clean --stop
docker-compose down -v
rm -rf build */build node_modules kotlin-js-store
npm install
docker-compose up --build -d
./gradlew build
```

### Port Conflicts
```bash
# Find and kill processes
lsof -i :8092 | grep LISTEN | awk '{print $2}' | xargs kill -9
lsof -i :8081 | grep LISTEN | awk '{print $2}' | xargs kill -9
lsof -i :8080 | grep LISTEN | awk '{print $2}' | xargs kill -9
```

### Docker Won't Start
```bash
# Restart Docker Desktop (macOS)
killall Docker && open /Applications/Docker.app

# Or via CLI
docker-machine restart default
```

---

## 📚 More Help

- **Full Guide:** `docs/HOW_TO_RUN_LOCALLY.md`
- **Detailed Setup:** `docs/LOCAL_DEVELOPMENT_GUIDE.md`
- **All Commands:** `just --list`
- **Gradle Help:** `./gradlew help`

---

_Quick reference | For full documentation see docs/_
