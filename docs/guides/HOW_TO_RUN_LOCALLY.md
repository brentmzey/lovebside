# 🎯 How to Run B-Side Locally - Quick Start

**Last Updated:** 2025-01-24

This is your **go-to guide** for running B-Side on your local machine. Follow these steps in order.

---

## ⚡ TL;DR - Just Get It Running

```bash
# 1. Start the backend
just backend

# 2. In Android Studio: 
#    - Open project
#    - Click ▶️ Run (for Android)
#    - Or Product → Run (for Desktop)

# 3. For iOS:
just ios
# Then click ▶️ in Xcode
```

That's it! Backend runs in Docker, mobile apps run from IDEs.

---

## 📋 Prerequisites Check

Before starting, verify you have:

```bash
# Check Docker
docker --version          # Should see version 20+
docker-compose --version  # Should see version 1.29+

# Check Java
java -version            # Should be 17 or higher

# macOS only - Check Xcode
xcodebuild -version      # Should be 15+
```

**Missing something?**
- Docker Desktop: https://www.docker.com/products/docker-desktop
- JDK 17: https://adoptium.net/
- Android Studio: https://developer.android.com/studio
- Xcode (macOS): Install from App Store

---

## 🚀 Step-by-Step Startup

###  1. Start Backend Services

The backend includes:
- **PocketBase** (database)
- **Ktor Server** (API)

#### Option A: Interactive Script (Recommended for First Time)
```bash
./scripts/dev-start.sh
```

This script will:
- ✅ Check prerequisites
- ✅ Build the server JAR
- ✅ Start Docker containers
- ✅ Wait for services to be healthy
- ✅ Optionally launch Desktop/Web apps
- ✅ Show you all the URLs and credentials

#### Option B: Just Backend (Quickest)
```bash
./scripts/backend-start.sh
```

Or using Just:
```bash
just backend
```

This starts **only** PocketBase + Ktor Server in Docker.

#### Option C: Everything Automated
```bash
just start
```

Attempts to start all targets automatically (Desktop, Web, Android, iOS).

**Expected Output:**
```
✅ Backend services are running!

📍 Endpoints:
  PocketBase:      http://localhost:8092
  PocketBase Admin: http://localhost:8092/_/
  Ktor API:        http://localhost:8081
  Health Check:    http://localhost:8081/health

🔑 Admin Credentials:
  Email:    tester_admin@bside.love
  Password: password123
```

---

### 2. Launch Android App

#### From Android Studio (Recommended)
1. Open project in Android Studio
   ```bash
   just android-studio
   # Or:
   open -a "Android Studio" .
   ```

2. Wait for Gradle sync to complete

3. Start an emulator:
   - Tools → Device Manager
   - Click ▶️ on any device (e.g., Pixel 6)
   - Or create new device if needed

4. Run the app:
   - Click green ▶️ Run button in toolbar
   - Or: Run → Run 'app'

#### From Command Line
```bash
# Make sure emulator or device is running first
adb devices

# Install and run
just android
# Or:
./gradlew :composeApp:installDebug
```

---

### 3. Launch iOS App (macOS Only)

#### From Xcode (Recommended)
```bash
just ios
# Or:
open iosApp/iosApp.xcodeproj
```

Then in Xcode:
1. Select a simulator (e.g., iPhone 15)
2. Click ▶️ Run button
3. Wait for build + app launches automatically

#### Troubleshooting iOS
If build fails with "Framework not found":
```bash
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

Then try Xcode again.

---

### 4. Launch Desktop App

#### From Terminal
```bash
just desktop
# Or:
./gradlew :composeApp:jvmRun
```

A native window should open with the app.

#### From Android Studio/IntelliJ
1. Open `composeApp` module
2. Find `Main.kt`
3. Click green ▶️ next to `fun main()`

#### With Hot Reload
```bash
just desktop-hot
```

Changes in code will reload automatically.

---

### 5. Launch Web App

```bash
just web
# Or:
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**Wait 30-60 seconds** for Webpack to build, then open:
- http://localhost:8080

Changes reload automatically.

---

## 🛑 Stopping Everything

```bash
just stop
```

