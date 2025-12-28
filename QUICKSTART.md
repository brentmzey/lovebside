# B-Side Quick Start Guide

**A concise yet comprehensive guide to building, running, and developing the B-Side app.**

## 🎯 What is B-Side?

B-Side is a Kotlin Multiplatform dating app with:
- **Frontend**: Android, iOS, Desktop (JVM), Web (Kotlin/JS & Wasm)
- **Backend**: Ktor server (JVM) with PocketBase database
- **Architecture**: UI Clients → Backend API (auth) → PocketBase/ORM

---

## ⚡ Quick Commands

```bash
# Build everything (5-10 min first time)
./gradlew build

# Run individual platforms (fastest iteration)
./scripts/run-desktop.sh        # Desktop app (fastest)
./scripts/run-web.sh             # Browser app with hot-reload
./scripts/run-server.sh          # Backend API server
./scripts/run-android.sh         # Android (needs device/emulator)
./scripts/run-ios.sh             # iOS (macOS only, needs Xcode)

# Run everything at once
./scripts/start-all.sh           # Server + Desktop + Web (background)
./scripts/stop-all.sh            # Stop all background processes

# Testing & Verification
./scripts/test-full-stack.sh     # Full integration tests
./scripts/verify-targets.sh      # Quick compile check (1-2 min)
```

---

## 🏗️ Project Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Clients                               │
│  Android │ iOS │ Desktop │ Web (JS/Wasm)                    │
└─────────────────┬───────────────────────────────────────────┘
                  │ HTTP/WebSocket
                  ↓
┌─────────────────────────────────────────────────────────────┐
│              Backend API (Ktor Server)                      │
│  • Authentication (JWT)                                     │
│  • Business Logic                                           │
│  • Request Validation                                       │
└─────────────────┬───────────────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────────────┐
│              Data Layer                                     │
│  • PocketBase (Database + Real-time)                        │
│  • ORM (Kotlin SDK)                                         │
│  • Caching (Memory + Offline)                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Requirements

**Check all your platform requirements automatically:**
```bash
./scripts/check-platform-setup.sh
```

This script checks and guides you through setting up all 5 target platforms!

### Mandatory
- **Java 17+** (Temurin recommended) - `java -version`
- **Gradle 9.2+** - Included via wrapper (`./gradlew`)
- **Node.js 18+** - For web builds - `node --version`

### Platform-Specific
- **Android**: Android SDK, `adb` command available
- **iOS**: macOS + Xcode (for iOS builds only)
- **Desktop**: JVM 17+ (already covered above)

### Optional but Recommended
- **direnv** - Auto-loads scripts into PATH when entering project
- **ktlint** - Kotlin code formatter (for development)

---

## 🚀 First Time Setup

### 0. Check Platform Readiness
```bash
./scripts/check-platform-setup.sh

# This verifies all 5 platforms and shows what needs setup:
# ✅ Desktop, iOS, Web, Server (ready on your M4 Pro!)
# ⚠️  Android (just needs ADB in PATH)
```

### 1. Clone & Navigate
```bash
cd /Users/brentzey/bside
```

### 2. Verify Java
```bash
java -version
# Should show: Java 17+ (Temurin/Adoptium recommended)
```

### 3. Build Everything (First Time)
```bash
./gradlew build

# This will:
# - Download all dependencies
# - Compile all Kotlin targets (JVM, JS, Android, iOS)
# - Run tests
# - Take 5-10 minutes on first run
```

### 4. (Optional) Set up direnv
```bash
# Install direnv
brew install direnv

# Add to shell (choose one)
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc    # for zsh
echo 'eval "$(direnv hook bash)"' >> ~/.bashrc  # for bash

# Reload shell
source ~/.zshrc  # or source ~/.bashrc

# Allow project
cd /Users/brentzey/bside
direnv allow

# Now scripts work from anywhere in project:
run-desktop.sh   # instead of ./scripts/run-desktop.sh
```

---

## 🎮 Development Workflow

### Option 1: Desktop (Fastest Iteration)
Best for UI development and debugging shared code.

