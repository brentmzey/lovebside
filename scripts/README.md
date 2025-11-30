# B-Side Development Scripts

This directory contains convenience scripts for building and running different targets of the B-Side Kotlin Multiplatform application. These scripts are essential for efficient development when you want to work on specific platforms without building everything.

## 🚀 Quick Setup

### Option 1: Using direnv (Recommended)
If you have [direnv](https://direnv.net/) installed:
```bash
# From project root
direnv allow
```
This automatically adds `./scripts` to your PATH whenever you're in the project directory.

### Option 2: Manual PATH Setup
Add to your `~/.bashrc` or `~/.zshrc`:
```bash
export PATH="$PATH:/Users/brentzey/bside/scripts"
```
Or run scripts with `./scripts/script-name.sh` from project root.

## 📱 Platform-Specific Scripts

### `run-android.sh`
Builds and deploys the Android app to a connected device or emulator.

**Usage:**
```bash
./run-android.sh
```

**Requirements:**
- Android SDK with `adb` in PATH
- Physical device with USB debugging OR running emulator

**Use Case:** Mobile development - test Android-specific UI, native features, or Material3 components.

---

### `run-desktop.sh`
Runs the JVM desktop application using Compose Desktop.

**Usage:**
```bash
# Foreground (interactive)
./run-desktop.sh

# Background mode
./run-desktop.sh --background
```

**Use Case:** 
- Fastest iteration cycle for UI development
- Testing desktop-specific layouts and keyboard navigation
- Quick testing without emulator overhead
- Development on macOS/Linux/Windows desktop

**Output:** `desktop.log` (background mode)

---

### `run-ios.sh`
Builds and launches the iOS app in Xcode Simulator or on device.

**Usage:**
```bash
./run-ios.sh
```

**Requirements:**
- macOS with Xcode installed
- iOS Simulator or connected iOS device

**Use Case:** iOS-specific testing - SwiftUI interop, iOS design patterns, App Store preview.

---

### `run-web.sh`
Starts the Kotlin/JS web application with hot reload.

**Usage:**
```bash
# Foreground with console output
./run-web.sh

# Background mode
./run-web.sh --background
```

**Access:** http://localhost:8080

**Use Case:**
- Web-specific development and responsive design
- Browser DevTools debugging
- Progressive Web App (PWA) features
- Fastest hot-reload experience

**Output:** `web.log` (background mode)

---

### `run-wasm.sh`
Launches the experimental Compose Multiplatform WebAssembly target described in the [Kotlin Multiplatform quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html#run-the-sample-apps).

**Usage:**
```bash
./run-wasm.sh
# or background mode
./run-wasm.sh --background
```

**Notes:**
- Uses `wasmJsBrowserDevelopmentRun --continuous` for hot reload
- Serves on http://localhost:8080 (same as JS dev server)
- Logs stream to `wasm.log` when running in background

**Use Case:**
- Validate Compose/Wasm builds on supported browsers
- Compare JS vs Wasm build performance with identical UI code

---

### `run-server.sh`
Starts the Ktor backend server.

**Usage:**
```bash
# Foreground
./run-server.sh

# Background mode
./run-server.sh --background
```

**Features:**
- Auto-builds server JAR if missing
- Stops existing server before starting new one
- Runs on http://localhost:8080

**Use Case:**
- Backend API development
- Testing client-server integration
- Database operations with PocketBase

**Output:** `server.log` (background mode)

---

## 🎯 Utility Scripts

### `start-all.sh`
Launches the complete development environment: server + desktop + web apps.

**Usage:**
```bash
./start-all.sh
```

**What it does:**
1. Starts server in background
2. Waits for server health check
3. Starts desktop app in background
4. Starts web app in background
5. Tails server log (Ctrl+C stops all)

**Use Case:** Full-stack development when you need everything running simultaneously.

---

### `stop-all.sh`
Gracefully stops all background processes started by `start-all.sh`.

**Usage:**
```bash
./stop-all.sh
```

**Use Case:** Clean shutdown of all development services.

---

### `verify-targets.sh`
Tests compilation of all Kotlin Multiplatform targets without running them.

**Usage:**
```bash
./verify-targets.sh
```

**What it checks:**
- ✅ Android Debug APK build
- ✅ Desktop JVM JAR compilation
- ✅ JavaScript (Web) compilation
- ✅ iOS Simulator Arm64 framework
- ✅ iOS Device Arm64 framework

**Use Case:**
- After updating dependencies
- Before committing major changes
- Ensuring all targets remain compatible
- CI/CD smoke test locally

---

### `build-all.sh`
Full build of all Kotlin Multiplatform targets using system Gradle.

**Usage:**
```bash
./build-all.sh
```

**What it does:**
- Runs `gradle build`
- Builds Android APKs, iOS frameworks, Desktop JARs, Web JS, Server JAR
- Executes unit tests alongside the build
- Shows location of all built artifacts

**Use Case:**
- Preparing release builds
- Validating full project compilation + test pass
- Building all deployment artifacts at once

---

### `test-full-stack.sh`
Runs comprehensive integration tests across the entire stack.

**Usage:**
```bash
./test-full-stack.sh
```

**Use Case:** Ensure client-server contracts are maintained, validate end-to-end workflows.

---

### `test-server-db.sh`
Tests server and database integration specifically.

**Usage:**
```bash
./test-server-db.sh
```

**Use Case:** Backend-focused development, testing PocketBase schemas and server logic.

---

## 💡 Common Development Workflows

### Working on UI Components (Desktop)
**Fastest feedback loop:**
```bash
./run-desktop.sh
# Edit Compose UI code -> auto-recompile -> see changes
```

### Testing Responsive Web Design
```bash
./run-web.sh --background
# Access http://localhost:8080 in browser
# Use browser DevTools for responsive testing
```

### Mobile Feature Development
```bash
# Android
./run-android.sh

# iOS (macOS only)
./run-ios.sh
```

### Full-Stack API Development
```bash
# Terminal 1: Server with live logs
./run-server.sh

# Terminal 2: Client app of choice
./run-desktop.sh
# or
./run-web.sh
```

### Pre-Commit Validation
```bash
# Quick: Verify all targets compile
./verify-targets.sh

# Thorough: Run full test suite
./test-full-stack.sh
```

### Working on Single Platform (Save Compilation Time)
Instead of building all targets (which can take 5-10+ minutes), build only what you need:

```bash
# Only need web? Skip Android/iOS/Desktop builds
./run-web.sh

# Only testing desktop? Skip web/mobile
./run-desktop.sh

# Backend changes only? Skip all frontends
./run-server.sh
```

**Time saved:** 70-90% reduction in build time vs `./gradlew build`

---

## 🔧 Background Process Management

Scripts use `.pids/` directory to track background processes:
- `desktop.pid` - Desktop app process ID
- `web.pid` - Web dev server process ID  
- `server.pid` - Backend server process ID

**Manual cleanup if needed:**
```bash
# View running processes
cat .pids/*.pid

# Kill specific process
kill $(cat .pids/web.pid)

# Or use the utility
./stop-all.sh
```

---

## 📊 Cost Optimization Tips

**Why these scripts save money:**
1. **Targeted builds** - Only compile what you're actively developing
2. **Local testing** - Avoid triggering expensive CI/CD pipelines
3. **Fast iteration** - Catch issues before pushing to GitHub
4. **Resource efficient** - Run only necessary services

**Best practices:**
- Use `verify-targets.sh` before pushing (instead of GitHub Actions)
- Develop with `run-desktop.sh` (fastest) then validate on other platforms
- Run `test-full-stack.sh` locally before creating PRs
- Use background mode for multi-service development without multiple terminals

---

## 🛠️ Customization

All scripts are executable and can be modified for your workflow:
```bash
# All scripts already have execute permissions
ls -la scripts/

# Edit any script
vim scripts/run-desktop.sh
```

---

## 📝 Logs

Background processes write to project root:
- `desktop.log` - Desktop app output
- `web.log` - Web dev server output
- `server.log` - Backend server output

**View logs:**
```bash
tail -f desktop.log
tail -f web.log  
tail -f server.log
```

---

## ⚡ Performance Tips

1. **Desktop is fastest** - Use for rapid UI iteration
2. **Web has hot-reload** - Great for CSS/layout work  
3. **Start server once** - Keep running in background for all clients
4. **Incremental builds** - Gradle caches intermediate results
5. **Verify before committing** - `verify-targets.sh` catches breaks early

## 🏗️ Building All Targets with System Gradle

You can use your system-installed `gradle` command to build the entire project:

```bash
# Build everything (compile only)
gradle assemble

# Build + run unit tests (recommended)
gradle build

# Clean and rebuild with tests
gradle clean build
```

**Note on JS/Web builds:** `gradle build` now runs the production `jsBrowserProductionWebpack` task successfully. If a workstation is missing the local Node toolchain, run `./gradlew kotlinNodeJsSetup` once to hydrate the cache.

**All Kotlin compiler backends work:**
- ✅ Android (ARM, x86)
- ✅ iOS (ARM64 device + simulator)
- ✅ JVM Desktop (macOS, Windows, Linux)
- ✅ JavaScript/Web (development & browser)
- ✅ Backend server (JVM)

---

## 🆘 Troubleshooting

**Scripts not in PATH?**
```bash
# Re-allow direnv
direnv allow

# Or run with full path
./scripts/run-desktop.sh
```

**Port 8080 already in use?**
```bash
# Find and kill process
lsof -ti:8080 | xargs kill -9

# Or use stop-all
./stop-all.sh
```

**Build failures?**
```bash
# Clean build
./gradlew clean

# Rebuild specific target
./gradlew :composeApp:jvmJar --rerun-tasks
```

**Permission denied?**
```bash
# Re-add execute permissions
chmod +x scripts/*.sh
```

---

## 📚 Related Documentation

- [Build Status](../docs/BUILD_STATUS.md) - CI/CD and build configuration
- [Design System](../docs/DESIGN_SYSTEM.md) - UI/UX guidelines
- [Shared Types Guide](../docs/SHARED_TYPES_GUIDE.md) - Cross-platform type safety
- [PocketBase Schema](../docs/POCKETBASE_SCHEMA.md) - Database schema and API

---

**Last Updated:** November 16, 2024
