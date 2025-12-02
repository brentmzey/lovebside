# Build & Test Guide

Complete guide to building and testing all Kotlin Multiplatform targets.

## 🔧 Prerequisites
- Temurin **JDK 17 LTS** (Adoptium). JDK 21 also works, but 17 is the default used by `scripts/start-all.sh` and CI.
- Android SDK + emulator images (via Android Studio)
- Xcode 15.4+ (for iOS targets)
- Node.js 20+ (for Kotlin/JS tooling)

## 🎯 Quick Commands

### ✅ Build All Targets (Recommended)
```bash
# Full build (all targets + unit tests)
./gradlew clean build

# Faster incremental rebuild
./gradlew build

# Compile-only (skip tests)
./gradlew assemble

# Outputs: Android APK, iOS Frameworks, Desktop JAR, Web JS, Server JAR
```

### 🧪 Run Tests

#### Unit Tests Only (No Backend Required)
```bash
# Run all unit tests (excludes integration tests)
./gradlew test --tests "*.validation.*" --tests "*.util.*"

# Or specific modules
./gradlew :shared:testDebugUnitTest --tests "*.validation.*"
(cd pocketbase-kt-sdk && ./gradlew test)  # optional when hacking on the SDK locally via the composite build
./gradlew :server:test
```

#### Integration Tests (Requires Running Backend)
```bash
# Start backend services first
./scripts/start-all.sh

# Then run integration tests
./gradlew :shared:testDebugUnitTest --tests "*.integration.*"

# Stop services when done
./scripts/stop-all.sh
```

## 📦 Build Targets Overview

| Target | Platform | Build Command | Output Location |
|--------|----------|---------------|-----------------|
| Android | Mobile | `./gradlew :composeApp:assembleDebug` | `composeApp/build/outputs/apk/` |
| iOS | Mobile | `./gradlew :composeApp:linkDebugFrameworkIosArm64` | `composeApp/build/bin/iosArm64/` |
| Desktop | JVM | `./gradlew :composeApp:createDistributable` | `composeApp/build/compose/binaries/` |
| Web | Browser | `./gradlew :composeApp:jsBrowserDevelopmentRun` | `composeApp/build/dist/js/` |
| Server | Backend | `./gradlew :server:shadowJar` | `server/build/libs/server-all.jar` |

## 🚀 Platform-Specific Builds

### Android
```bash
# Debug APK
./gradlew :composeApp:assembleDebug

# Release APK (requires signing config)
./gradlew :composeApp:assembleRelease

# Install on connected device
./gradlew :composeApp:installDebug
```

### iOS
```bash
# Simulator (Arm64)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Device (Arm64)
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Or use the script
./scripts/run-ios.sh
```

### Desktop (JVM)
```bash
# Run directly
./gradlew :composeApp:run

# Create distributable
./gradlew :composeApp:createDistributable

# Or use the script (fastest dev loop)
./scripts/run-desktop.sh
```

### Web (JavaScript)
```bash
# Development server with hot-reload
./gradlew :composeApp:jsBrowserDevelopmentRun
# Access at http://localhost:8080

# Or use the script
./scripts/run-web.sh
```

### Server (Ktor)
```bash
# Build fat JAR
./gradlew :server:shadowJar

# Run server
java -jar server/build/libs/server-all.jar

# Or use the script
./scripts/run-server.sh
```

## 🖥️ Emulator & Device Checklist

### Android Emulator (Pixel/AVD)
1. Open **Android Studio → Device Manager** and start a Pixel-class emulator (API 34+).
2. From the repo root run `./scripts/run-android.sh` – it builds, installs, and launches the debug APK on the running emulator or any attached device.
3. To smoke-test different densities, duplicate the virtual device (Fold, Tablet) and rerun the same script.

### iOS Simulator (Xcode)
1. Ensure Xcode 15.4+ command-line tools are installed.
2. Run `./scripts/run-ios.sh` to open the Xcode project, pick `iPhone 15 Pro` (or your favorite sim), then hit **Run**.
3. For CI-style builds without the IDE: `xcodebuild -scheme iosApp -configuration Debug -sdk iphonesimulator -arch arm64 build`.

### Desktop (macOS/Linux) & Windows 11 VM
- Fast dev loop on macOS/Linux: `./scripts/run-desktop.sh` (Compose Desktop hot reload).
- To generate native installers:
  - macOS DMG: `./gradlew :composeApp:createDistributable` (outputs under `composeApp/build/compose/binaries/`).
  - Linux DEB: run the same command on a Debian-based machine or container.
- **Windows 11 emulator / VM**:
  1. Boot your Windows 11 VM/emulator (Hyper-V, Parallels, etc.) and clone this repo inside it.
  2. From PowerShell run `gradlew.bat :composeApp:createDistributable -Pcompose.desktop.application.nativeDistributions.targetFormat=msi`.
  3. Install the generated MSI from `composeApp\build\compose\binaries\main\msi` and launch the app like any native Windows program.

### Web Preview
- Hot reload server: `./scripts/run-web.sh` (wraps `:composeApp:jsBrowserDevelopmentRun`).
- Production artifact (for inspection in browsers/Windows Edge): `./gradlew :composeApp:jsBrowserProductionWebpack`.