```bash
./scripts/run-desktop.sh

# Edit code → auto-recompile → see changes
# Uses JVM runtime (fast startup)
# Hot-reload enabled
```

### Option 2: Web with Hot-Reload
Best for testing browser compatibility.

```bash
./scripts/run-web.sh

# Access: http://localhost:8080
# Changes auto-reload in browser
# Uses Kotlin/JS or Wasm
```

### Option 3: Full Stack (Backend + Frontend)
Run backend API and a frontend together.

```bash
# Terminal 1: Backend
./scripts/run-server.sh

# Terminal 2: Frontend (choose one)
./scripts/run-desktop.sh
./scripts/run-web.sh
./scripts/run-android.sh  # needs device/emulator
```

### Option 4: Everything at Once
```bash
./scripts/start-all.sh

# Runs in background:
# - Backend server (port 8081)
# - Desktop app
# - Web app (port 8080)

# Stop everything:
./scripts/stop-all.sh
```

---

## 🔑 Key Directories

```
bside/
├── composeApp/          # UI layer (Compose Multiplatform)
│   ├── src/commonMain/  # Shared UI code
│   ├── src/androidMain/ # Android-specific
│   ├── src/iosMain/     # iOS-specific
│   ├── src/jvmMain/     # Desktop-specific
│   └── src/webMain/     # Web-specific
│
├── shared/              # Business logic (all platforms)
│   ├── src/commonMain/  # Shared Kotlin code
│   ├── src/androidMain/ # Android platform code
│   ├── src/iosMain/     # iOS platform code
│   ├── src/jvmMain/     # JVM/Desktop platform code
│   └── src/jvmTest/     # Integration tests
│
├── server/              # Backend API (Ktor)
│   ├── src/main/        # Server implementation
│   ├── routes/          # API endpoints
│   ├── repositories/    # Data access layer
│   └── services/        # Business logic
│
├── pocketbase/          # Database & backend runtime
│   ├── pb_data/         # Database files
│   ├── pb_migrations/   # Schema migrations
│   └── pb_hooks/        # Server-side hooks (JS/TS)
│
├── scripts/             # Development scripts (all executable)
│   ├── run-*.sh         # Platform runners
│   ├── build-all.sh     # Full build
│   ├── test-*.sh        # Testing utilities
│   └── README.md        # Detailed script docs
│
└── docs/                # Project documentation
    ├── BUILD_RUN_TEST.md      # Build & test guide
    ├── POCKETBASE_SCHEMA.md   # Database schema
    └── DESIGN_SYSTEM.md       # UI/UX guidelines
```

---

## 🧪 Testing

### Quick Verification (1-2 min)
```bash
./scripts/verify-targets.sh
# Verifies all platforms compile without building artifacts
```

### Unit Tests Only
```bash
./gradlew test -x :shared:iosSimulatorArm64Test -x :shared:iosX64Test
```

### Full Stack Integration Tests (5-10 min)
```bash
./scripts/test-full-stack.sh
# Tests entire stack: UI → Backend → Database
```

### Platform-Specific Tests
```bash
./gradlew :shared:jvmTest          # Shared module JVM tests
./gradlew :composeApp:jvmTest      # UI module JVM tests
./gradlew :server:test             # Backend tests
```

---

## 🔧 Common Tasks

### Clean Build
```bash
./gradlew clean build
```

### Build Without Tests (Faster)
```bash
./gradlew assemble
```

### Check Code Style
```bash
ktlint "**/*.kt"
```

### Remove Unused Imports
```bash
python3 scripts/remove_unused_imports.py --dry-run  # Preview
python3 scripts/remove_unused_imports.py            # Apply
```

### Run Backend with Live Logs
```bash
./scripts/run-server.sh  # Foreground mode with logs
```

### Run Backend in Background
```bash
./scripts/run-server.sh --background
# Logs go to: logs/server.log
```

---

## 🐛 Troubleshooting

### "Port 8080 already in use"
```bash
lsof -ti:8080 | xargs kill -9
# Or use stop-all.sh
```

### "Module not found" or dependency issues
```bash
./gradlew clean
./gradlew build --refresh-dependencies
```

