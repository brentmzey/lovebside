# 🚀 Local Development Guide - B-Side

Complete guide for setting up and running B-Side locally on your machine.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Detailed Setup](#detailed-setup)
- [Running Different Targets](#running-different-targets)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Tools
1. **JDK 17+** (for Kotlin/JVM)
   ```bash
   java -version  # Should be 17 or higher
   ```

2. **Node.js 18+** (for Web target and scripts)
   ```bash
   node --version  # Should be 18 or higher
   ```

3. **Docker Desktop** (for PocketBase backend)
   ```bash
   docker --version
   docker-compose --version
   ```

4. **Gradle** (bundled with project via `./gradlew`)

### Platform-Specific Tools

#### macOS
- **Xcode 15+** (for iOS)
- **CocoaPods** (for iOS dependencies)
  ```bash
  sudo gem install cocoapods
  ```
- **Android Studio** (for Android)

#### Linux/Windows
- **Android Studio** (for Android)

---

## Quick Start

### 1. One-Command Launch (All Targets)
```bash
just start
```

This will:
- ✅ Start PocketBase backend (Docker)
- ✅ Start Ktor Server (Docker)
- ✅ Launch Desktop app
- ✅ Launch Web app
- ✅ Build/install Android app (if device/emulator detected)
- ✅ Build/install iOS app (macOS only, if simulator detected)

### 2. Stop Everything
```bash
just stop
```

---

## Detailed Setup

### Step 1: Clone and Install Dependencies

```bash
# Clone repository
git clone https://github.com/brentzey/bside.git
cd bside

# Copy environment variables (optional, has defaults)
cp .env.example .env

# Install Node dependencies
npm install
```

### Step 2: Build the Server JAR

```bash
./gradlew :server:shadowJar
```

This creates a fat JAR for the Ktor backend server.

### Step 3: Start Backend Services

#### Option A: Docker Compose (Recommended)
```bash
# Start PocketBase + Ktor Server
just up

# Or manually:
docker-compose up --build

# Run in background:
docker-compose up -d
```

**Services:**
- **PocketBase**: `http://localhost:8092` (mapped from container port 8090)
- **Ktor Server**: `http://localhost:8081` (mapped from container port 8080)
- **PocketBase Admin**: `http://localhost:8092/_/`
  - Email: `tester_admin@bside.love`
  - Password: `password123`

#### Option B: Local Binaries (Development)
```bash
# Start PocketBase locally (no Docker)
just pb-local

# In another terminal, start Ktor Server
just server-local
```

---

## Running Different Targets

### Desktop (JVM)

#### Standard Mode
```bash
just desktop
# Or:
./gradlew :composeApp:jvmRun
```

#### Hot Reload Mode
```bash
just desktop-hot
# Or:
./gradlew :composeApp:hotRunJvm
```

**App runs at:** Native window opens automatically

---

### Web (JS/Wasm)

#### Development Server (Hot Reload)
```bash
just web
# Or:
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**App runs at:** `http://localhost:8080`

#### Production Build
```bash
./gradlew :composeApp:jsBrowserProductionWebpack
# Output: composeApp/build/dist/js/productionExecutable/
```

---

### Android

#### Prerequisites
1. **Set ANDROID_HOME**
   ```bash
   export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
   export ANDROID_HOME=$HOME/Android/Sdk          # Linux
   ```

2. **Start Emulator** (via Android Studio)
   - Open Android Studio
   - Tools → Device Manager
   - Create/Start an emulator (e.g., Pixel 6 API 34)

3. **Or Connect Physical Device**
   ```bash
   adb devices  # Should show your device
   ```

#### Install Debug Build
```bash
just android
# Or:
./gradlew :composeApp:installDebug
```

#### Open in Android Studio
```bash
just android-studio
# Or manually:
open -a "Android Studio" .
```

---

### iOS (macOS Only)

#### Prerequisites
1. **Xcode 15+** installed
2. **Command Line Tools**
   ```bash
   xcode-select --install
   ```

#### Option A: Via Xcode (Recommended)
```bash
just ios
# Or:
open iosApp/iosApp.xcodeproj
```

Then in Xcode:
1. Select a simulator (e.g., iPhone 15)
2. Press ▶️ Run

#### Option B: Command Line
```bash
# List available simulators
xcrun simctl list devices

# Boot a simulator
xcrun simctl boot <UDID>

# Build and install
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
xcodebuild -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=<UDID>" \
  install

# Launch app
xcrun simctl launch <UDID> love.bside.app
```

---

## Database Migrations

### Run Migrations
```bash
# Check status
just migrate-status

# Apply pending migrations
just migrate

# Rollback last batch
just migrate-down
```

### Create New Migration
```bash
just migrate-create "add_reactions_to_messages"
```

### Test Migrations Locally
```bash
# ⚠️ DESTROYS LOCAL DATA
just test-migrations
```

---

## Troubleshooting

### Backend Issues

#### PocketBase Won't Start
```bash
# Check Docker logs
docker logs bside-pocketbase

# Check port conflicts
lsof -i :8092

# Clean restart
docker-compose down -v
docker-compose up --build
```

#### Server Won't Connect to PocketBase
```bash
# Check network
docker network inspect bside_bside-network

# Verify PocketBase is running
curl http://localhost:8092/api/health

# Check server logs
docker logs bside-server
```

### Desktop App Issues

#### App Won't Launch
```bash
# Clean build
./gradlew :composeApp:clean
./gradlew :composeApp:jvmRun

# Check Java version
java -version  # Must be 17+
```

### Web App Issues

#### Port 8080 Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill it
kill -9 <PID>

# Or change port in gradle.properties
```

#### Webpack Build Fails
```bash
# Clean Node modules and rebuild
rm -rf node_modules kotlin-js-store
npm install
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### Android Issues

#### Gradle Sync Fails
```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk

# Refresh dependencies
./gradlew --refresh-dependencies
```

#### ADB Not Found
```bash
# Add to PATH
export PATH=$PATH:$ANDROID_HOME/platform-tools

# Or use full path
$ANDROID_HOME/platform-tools/adb devices
```

#### Build Fails with "SDK Location Not Found"
Create `local.properties`:
```properties
sdk.dir=/Users/YOUR_USERNAME/Library/Android/sdk
```

### iOS Issues

#### Build Fails: "Framework Not Found"
```bash
# Ensure pods are installed
cd iosApp
pod install
cd ..

# Rebuild framework
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

#### Simulator Won't Boot
```bash
# List devices
xcrun simctl list devices

# Delete and recreate
xcrun simctl delete <UDID>
# Then create new via Xcode → Window → Devices and Simulators
```

#### Xcode Can't Find JAVA_HOME
Add to `~/.zshrc` or `~/.bash_profile`:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

---

## Environment Variables

### .env File
```bash
# Backend
POCKETBASE_ADMIN_EMAIL=tester_admin@bside.love
POCKETBASE_ADMIN_PASSWORD=password123

# Public URLs (for file access)
PB_PUBLIC_URL=http://localhost:8092

# CDN (optional)
CDN_ENABLED=false
CDN_BASE_URL=

# AWS S3 (optional)
AWS_REGION=
AWS_S3_BUCKET=
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
```

---

## Development Workflow

### Typical Day-to-Day

1. **Start Backend Once**
   ```bash
   just up  # Runs in background
   ```

2. **Work on Desktop** (hot reload)
   ```bash
   just desktop-hot
   ```

3. **Test on Web**
   ```bash
   just web
   ```

4. **Test on Mobile** (when needed)
   ```bash
   # Android
   just android

   # iOS
   just ios
   ```

5. **End of Day Cleanup**
   ```bash
   just stop
   ```

### Testing Full Stack
```bash
# Run all unit tests
./gradlew test

# Run integration tests (requires backend running)
./gradlew :shared:jvmTest

# Run specific test
./gradlew :composeApp:jvmTest --tests "*ChatViewModelTest*"
```

---

## Quick Reference

| Command | Description |
|---------|-------------|
| `just start` | Launch everything |
| `just stop` | Stop everything |
| `just up` | Start backend (Docker) |
| `just down` | Stop backend |
| `just desktop` | Run desktop app |
| `just web` | Run web app (dev server) |
| `just android` | Install Android debug |
| `just ios` | Open iOS in Xcode |
| `just migrate` | Run DB migrations |
| `just android-studio` | Open in Android Studio |

---

## Next Steps

- [Architecture Overview](./ARCHITECTURE.md)
- [API Documentation](./API.md)
- [Testing Guide](./TESTING.md)
- [Deployment Guide](./DEPLOYMENT.md)

---

## Need Help?

- 📖 Check [docs/](./README.md)
- 🐛 File an issue: [GitHub Issues](https://github.com/brentzey/bside/issues)
- 💬 Slack: `#bside-dev`
