# Local Development & Build Guide

**Last Updated:** 2026-01-24  
**Platform Support:** macOS, Linux, Windows

---

## 🚀 Quick Start (5 Minutes)

### Prerequisites

**Required:**
- **JDK 21+** (Temurin/Adoptium recommended)
- **Git**

**Platform-Specific:**
- **Android:** Android SDK (via Android Studio)
- **iOS:** Xcode 15+ (macOS only)
- **Web:** Node.js 20+
- **Desktop:** No additional requirements

---

## 📦 Clone & Setup

```bash
# Clone repository
git clone https://github.com/brentmzey/lovebside.git
cd lovebside

# Verify Java version
java -version  # Should be 21+

# First-time setup (optional, auto-runs on first build)
./gradlew --version
```

---

## 🏗️ Build Commands

### Android

#### Debug APK (Fastest)
```bash
# Build debug APK
./gradlew :composeApp:assembleDebug

# Output location
ls -lh composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Install on device/emulator
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Or build + install in one command
./gradlew :composeApp:installDebug
```

#### Release APK (Signed)
```bash
# Requires keystore setup (see below)
./gradlew :composeApp:assembleRelease

# Output
ls -lh composeApp/build/outputs/apk/release/
```

**Build Time:** ~3-5 minutes (first build), ~30 seconds (incremental)

---

### iOS

**Requirements:** macOS with Xcode 15+

#### Open in Xcode
```bash
# Open iOS project
open iosApp/iosApp.xcodeproj
```

#### Build from Command Line
```bash
# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# For device (requires signing)
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Output location
ls -lh composeApp/build/bin/iosSimulatorArm64/debugFramework/
```

#### Run in Simulator
```bash
# List available simulators
xcrun simctl list devices

# Boot simulator (if not running)
xcrun simctl boot "iPhone 15 Pro"

# Build and run from Xcode
# Product → Run (⌘R)
```

**Build Time:** ~5-10 minutes (first build), ~1-2 minutes (incremental)

---

### Desktop

#### Run Directly (Development)
```bash
# Run on current OS
./gradlew :composeApp:run

# This opens the app window immediately
```

#### Create Distributable Package

**macOS (DMG):**
```bash
./gradlew :composeApp:packageDmg

# Output
ls -lh composeApp/build/compose/binaries/main/dmg/
```

**Windows (MSI):**
```bash
./gradlew :composeApp:packageMsi

# Output
ls -lh composeApp/build/compose/binaries/main/msi/
```

**Linux (DEB):**
```bash
./gradlew :composeApp:packageDeb

# Output
ls -lh composeApp/build/compose/binaries/main/deb/
```

**Build Time:** ~2-4 minutes (first build), ~30 seconds (incremental)

---

### Web (Browser)

#### Development Server
```bash
# Run development server with hot reload
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# Open browser to: http://localhost:8080
```

#### Production Build
```bash
# Build optimized production bundle
./gradlew :composeApp:jsBrowserProductionWebpack

# Output
ls -lh composeApp/build/dist/js/productionExecutable/

# Serve locally to test
cd composeApp/build/dist/js/productionExecutable
python3 -m http.server 8000
# Open: http://localhost:8000
```

**Build Time:** ~3-5 minutes (first build), ~1 minute (incremental)

---

## 📸 Taking Screenshots

### Setup Screenshot Directories

```bash
# Create screenshot directories
mkdir -p docs/screenshots/{android,ios,web,desktop}/{chat,reactions,profile,settings}
mkdir -p docs/screenshots/baselines
```

---

### Android Screenshots

#### Using ADB (Command Line)
```bash
# Take screenshot
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png docs/screenshots/android/chat/main-screen.png
adb shell rm /sdcard/screenshot.png

# Shortcut function (add to ~/.bashrc)
android-screenshot() {
  local name=${1:-screenshot.png}
  adb shell screencap -p /sdcard/temp.png
  adb pull /sdcard/temp.png "$name"
  adb shell rm /sdcard/temp.png
  echo "Screenshot saved: $name"
}

# Usage
android-screenshot docs/screenshots/android/chat/conversation-list.png
```

#### Using Android Studio
1. Run app in emulator
2. Click camera icon in emulator toolbar
3. Or: Tools → Device Manager → Screenshot
4. Save to `docs/screenshots/android/`

#### Automated Screenshots (Recommended)
```bash
# Add to your test suite
# composeApp/src/androidTest/kotlin/ScreenshotTest.kt

@Test
fun captureMainScreen() {
    composeTestRule.onRoot().captureToImage()
        .asAndroidBitmap()
        .saveToFile("screenshots/main-screen.png")
}
```

---

### iOS Screenshots

