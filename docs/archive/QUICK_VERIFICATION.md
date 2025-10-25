# Quick Verification Commands ⚡

Run these to verify everything works:

## 🔧 Build All Platforms

```bash
cd /Users/brentzey/bside

# Android
./gradlew :composeApp:assembleDebug --no-daemon

# iOS Simulator
./gradlew :composeApp:compileKotlinIosSimulatorArm64 --no-daemon

# Desktop (JVM)
./gradlew :composeApp:jvmJar --no-daemon

# Web
./gradlew :composeApp:jsBrowserDevelopmentWebpack --no-daemon

# All at once (takes ~2 min)
./gradlew :composeApp:build :shared:build :pocketbase-kt-sdk:build --no-daemon
```

## 🧪 Test Security Setup

```bash
# Verify repositories use InternalApiClient (should return 5+ matches)
grep -r "InternalApiClient" shared/src/commonMain/kotlin/love/bside/app/data/repository --include="*.kt" | wc -l

# Verify PocketBase NOT used in UI (should return 0)
grep -r "PocketBaseClient" composeApp/src/commonMain --include="*.kt" | grep -v "import" | wc -l

# Check JWT configuration
grep -r "JWT\|jwt" server/src/main/kotlin/love/bside/server/plugins/Security.kt
```

## 🎨 Check Color Palette

```bash
# View theme colors
cat composeApp/src/commonMain/kotlin/love/bside/app/ui/theme/Theme.kt | grep "Color(0x"
```

## 📦 SDK Verification

```bash
# Compile SDK for all platforms
./gradlew :pocketbase-kt-sdk:compileKotlinJvm \
          :pocketbase-kt-sdk:compileKotlinAndroid \
          :pocketbase-kt-sdk:compileKotlinIosSimulatorArm64 \
          :pocketbase-kt-sdk:compileKotlinIosArm64 \
          --no-daemon

# Test local publishing
./gradlew :pocketbase-kt-sdk:publishToMavenLocal --no-daemon
```

## ✅ Expected Results

All commands should return:
```
BUILD SUCCESSFUL in Xs
```

If you see errors, check:
- iOS: Should all pass (we fixed them!)
- Tests: May have some disabled tests (harmless)
- Daemon: Use `--no-daemon` to avoid memory issues

## 🚀 Run the App

### Android
```bash
# Build APK
./gradlew :composeApp:assembleDebug

# APK location:
# composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

### iOS
```bash
# Open in Xcode
open iosApp/iosApp.xcodeproj

# Or via command line
./gradlew :composeApp:iosSimulatorArm64Run
```

### Desktop
```bash
./gradlew :composeApp:run
```

### Web
```bash
./gradlew :composeApp:jsBrowserRun --continuous
# Open http://localhost:8080
```

## 📊 Project Stats

```bash
# Count Kotlin files
find . -name "*.kt" -not -path "*/build/*" | wc -l

# Count lines of code
find . -name "*.kt" -not -path "*/build/*" -exec wc -l {} + | tail -1

# Check module structure
ls -la | grep -E "shared|composeApp|server|pocketbase-kt-sdk"
```

---

**Pro Tip**: If builds are slow, try:
```bash
# Clean build cache
./gradlew clean

# Build with parallel execution
./gradlew build --parallel --max-workers=8
```
