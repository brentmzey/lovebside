# ⚡ Quick Start: Backend & All UIs

**TL;DR - Get everything running in 2 minutes**

---

## 🎯 The Fastest Way (Just One Command!)

```bash
# Install 'just' if you don't have it
brew install just

# Start EVERYTHING (backend + all frontends)
just start
```

This starts:
- ✅ PocketBase (Database/Backend)
- ✅ Ktor Server (API)
- ✅ Web UI (http://localhost:8080)
- ✅ Desktop UI (native window)

**To stop everything:**
```bash
just stop
```

---

## 🐳 Backend Only (Docker - Recommended)

### Step 1: Build Server JAR

```bash
./gradlew :server:shadowJar
```

### Step 2: Start All Backend Services

```bash
docker-compose up --build
```

**Or run in background:**
```bash
docker-compose up -d
```

### What's Running?

| Service | URL | Purpose |
|---------|-----|---------|
| **PocketBase Admin** | http://localhost:8092/_/ | Database admin UI |
| **PocketBase API** | http://localhost:8092/api/ | Database API |
| **Ktor Server** | http://localhost:8081/ | Backend API |
| **Nginx** | http://localhost:8082/ | Reverse proxy |

**Default Login:**
- Email: `tester_admin@bside.love`
- Password: `password123`

### Stop Backend

```bash
docker-compose down
```

---

## 💻 Local Backend (Without Docker)

### Terminal 1: PocketBase

```bash
./scripts/setup_dev_env.sh
# Or: just pb-local
```

Runs on http://localhost:8090

### Terminal 2: Ktor Server

```bash
./gradlew :server:run
# Or: just server-local
```

Runs on http://localhost:8080

---

## 📱 Run Frontends (After Backend is Running)

### 1. Desktop App

```bash
# Standard mode
just desktop

# Hot reload mode (auto-restart on code changes)
just desktop-hot
```

### 2. Web Browser

```bash
just web
```

Opens at http://localhost:8080 with hot reload

### 3. Android Studio

```bash
# Open project in Android Studio
just android-studio

# Or manually
open -a "Android Studio" .
```

Then:
1. Wait for Gradle sync
2. Select target device/emulator from dropdown
3. Click Run ▶️ button

**Or from command line:**
```bash
# Install to connected device/emulator
just android

# Or
./gradlew :composeApp:installDebug
```

### 4. iOS (macOS only)

```bash
# Open in Xcode
just ios

# Or manually
open iosApp/iosApp.xcodeproj
```

Then click Run ▶️ in Xcode

---

## 🎮 Android Studio Emulator Setup

### Create an Emulator (First Time Only)

1. **Open Android Studio** → **Device Manager** (phone icon on right toolbar)

2. **Create Device** → Choose:
   - **Phone:** Pixel 6 Pro
   - **System Image:** Android 14 (API 34) with Google APIs
   - Click **Download** if needed
   - **Finish**

3. **Start Emulator** → Click ▶️ play button

### Run App on Emulator

**Method 1: From Android Studio**
- Select emulator from device dropdown (top toolbar)
- Click Run ▶️ (green play button)

**Method 2: From Terminal**
```bash
# Make sure emulator is running first!
./gradlew :composeApp:installDebug
```

### iOS Simulator Setup (macOS)

**Open Xcode:**
```bash
open iosApp/iosApp.xcodeproj
```

**Select Simulator:**
- Top bar → Select "iPhone 15 Pro" (or any iOS 17+ device)
- Click Run ▶️

**Or from Terminal:**
```bash
# List available simulators
xcrun simctl list devices

# Boot a simulator
xcrun simctl boot "iPhone 15 Pro"

# Then build
cd iosApp
xcodebuild -workspace iosApp.xcworkspace -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

---

## 🔧 Troubleshooting

### "Port already in use"

```bash
# Kill all B-Side processes
./scripts/stop-all.sh

# Or manually kill ports
lsof -ti:8090 | xargs kill -9  # PocketBase
lsof -ti:8080 | xargs kill -9  # Server/Web
```

### "Gradle build failed"

```bash
# Clean and rebuild
./gradlew clean
./gradlew :server:shadowJar
```

### "Docker won't start"

```bash
# Clean Docker
docker-compose down -v
docker system prune -a

# Restart Docker Desktop app
# Then try again
docker-compose up --build
```

### "Android Studio won't sync"

```bash
# Invalidate caches
# File → Invalidate Caches → Invalidate and Restart

# Or clean manually
rm -rf .idea/ .gradle/ build/
./gradlew clean
```

### "Can't connect to backend from app"

**If using emulator:** Backend URLs are different!

**Android Emulator:**
```kotlin
// Use 10.0.2.2 instead of localhost
val BASE_URL = "http://10.0.2.2:8090"
```

**iOS Simulator:**
```kotlin
// localhost works
val BASE_URL = "http://localhost:8090"
```

**Physical Device:**
```kotlin
// Use your computer's IP
val BASE_URL = "http://192.168.1.XXX:8090"
```

---

## 📊 Check Everything is Working

```bash
# Health checks
curl http://localhost:8092/api/health  # PocketBase
curl http://localhost:8081/health       # Ktor Server

# Or use script
./scripts/verify-targets.sh
```

---

## 🎯 Typical Workflow

```bash
# Morning: Start everything
just start

# Develop (changes auto-reload)...

# Run tests before committing
./gradlew check

# Evening: Stop everything
just stop
```

---

## 📚 More Details

- **Full Guide:** [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md)
- **Database Setup:** [DATABASE.md](./DATABASE.md)
- **Testing:** [TESTING.md](./TESTING.md)
- **Deployment:** [DEPLOYMENT.md](./DEPLOYMENT.md)

---

## 🆘 Still Stuck?

1. Check logs: `docker-compose logs -f`
2. Verify Docker is running: `docker ps`
3. Check Java version: `java -version` (needs 17+)
4. Try clean build: `./gradlew clean build`
5. Read detailed guide: [LOCAL_DEVELOPMENT.md](./LOCAL_DEVELOPMENT.md)

---

**That's it! You're ready to develop! 🚀**
