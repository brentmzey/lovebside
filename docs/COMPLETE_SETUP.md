# 🚀 Complete Multi-Platform Setup & Launch Guide

## ✅ Current Status

**Builds Successfully:**

- ✅ Android APK: Ready (`composeApp/build/outputs/apk/debug/composeApp-debug.apk`)
- ✅ Desktop (JVM): Running (PID 46274)
- ✅ Server: Can build
- ⚠️  iOS: Needs Xcode project setup

**Backend Connection:**

- URL: `https://bside.pockethost.io`
- Configured in: `Constants.kt`, `PocketBaseClient.kt`, `AppConfig.kt`
- Status: Ready to connect

---

## 1️⃣ Android - Launch Now

### Quick Launch (Android Studio)

```bash
# Open Android Studio
open -a "Android Studio" /Users/brentzey/bside
```

**In Android Studio:**

1. Wait for Gradle sync (~30 sec)
2. Select `composeApp` configuration
3. Select/create Android emulator (Tools → Device Manager)
4. Click ▶ Run

**The app will:**

- Connect to <https://bside.pockethost.io>
- Show login/signup screen
- Display messaging UI components

### Command Line (if adb installed)

```bash
# Install platform tools
brew install android-platform-tools

# List/create emulator
emulator -list-avds
# If none: Create in Android Studio Tools → Device Manager

# Start emulator
emulator -avd Pixel_6_API_34 &

# Install app
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Launch
adb shell am start -n love.bside.app/.MainActivity
```

---

## 2️⃣ iOS - Setup Required

### iOS Project Doesn't Exist Yet

Kotlin Multiplatform needs to generate the Xcode project. Here's how:

**Option A: Generate iOS Project (Recommended)**

```bash
# Create iOS project using Kotlin Multiplatform wizard
./gradlew :composeApp:createXCFramework

# OR use KMM plugin in Android Studio
# Settings → Plugins → "Kotlin Multiplatform Mobile"
# Then: New → Kotlin Multiplatform → iOS Framework
```

**Option B: Manual Xcode Project Setup**

1. Open Xcode
2. Create New Project → iOS → App
3. Product Name: `iosApp`
4. Save in `/Users/brentzey/bside/iosApp/`
5. Add framework:
   - Build Phases → Link Binary with Libraries
   - Add `ComposeApp.framework` from `composeApp/build/bin/iosSimulatorArm64/debugFramework/`

**Option C: Use Android Studio KMM Plugin** (Easiest)

```bash
# In Android Studio:
# 1. Install "Kotlin Multiplatform Mobile" plugin
# 2. File → Project Structure → Modules → Add iOS module
# 3. Sync and run
```

### Once iOS Project Exists

```bash
open iosApp/iosApp.xcodeproj
# Select iPhone simulator → Run
```

---

## 3️⃣ Desktop - Already Running! ✅

```bash
# Check if running
ps aux | grep "love.bside.app" | grep -v grep

# Or start new instance
./gradlew :composeApp:run

# Package as standalone app
./gradlew :composeApp:packageUberJarForCurrentOS
# Output: composeApp/build/compose/jars/
```

---

## 4️⃣ Web (JS/Wasm)

```bash
# Run dev server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Build production
./gradlew :composeApp:jsBrowserProductionWebpack

# Output: composeApp/build/dist/js/productionExecutable/
```

---

## 5️⃣ Backend Connection Test

### Verify Backend is Reachable

```bash
curl https://bside.pockethost.io/api/health
```

**Expected:** PocketBase health response

### Start Local PocketBase (Optional)

```bash
cd pocketbase
./pocketbase serve

# Local URL: http://127.0.0.1:8090
```

**To use local backend:**
Edit `shared/src/commonMain/kotlin/love/bside/app/Constants.kt`:

```kotlin
const val USE_PRODUCTION = false  // Change to false for local
```

---

## 🎯 What Each Platform Will Show

### All Platforms Display

1. **Landing Screen** - Orbit animation with profile avatars
2. **Login/Signup** - Beautiful gradient UI
3. **Messaging UI**:
   - MessageBubble (gradients, tails, animations)
   - ConversationList (avatars, online status, unread badges)
   - MessageComposer (input field, send button)

### Backend Features

- User authentication (email/password)
- Profile creation
- Messaging (real-time via PocketBase)
- **Matching Algorithm** (server-side JS hook):
  - Jaccard similarity on interests (40 pts)
  - Location matching (20 pts)
  - Proust questionnaire compatibility (40 pts)
  - Threshold: 15+ pts creates match

---

## 🔧 Troubleshooting

### Android Studio Not Showing Run Button

```bash
# Sync Gradle
# File → Sync Project with Gradle Files

# Invalidate caches
# File → Invalidate Caches → Restart
```

### Backend Connection Failed

**Check:**

```bash
ping bside.pockethost.io
curl https://bside.pockethost.io/api/health
```

**If down, use local:**

```bash
cd pocketbase && ./pocketbase serve
```

Then set `USE_PRODUCTION = false` in `Constants.kt`

### iOS Build Fails

**Common issues:**

- Xcode not installed: `xcode-select --install`
- Wrong SDK: Open Xcode → Preferences → Locations → Command Line Tools
- Framework not built: `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`

---

## ✅ Summary - Ready to Launch

**Working NOW:**

1. **Android**: Open Android Studio → Run ✅
2. **Desktop**: Already running (PID 46274) ✅
3. **Backend**: <https://bside.pockethost.io> ✅

**Needs Setup:**

1. **iOS**: Generate Xcode project first
2. **Web**: Can run with `jsBrowserDevelopmentRun`

**Backend Integration:**

- All platforms configured to connect to production
- Authentication, messaging, matching all ready
- Local PocketBase available for development

---

## 📱 Quick Start Right Now

```bash
# 1. Open Android Studio
open -a "Android Studio" /Users/brentzey/bside

# 2. Click Run when Gradle syncs complete
# That's it! App launches with full backend connectivity
```

**Android will work immediately!** iOS needs project generation first.