#### Using Simulator
```bash
# Take screenshot (⌘S in simulator)
# Or command line:
xcrun simctl io booted screenshot screenshot.png

# Move to docs
mv screenshot.png docs/screenshots/ios/chat/main-screen.png

# Shortcut function
ios-screenshot() {
  local name=${1:-screenshot.png}
  xcrun simctl io booted screenshot "$name"
  echo "Screenshot saved: $name"
}

# Usage
ios-screenshot docs/screenshots/ios/chat/conversation-list.png
```

#### Using Xcode
1. Run app in simulator
2. Go to simulator window
3. File → Save Screen (⌘S)
4. Or use Screenshot toolbar (⌘⇧4 + click simulator)

---

### Desktop Screenshots

#### macOS
```bash
# Capture specific window
# 1. Press ⌘⇧4, then Space
# 2. Click on app window
# 3. Screenshot saved to ~/Desktop

# Or command line
screencapture -w docs/screenshots/desktop/chat/main-window.png
# Then click the window

# Timed capture (5 seconds)
screencapture -T 5 -w screenshot.png
```

#### Linux
```bash
# Using GNOME
gnome-screenshot -w -f docs/screenshots/desktop/chat/main-window.png

# Using KDE
spectacle -w -o docs/screenshots/desktop/chat/main-window.png

# Using ImageMagick
import docs/screenshots/desktop/chat/main-window.png
# Then click window
```

#### Windows
```bash
# Using built-in Snipping Tool
# Win + Shift + S

# Or PowerShell
Add-Type -AssemblyName System.Windows.Forms
[System.Windows.Forms.SendKeys]::SendWait("{PRTSC}")
# Then paste into Paint and save
```

---

### Web Screenshots

#### Browser DevTools
```bash
# Chrome/Edge
# 1. F12 to open DevTools
# 2. Ctrl/Cmd + Shift + P
# 3. Type "screenshot"
# 4. Choose "Capture full size screenshot"

# Firefox
# 1. F12 to open DevTools
# 2. Ctrl/Cmd + Shift + P
# 3. Type "screenshot"
# 4. Choose ":screenshot --fullpage"
```

#### Command Line (Headless)
```bash
# Install Playwright
npm install -D @playwright/test

# Take screenshot script
cat > take-screenshot.js << 'EOF'
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setViewportSize({ width: 1280, height: 720 });
  await page.goto('http://localhost:8080');
  await page.screenshot({ 
    path: 'docs/screenshots/web/chat/main-screen.png',
    fullPage: true 
  });
  await browser.close();
})();
EOF

node take-screenshot.js
```

#### Automated Screenshot Script
```bash
#!/usr/bin/env bash
# scripts/capture-web-screenshots.sh

set -e

echo "Starting web app..."
./gradlew :composeApp:jsBrowserDevelopmentRun &
WEB_PID=$!

echo "Waiting for server to start..."
sleep 10

echo "Capturing screenshots..."
node << 'EOF'
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.setViewportSize({ width: 1280, height: 720 });
  
  // Login page
  await page.goto('http://localhost:8080');
  await page.screenshot({ path: 'docs/screenshots/web/auth/login.png' });
  
  // More screenshots...
  
  await browser.close();
})();
EOF

echo "Stopping web server..."
kill $WEB_PID

echo "Screenshots saved to docs/screenshots/web/"
```

---

## 🎥 Recording Videos/GIFs

### Android

#### Using ADB
```bash
# Record video (Ctrl+C to stop)
adb shell screenrecord /sdcard/demo.mp4

# Pull video
adb pull /sdcard/demo.mp4 docs/videos/android/chat-demo.mp4
adb shell rm /sdcard/demo.mp4

# Convert to GIF
ffmpeg -i docs/videos/android/chat-demo.mp4 -vf "fps=10,scale=320:-1:flags=lanczos" docs/videos/android/chat-demo.gif
```

#### Using Android Studio
1. Run app
2. Click record button in emulator toolbar
3. Stop recording
4. Save video

---

### iOS

#### Using Simulator
```bash
# Start recording
xcrun simctl io booted recordVideo demo.mov

# Stop with Ctrl+C

# Move to docs
mv demo.mov docs/videos/ios/chat-demo.mov

# Convert to GIF
ffmpeg -i docs/videos/ios/chat-demo.mov -vf "fps=10,scale=320:-1:flags=lanczos" docs/videos/ios/chat-demo.gif
```

---

### Desktop

#### macOS (QuickTime)
1. Open QuickTime Player
2. File → New Screen Recording
3. Select app window
4. Start recording

#### Linux (FFmpeg)
```bash
ffmpeg -f x11grab -s 1280x720 -i :0.0 output.mp4
# Ctrl+C to stop
```

#### Windows (Xbox Game Bar)
1. Win + G to open Game Bar
2. Click record button
3. Stop when done

---

### Web

