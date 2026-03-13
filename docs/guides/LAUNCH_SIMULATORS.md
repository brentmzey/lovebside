# 🚀 Launch iOS & Android Simulators - Quick Guide

## Status: ✅ Apps Ready to Launch

**Build Status:**

- ✅ Android APK: `composeApp/build/outputs/apk/debug/composeApp-debug.apk` (25.7 MB)
- ✅ iOS Framework: Compiled and ready
- ✅ Desktop: Running (PID 46274)

---

## 📱 Android Simulator (2 Options)

### Option 1: Android Studio (Easiest - RECOMMENDED)

**I'm opening Android Studio for you now...**

```bash
open -a "Android Studio" /Users/brentzey/bside
```

**Once Android Studio opens:**

1. **Wait for Gradle sync** (bottom status bar, ~30 seconds)
2. **Top toolbar**: Click configuration dropdown → Select `composeApp`
3. **Device dropdown**:
   - If emulator exists: Select it
   - If no emulator: Click "Device Manager" → "Create Device" → Pixel 6, API 34
4. **Click green ▶ Run button**

**That's it!** Android Studio will:

- Build the APK automatically
- Launch the emulator
- Install and run the app

### Option 2: Command Line (Advanced)

```bash
# Install Android SDK platform tools first
brew install android-platform-tools

# List emulators
emulator -list-avds

# Start emulator (replace with your AVD name)
emulator -avd Pixel_6_API_34 &

# Install APK
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Launch app
adb shell am start -n love.bside.app/.MainActivity
```

---

## 🍎 iOS Simulator

**I'm opening Xcode for you now...**

```bash
open iosApp/iosApp.xcodeproj
```

**Once Xcode opens:**

1. **Select a simulator**: Top toolbar → iPhone 15 (or any device)
2. **Click ▶ Run button** (top left)

Xcode will:

- Build the iOS app
- Launch the simulator automatically
- Install and run the app

### Quick Command Line Launch (Alternative)

```bash
# List available simulators
xcrun simctl list devices

# Boot a simulator
xcrun simctl boot "iPhone 15"

# Open Simulator app
open -a Simulator

# Build and run from Xcode or:
cd iosApp
xcodebuild -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15' \
  -derivedDataPath build
```

---

## ✅ What You Should See

### Android App

- BSide logo/branding
- Login/Sign up screen (LandingScreen with orbit animation)
- Beautiful gradient UI with messaging components

### iOS App  

- Same beautiful UI
- All Compose Multiplatform components working
- Smooth animations and interactions

### Desktop App (Already Running!)

- Check for desktop window
- Same UI as mobile platforms

---

## 🎯 Quick Troubleshooting

**Android Studio doesn't show Run button?**

- File → Sync Project with Gradle Files
- Wait for indexing to complete

**No Android emulators?**

- Tools → Device Manager → Create Device
- Select Pixel 6, download API 34 system image

**iOS Xcode build fails?**

- Try: Product → Clean Build Folder
- Then: Product → Build

**App crashes on launch?**

- Check logs in Android Studio (Logcat) or Xcode (Console)
- Likely: Network/PocketBase connection issue (expected without backend)

---

## 🎨 What's Ready in the App

- ✅ **MessageBubble** - Beautiful chat bubbles with gradients
- ✅ **ConversationList** - Avatar, online status, unread badges
- ✅ **MessageComposer** - Input field with send button
- ✅ **Auth Screens** - Login, Sign up with biometric options
- ✅ **Matching Algorithm** - Server-side (PocketBase hook)

---

**Both IDEs should be opening now!** Just click Run in each one and your simulators will launch! 🚀