### Backend Stack Health
- PocketBase + Ktor API: `./scripts/start-all.sh` spins up both services; `./scripts/stop-all.sh` tears them down.
- Verify health endpoints before running integration tests:
  ```bash
  curl http://localhost:8080/health
  curl http://localhost:8090/api/health
  ```

## 🧪 Testing Strategy

### Test Categories

1. **Unit Tests** - Fast, no dependencies
   - Location: `shared/src/commonTest/kotlin/love/bside/app/validation/`
   - Run: `./gradlew test --tests "*.validation.*"`
   - Always pass ✅

2. **Integration Tests** - Require running backend
   - Location: `shared/src/commonTest/kotlin/love/bside/app/integration/`
   - Run: Start services → `./gradlew test --tests "*.integration.*"`
   - Need PocketBase + Server running

3. **Platform-Specific Tests**
   - Android: `./gradlew :shared:testDebugUnitTest`
   - JVM: `./gradlew :shared:jvmTest`
   - iOS: `./gradlew :shared:iosSimulatorArm64Test`
   - JS: `./gradlew :shared:jsTest`

### Running All Tests (Complete)

```bash
# 1. Start backend services
./scripts/start-all.sh

# 2. Wait for services to be ready (~5 seconds)
sleep 5

# 3. Run all tests
./gradlew test

# 4. Stop services
./scripts/stop-all.sh
```

## ⚡ Common Build Workflows

### Daily Development (Fast)
```bash
# Build + run specific platform
./scripts/run-desktop.sh    # Fastest feedback loop
./scripts/run-web.sh         # Browser with hot-reload

# Run unit tests only
./gradlew test --tests "*.validation.*"
```

### Pre-Commit Checks
```bash
# Verify all targets compile
./scripts/verify-targets.sh

# Run unit tests
./gradlew test --tests "*.validation.*" --tests "*.util.*"
```

### Full Integration Test
```bash
# Complete end-to-end test
./scripts/test-full-stack.sh
```

### Release Build
```bash
# Build all platforms for release (artifacts + tests)
./gradlew clean build

# Package outputs
ls composeApp/build/outputs/apk/release/        # Android
ls composeApp/build/bin/iosArm64/releaseFramework/  # iOS
ls composeApp/build/compose/binaries/main/      # Desktop
ls server/build/libs/server-all.jar             # Server
```

## 🐛 Troubleshooting

### Build Failures

**Webpack not found (JS builds)**
```bash
# Hydrate Node/webpack toolchain then retry the task
./gradlew kotlinNodeJsSetup
./gradlew :composeApp:jsBrowserProductionWebpack
```

**Integration tests failing**
```bash
# Skip all tests
./gradlew assemble -x test

# Or start backend first
./scripts/start-all.sh
```

**Out of memory**
```bash
# Increase Gradle heap
export GRADLE_OPTS="-Xmx4g"
./gradlew assemble
```

### Test Failures

**Integration tests fail**
- Ensure backend is running: `./scripts/start-all.sh`
- Check PocketBase: `curl http://localhost:8090/api/health`
- Check Server: `curl http://localhost:8080/health`

**Platform-specific test failures**
- iOS tests require macOS with Xcode
- Android tests require Android SDK
- Use `./gradlew test --tests "*.validation.*"` for cross-platform tests only

## 📊 Build Performance

| Command | Time | Purpose |
|---------|------|---------|
| `./gradlew assemble` | ~3-4 min | Full build, no tests |
| `./scripts/verify-targets.sh` | ~1-2 min | Quick compile check |
| `./scripts/run-desktop.sh` | ~30 sec | Single platform (fastest) |
| `./gradlew test` | ~2-3 min | All tests (backend must run) |
| `./gradlew clean build` | ~5-8 min | Complete rebuild + tests |

## ✅ Recommended Workflow

**For feature development:**
```bash
# 1. Fast iteration on desktop
./scripts/run-desktop.sh

# 2. Test changes
./gradlew test --tests "*.validation.*"

# 3. Verify all platforms compile
./scripts/verify-targets.sh

# 4. Full integration test before PR
./scripts/test-full-stack.sh
```

**For releases:**
```bash
# 1. Clean build all targets + unit tests
./gradlew clean build

# 2. Run full integration suite
./scripts/test-full-stack.sh

# 3. Package artifacts
# See outputs in build directories listed above
```

## 🎯 Success Criteria

✅ **Build succeeds when:**
- All platforms compile: `./gradlew assemble`
- Unit tests pass: `./gradlew test --tests "*.validation.*"`
- Integration tests pass with backend running

✅ **Targets configured:**
- Android (arm64-v8a, armeabi-v7a)
- iOS (arm64 device, arm64 simulator)
- Desktop (JVM - macOS, Windows, Linux)
- Web (JavaScript/Browser)
- Server (JVM/Ktor)

## 📝 Notes

- **WasmJS is disabled** - Koin dependency doesn't support WASM yet
- **Integration tests require backend** - Use `./scripts/start-all.sh` first
- **JS production builds work end-to-end** - `gradle build` drives `jsBrowserProductionWebpack`; rerun `./gradlew kotlinNodeJsSetup` if Node tooling is missing
- **GitHub Actions disabled** - Build locally to save costs
- **Hot reload available** - Desktop and Web support hot reload for fast iteration