This will:
- Stop all Docker containers
- Kill all Gradle daemons
- Clean up background processes

---

## 🔍 Troubleshooting

### Backend Won't Start

```bash
# Check Docker is running
docker ps

# View logs
docker logs bside-pocketbase
docker logs bside-server

# Clean restart
docker-compose down -v
docker-compose up --build
```

### Port Already in Use

```bash
# Find what's using port 8092 (PocketBase)
lsof -i :8092

# Kill it
kill -9 <PID>

# Or change ports in docker-compose.yml
```

### Android Build Fails

```bash
# Make sure ANDROID_HOME is set
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk          # Linux

# Or create local.properties
echo "sdk.dir=$HOME/Library/Android/sdk" > local.properties

# Clean and rebuild
./gradlew clean
./gradlew :composeApp:assembleDebug
```

### iOS Build Fails

```bash
# Rebuild KMP framework
./gradlew :composeApp:clean
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode

# If Xcode complains about JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

# Clean Xcode derived data
rm -rf ~/Library/Developer/Xcode/DerivedData
```

### Desktop App Won't Launch

```bash
# Check Java version
java -version  # Must be 17+

# Clean build
./gradlew :composeApp:clean
./gradlew :composeApp:jvmRun
```

---

## 📊 Checking If Everything Works

### Health Checks

```bash
# PocketBase
curl http://localhost:8092/api/health
# Should return: {"code":200,"message":"OK","data":{}}

# Ktor Server
curl http://localhost:8081/health
# Should return: {"status":"OK"}
```

### Access PocketBase Admin UI

1. Open http://localhost:8092/_/
2. Login with:
   - Email: `tester_admin@bside.love`
   - Password: `password123`
3. Browse collections (s_profiles, m_messages, m_conversations, etc.)

### Test API Endpoint

```bash
# Get all profiles (requires auth token)
curl http://localhost:8081/api/profiles
```

---

## 🎯 Recommended Workflow

For most development tasks:

1. **Start backend once** (keep running)
   ```bash
   just backend
   ```

2. **Use Android Studio** for Android/Desktop development
   - Click Run whenever you make changes
   - Hot reload works in most cases

3. **Use Xcode** for iOS development
   - Click Run whenever you make changes

4. **Use terminal** for Web development
   ```bash
   just web
   ```
   - Hot reload automatic
   - View at http://localhost:8080

5. **At end of day**
   ```bash
   just stop
   ```

---

## 📁 Important Directories

```
bside/
├── composeApp/          # Shared UI code + platform targets
├── shared/              # Shared business logic, models, repos
├── server/              # Ktor backend server
├── pocketbase/          # PocketBase config, migrations, hooks
├── scripts/             # Helper scripts
└── docs/                # Documentation
```

---

## 🔗 Quick Links

- **Full Dev Guide:** [docs/LOCAL_DEVELOPMENT_GUIDE.md](./LOCAL_DEVELOPMENT_GUIDE.md)
- **Architecture:** [docs/ARCHITECTURE.md](./ARCHITECTURE.md) (if exists)
- **API Docs:** [docs/API.md](./API.md) (if exists)
- **Testing:** Run `./gradlew test`

---

## 🆘 Getting Help

**Logs to Check:**
```bash
docker logs bside-pocketbase   # Backend database
docker logs bside-server       # Backend API
tail -f desktop.log            # Desktop app (if started via script)
tail -f web.log                # Web app (if started via script)
```

**Clean Slate Reset:**
```bash
just stop
docker-compose down -v  # ⚠️ Deletes local database!
rm -rf build */build
just backend
```

---

## ✅ Success Checklist

After following this guide, you should have:

- [ ] Backend running (PocketBase + Ktor)
- [ ] Can access PocketBase Admin UI
- [ ] Android app runs on emulator
- [ ] iOS app runs on simulator (macOS)
- [ ] Desktop app opens as native window
- [ ] Web app accessible at localhost:8080

**All working? Awesome! You're ready to develop. 🎉**

**Issues? Check [Troubleshooting](#troubleshooting) or ask for help.**

---

_Last updated: 2025-01-24 | For B-Side v1.0_