### Android emulator not found
```bash
adb devices                    # List devices
adb start-server               # Start ADB
# Or open Android Studio → Device Manager
```

### iOS build fails
```bash
cd iosApp
pod install
xcodebuild -list              # Verify Xcode setup
```

### Gradle daemon issues
```bash
./gradlew --stop              # Stop all daemons
./gradlew build               # Fresh start
```

---

## 📱 Running on Devices

### Android
```bash
# Option 1: Via script
./scripts/run-android.sh

# Option 2: Via Gradle
./gradlew :composeApp:installDebug

# Option 3: Android Studio
# Open project → Select "composeApp" → Run
```

### iOS (macOS only)
```bash
# Option 1: Via script
./scripts/run-ios.sh

# Option 2: Xcode
cd iosApp
open iosApp.xcworkspace
# Select simulator/device → Run
```

### Desktop
```bash
./scripts/run-desktop.sh

# Or directly:
./gradlew :composeApp:run
```

### Web Browser
```bash
./scripts/run-web.sh

# Then open: http://localhost:8080
```

---

## 🔐 Backend & Database

### Start PocketBase Directly
```bash
cd pocketbase
./pocketbase serve

# Admin UI: http://127.0.0.1:8090/_/
# API: http://127.0.0.1:8090/api/
```

### Run Migrations
```bash
cd pocketbase
./pocketbase migrate up --dir migrations

# Or via migration manager:
cd migrations-manager
npm install
npm run migrate
```

### View Database Schema
See [docs/POCKETBASE_SCHEMA.md](./docs/POCKETBASE_SCHEMA.md)

---

## 📚 Key Documentation

- **[scripts/README.md](./scripts/README.md)** - Detailed script documentation
- **[docs/BUILD_RUN_TEST.md](./docs/BUILD_RUN_TEST.md)** - Comprehensive build guide
- **[docs/POCKETBASE_SCHEMA.md](./docs/POCKETBASE_SCHEMA.md)** - Database schema
- **[docs/DESIGN_SYSTEM.md](./docs/DESIGN_SYSTEM.md)** - UI/UX guidelines
- **[docs/SETUP_CHECKLIST.md](./docs/SETUP_CHECKLIST.md)** - Environment setup

---

## 🎯 For Your M4 Pro Mac

Your machine is perfect for this project! To get the full experience:

### 1. Run Backend + Emulator + Desktop
```bash
# Terminal 1: Backend
./scripts/run-server.sh

# Terminal 2: Android Emulator (if using Android Studio)
# Open Android Studio → Device Manager → Start emulator

# Terminal 3: Android App
./scripts/run-android.sh

# Terminal 4: Desktop App (for comparison)
./scripts/run-desktop.sh
```

### 2. Monitor Everything
```bash
# Watch backend logs
tail -f logs/server.log

# Watch PocketBase logs
cd pocketbase && ./pocketbase serve
```

### 3. Performance Tips
Your M4 Pro can handle parallel builds:
```bash
./gradlew build --parallel --max-workers=8
```

---

## 💡 Pro Tips

1. **Fastest Development**: Use `run-desktop.sh` - JVM hot-reload is instant
2. **Backend Development**: Use `run-server.sh` - live log streaming
3. **UI Debugging**: Web has best DevTools - use `run-web.sh`
4. **Pre-Commit**: Run `./scripts/verify-targets.sh` (1-2 min) instead of full build
5. **direnv**: Set it up once, never type `./scripts/` again

---

## 🆘 Need Help?

1. Check **[scripts/README.md](./scripts/README.md)** for detailed script usage
2. Review **[docs/BUILD_FIXES.md](./docs/BUILD_FIXES.md)** for common issues
3. See **[docs/STATUS.md](./docs/STATUS.md)** for current project status

---

## ✅ What Just Got Cleaned Up

**This session**:
- ✅ Moved 13 markdown files from root to `docs/`
- ✅ Removed 91 unused imports across 44 files
- ✅ Verified project builds successfully
- ✅ Updated README with better organization
- ✅ Created this QUICKSTART guide

**Result**: Cleaner codebase, organized docs, verified working build!