#### Chrome DevTools
```bash
# Use Puppeteer for automated recording
npm install puppeteer

cat > record-web.js << 'EOF'
const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch({ headless: false });
  const page = await browser.newPage();
  await page.goto('http://localhost:8080');
  
  // Your interactions here
  await page.click('#login-button');
  await page.waitForTimeout(2000);
  
  await browser.close();
})();
EOF
```

---

## 🔧 Development Tips

### Fast Incremental Builds

```bash
# Use configuration cache for faster builds
./gradlew build --configuration-cache

# Parallel builds
./gradlew build --parallel

# Both
./gradlew build --parallel --configuration-cache
```

### Live Reload

**Web:**
```bash
# Auto-rebuild on file changes
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
```

**Desktop:**
```bash
# Install gradle-watch plugin (if available)
./gradlew :composeApp:run --watch
```

### Clean Build (When Things Break)

```bash
# Clean all build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean build

# Nuclear option (delete all caches)
rm -rf .gradle/ build/ */build/
./gradlew build
```

---

## 🐛 Troubleshooting

### Build Failures

**"Gradle daemon disappeared unexpectedly"**
```bash
# Increase memory
export GRADLE_OPTS="-Xmx4g"
./gradlew build
```

**"Out of memory"**
```bash
# Edit gradle.properties
echo "org.gradle.jvmargs=-Xmx8g" >> gradle.properties
```

**"Cannot resolve dependencies"**
```bash
# Refresh dependencies
./gradlew build --refresh-dependencies
```

### Android Issues

**"SDK not found"**
```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk          # Linux
```

**"Device not found"**
```bash
# Check connected devices
adb devices

# Restart ADB
adb kill-server
adb start-server
```

### iOS Issues

**"Code signing error"**
- Open in Xcode
- Select project → Signing & Capabilities
- Choose your team

**"Simulator not found"**
```bash
# List simulators
xcrun simctl list devices

# Create new simulator
xcrun simctl create "iPhone 15 Pro" "iPhone 15 Pro"
```

---

## 📸 Screenshot Naming Convention

```
docs/screenshots/{platform}/{feature}/{description}.png

Examples:
  docs/screenshots/android/chat/conversation-list.png
  docs/screenshots/android/chat/message-compose.png
  docs/screenshots/android/reactions/reaction-picker.png
  docs/screenshots/ios/profile/user-profile.png
  docs/screenshots/web/settings/preferences.png
  docs/screenshots/desktop/chat/main-window-light.png
  docs/screenshots/desktop/chat/main-window-dark.png
```

**Recommended Sizes:**
- **Mobile:** 1080x2400 (9:16)
- **Tablet:** 2048x2732 (3:4)
- **Desktop:** 1920x1080 (16:9)
- **Web:** 1280x720 (16:9)

---

## 🎬 Automated Screenshot Workflow

Create `scripts/capture-all-screenshots.sh`:

```bash
#!/usr/bin/env bash
set -e

echo "📸 Capturing screenshots for all platforms..."

# Android
if command -v adb &> /dev/null; then
  echo "📱 Android screenshots..."
  ./scripts/capture-android-screenshots.sh
fi

# iOS (macOS only)
if [[ "$OSTYPE" == "darwin"* ]]; then
  echo "🍎 iOS screenshots..."
  ./scripts/capture-ios-screenshots.sh
fi

# Desktop
echo "🖥️  Desktop screenshots..."
./scripts/capture-desktop-screenshots.sh

# Web
echo "🌐 Web screenshots..."
./scripts/capture-web-screenshots.sh

echo "✅ All screenshots captured!"
echo "📂 Location: docs/screenshots/"
ls -lhR docs/screenshots/
```

---

## 📚 Next Steps

After capturing screenshots:

1. **Optimize images:**
   ```bash
   # Install optipng and jpegoptim
   find docs/screenshots -name "*.png" -exec optipng -o7 {} \;
   ```

2. **Add to documentation:**
   - Update README.md with screenshots
   - Add to docs/FEATURES.md
   - Include in release notes

3. **Commit:**
   ```bash
   git add docs/screenshots/
   git commit -m "docs: Add screenshots for all platforms"
   git push
   ```

---

## 🎯 Quick Reference Card

```bash
# Android
./gradlew :composeApp:assembleDebug && adb install composeApp/build/outputs/apk/debug/*.apk

# iOS
open iosApp/iosApp.xcodeproj  # Then ⌘R

# Desktop
./gradlew :composeApp:run

# Web
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous

# Screenshot (Android)
adb shell screencap -p /sdcard/s.png && adb pull /sdcard/s.png screenshot.png

# Screenshot (iOS)
xcrun simctl io booted screenshot screenshot.png

# Clean build
./gradlew clean build
```

---

**Maintained by:** BSide Dev Team  
**Last Updated:** 2026-01-24  
**Feedback:** Open an issue on GitHub
