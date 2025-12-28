# BSide - Build, Run & Test Guide

## 🚀 Quick Start

### Build Everything

```bash
# Build all platforms
./gradlew build

# Build specific platforms
./gradlew :shared:build              # Shared KMP code
./gradlew :composeApp:assembleDebug  # Android APK
./gradlew :composeApp:compileKotlinIosSimulatorArm64  # iOS
```

### Run Tests

```bash
# All tests
./gradlew test

# Specific module tests
./gradlew :shared:test
./gradlew :shared:jvmTest
./gradlew :composeApp:testDebugUnitTest  # Android unit tests
```

## 📱 Android Studio - Launch Emulators

### Setup

1. **Open Project**: File → Open → Select `/Users/brentzey/bside`
2. **Sync Gradle**: Click "Sync Now" banner or File → Sync Project with Gradle Files
3. **Wait for indexing** to complete

### Launch Android Emulator

#### Method 1: Run Configuration (Recommended)

1. **Select configuration** dropdown (top toolbar) → `composeApp`
2. **Select device**: Click device dropdown → Choose emulator or create new
3. **Click Run** (green play button) or press `^R`

#### Method 2: Terminal

```bash
# Install on connected device/emulator
./gradlew :composeApp:installDebug

# Or run directly
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

#### Create New Android Emulator

1. Tools → Device Manager (or AVD Manager)
2. Click "Create Device"
3. Select device (e.g., Pixel 6)
4. Select system image (e.g., API 34 - Tiramisu)
5. Finish → Click Play to launch

### Launch iOS Simulator

#### From Android Studio (KMP Plugin)

1. **Install Kotlin Multiplatform Mobile plugin** (if not installed)
   - Settings → Plugins → Search "Kotlin Multiplatform Mobile" → Install
2. **Select iOS configuration**: `iosApp` in dropdown
3. **Select simulator**: Choose from device dropdown
4. **Click Run**

#### From Xcode (Recommended)

```bash
# Open iOS project
open iosApp/iosApp.xcodeproj

# Or from terminal
cd iosApp
xcodebuild -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15'
```

#### From Terminal

```bash
# Build for simulator
./gradlew :composeApp:iosSimulatorArm64Test

# Or use Xcode command line
cd iosApp
xcodebuild -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 15' build
```

## 🖥️ Desktop (JVM)

```bash
# Run desktop app
./gradlew :composeApp:run

# Or build JAR
./gradlew :composeApp:packageUberJarForCurrentOS
# Output: composeApp/build/compose/jars/
```

## 🌐 Web (Wasm)

```bash
# Run web app in browser
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Build production
./gradlew :composeApp:wasmJsBrowserProductionWebpack
```

## 🧪 Testing All Targets

### Unit Tests

```bash
# All platforms
./gradlew :shared:allTests

# Specific platforms
./gradlew :shared:jvmTest
./gradlew :shared:iosSimulatorArm64Test
./gradlew :composeApp:testDebugUnitTest  # Android
```

### Integration Tests

```bash
# Shared module integration tests
./gradlew :shared:jvmTest --tests "*Integration*"

# Android instrumented tests (requires emulator running)
./gradlew :composeApp:connectedDebugAndroidTest
```

## 🔧 Common Tasks

### Clean Build

```bash
./gradlew clean build
```

### Check Code Quality

```bash
# Kotlin lint
./gradlew detekt

# Android lint
./gradlew :composeApp:lintDebug
```

### Generate APK

```bash
./gradlew :composeApp:assembleDebug
# Output: composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Release (requires signing config)
./gradlew :composeApp:assembleRelease
```

### Install on Device

```bash
# Debug build
./gradlew :composeApp:installDebug

# Uninstall
./gradlew :composeApp:uninstallDebug
```

## 🎯 Android Studio Shortcuts

| Action | Shortcut (Mac) | Shortcut (Win/Linux) |
|--------|----------------|----------------------|
| Run | `^R` | `Shift+F10` |
| Debug | `^D` | `Shift+F9` |
| Sync Gradle | `⌘⇧O` | `Ctrl+Shift+O` |
| Device Manager | `⌘⇧A` → "Device Manager" | `Ctrl+Shift+A` → "Device Manager" |
| Terminal | `⌥F12` | `Alt+F12` |

## 📊 Build Status Check

```bash
# Quick health check
./gradlew tasks --all | grep -E "(build|assemble|test)"

# Verify all platforms compile
./gradlew :shared:build :composeApp:assembleDebug \
  :composeApp:compileKotlinIosSimulatorArm64
```

## 🐛 Troubleshooting

### Gradle Daemon Issues

```bash
./gradlew --stop  # Stop all daemons
./gradlew build   # Fresh build
```

### Android Studio Not Recognizing Project

1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.idea` folder and re-open project
3. Ensure Android SDK is installed: Settings → Appearance & Behavior → System Settings → Android SDK

### Emulator Not Starting

```bash
# List available emulators
emulator -list-avds

# Start specific emulator
emulator -avd Pixel_6_API_34 &

# Check adb devices
adb devices
```

### iOS Simulator Issues

```bash
# List available simulators
xcrun simctl list devices

# Boot simulator
xcrun simctl boot "iPhone 15"

# Open Simulator app
open -a Simulator
```

## 🎨 Running the Messaging UI Demo

The messaging UI components are ready! To integrate:

1. **Open `composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/messaging/MessagingDemo.kt`**
2. **Add to navigation** (or run standalone)
3. **Launch on any platform** to see:
   - MessageBubble with gradients
   - ConversationList with avatars
   - MessageComposer with send button

## ✅ Current Build Status

- ✅ **Android**: `assembleDebug` builds successfully
- ✅ **iOS**: `compileKotlinIosSimulatorArm64` builds successfully  
- ✅ **Desktop**: JVM compilation successful
- ✅ **Messaging UI**: All 3 components ready (MessageBubble, ConversationList, MessageComposer)

---

**Pro Tip**: Use Android Studio's "Run Configurations" to set up different targets (Android, iOS, Desktop) for quick switching!
