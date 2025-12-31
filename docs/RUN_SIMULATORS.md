# 🚀 Running Simulators on All Targets

## Quick Start - One Command Per Platform

```bash
# Make scripts executable
chmod +x run-*.sh

# iOS
./run-ios.sh

# Android  
./run-android.sh

# Desktop
./gradlew :composeApp:run

# Server
./gradlew :server:run
```

---

## iOS Simulator - Step by Step

### Method 1: Using Script (Automatic)

```bash
./run-ios.sh
```

### Method 2: Using Xcode (Recommended)

```bash
open iosApp/iosApp.xcodeproj
```

**In Xcode:**

1. Top bar: Select "iosApp" scheme
2. Device dropdown: Select "iPhone 15" (or any simulator)
3. Click ▶ Run button (⌘R)

### Method 3: Command Line

```bash
# Build framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# Boot simulator
xcrun simctl boot "iPhone 15"
open -a Simulator

# In Xcode or:
cd iosApp
xcodebuild -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15'
```

---

## Android Emulator - Step by Step

### Method 1: Using Script (Automatic)

```bash
./run-android.sh
```

### Method 2: Using Android Studio (Recommended)

```bash
open -a "Android Studio" .
```

**In Android Studio:**

1. Wait for Gradle sync
2. Top bar: Select "composeApp" configuration
3. Device dropdown: Select emulator (or create new)
4. Click ▶ Run button (^R)

### Method 3: Command Line

```bash
# Install platform tools if needed
brew install android-platform-tools

# List emulators
emulator -list-avds

# Start emulator (replace with your AVD name)
emulator -avd Pixel_6_API_34 &

# Wait for boot
adb wait-for-device

# Install and launch
./gradlew :composeApp:installDebug
adb shell am start -n love.bside.app/.MainActivity
```

**Create Android Emulator:**

```bash
# In Android Studio
# Tools → Device Manager → ➕ Create Device
# Select: Pixel 6, API 34 (Tiramisu)
```

---

## Desktop - Step by Step

### Direct Run

```bash
./gradlew :composeApp:run
```

### Package as Application

```bash
./gradlew :composeApp:packageUberJarForCurrentOS

# Output in: composeApp/build/compose/jars/
# Double-click to run
```

---

## Server - Step by Step

### Local Server

```bash
./gradlew :server:run

# Server starts on: http://localhost:8080
```

### PocketBase Backend (Alternative)

```bash
cd pocketbase
./pocketbase serve

# Admin UI: http://127.0.0.1:8090/_/
```

---

## All Platforms at Once

```bash
# Terminal 1 - iOS
./run-ios.sh

# Terminal 2 - Android  
./run-android.sh

# Terminal 3 - Desktop
./gradlew :composeApp:run

# Terminal 4 - Server
./gradlew :server:run
```

---

## Troubleshooting

### iOS Simulator Not Found

```bash
# List available simulators
xcrun simctl list devices

# Create new simulator in Xcode
# Xcode → Window → Devices and Simulators → Simulators → +
```

### Android Emulator Fails

```bash
# Check SDK location
echo $ANDROID_HOME

# If not set:
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/emulator:$ANDROID_HOME/platform-tools

# Or use Android Studio to manage emulators
```

### Desktop Window Not Appearing

```bash
# Check if running
ps aux | grep "love.bside.app"

# Kill and restart
pkill -f "love.bside.app"
./gradlew :composeApp:run
```

---

## What You'll See

All platforms connect to: **<https://bside.pockethost.io>**

**Screens:**

1. Landing - Orbit animation with avatars
2. Login/Signup - Email auth with biometrics
3. Messaging - ConversationList, MessageBubble, MessageComposer
4. Matches - Algorithm-driven compatibility matches

**All platforms show identical UI!** 🎨
