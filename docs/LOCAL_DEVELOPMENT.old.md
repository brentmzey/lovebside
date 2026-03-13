# 🚀 B-Side Local Development Guide

**Last Updated:** 2026-01-24  
**Platform Support:** macOS, Linux, Windows

Complete step-by-step guide for building and running B-Side locally.

---

## 📋 Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Backend Setup](#backend-setup)
4. [Running Clients](#running-clients)
5. [Android Studio & Emulators](#android-studio--emulators)
6. [iOS Development](#ios-development)
7. [Database & Migrations](#database--migrations)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

✅ **Java Development Kit (JDK) 17+**
```bash
# Check version
java -version
# Install via SDKMAN (recommended)
curl -s "https://get.sdkman.io" | bash
sdk install java 17.0.9-tem
```

✅ **Docker Desktop** (for backend services)
- Download: https://www.docker.com/products/docker-desktop
- **Must be running before starting backend**

✅ **Android Studio** (Latest stable version)
- Download: https://developer.android.com/studio
- Required for Android development and emulators

✅ **Node.js 18+** (for tooling scripts)
```bash
node --version
npm --version
```

### Optional but Recommended

- **Just** (Task runner): `brew install just` or `cargo install just`
- **Xcode** (macOS only, for iOS): From Mac App Store
- **Git**: Should already be installed

---

## Quick Start

### 1. Clone and Configure

```bash
# Clone repository
git clone https://github.com/brentmzey/lovebside.git
cd bside

# Copy environment template
cp .env.example .env

# Edit .env with your settings
nano .env
```

### 2. Configure Environment

Edit `.env` file:

```bash
# PocketBase Admin (will be auto-created)
POCKETBASE_ADMIN_EMAIL=tester_admin@bside.love
POCKETBASE_ADMIN_PASSWORD=password123

# Local backend URLs
POCKETBASE_URL=http://localhost:8092
SERVER_URL=http://localhost:8081

# Production (for testing against live backend)
POCKETHOST_URL=https://lovebside.pockethost.io

# CDN (keep disabled for local dev)
CDN_ENABLED=false
```

### 3. Verify Setup

```bash
# Check Java
java -version  # Should show 17+

# Check Docker
docker --version
docker ps  # Should not error

# Check Gradle
./gradlew --version
```

---

## Backend Setup

### Start Backend Services (Docker)

**Method 1: Using Just (Recommended)**

```bash
# Build and start all services
just up

# This starts:
# - PocketBase on http://localhost:8092
# - Ktor Server on http://localhost:8081  
# - Nginx on http://localhost:8082
```

**Method 2: Using Docker Compose Directly**

```bash
# Build server JAR first
./gradlew :server:shadowJar

# Start services
docker-compose up --build

# Run in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop services
docker-compose down
```

### Verify Backend is Running

```bash
# Check PocketBase health
curl http://localhost:8092/api/health
# Expected: {"code":200,"message":"","data":{}}

# Check Ktor Server health
curl http://localhost:8081/health
# Expected: {"status":"OK"}

# Or check Docker containers
docker ps
# Should show: bside-pocketbase, bside-server, bside-nginx
```

### Access PocketBase Admin UI

1. Open browser: http://localhost:8092/_/
2. **First time only:** Create admin account
   - Email: `tester_admin@bside.love`
   - Password: `password123`
3. You can now browse collections, manage data, etc.

### Seed Test Data

```bash
# Seed demo users and profiles
./scripts/seed_for_demo.sh

# Or seed specific data
./scripts/seed_users.sh        # Just users
./scripts/seed_data.sh         # All collections
```

---

## Running Clients

### Desktop (JVM)

```bash
# Standard run
./gradlew :composeApp:jvmRun

# Or with just
just desktop

# Hot reload (experimental)
just desktop-hot
```

**Window will open automatically**

### Web Browser

```bash
# Start web dev server
./gradlew :composeApp:jsBrowserDevelopmentRun

# Or with just
just web
```

Access at: **http://localhost:8080**

**Hot Reload:** Web client automatically reloads on code changes!

### WebAssembly (Experimental)

```bash
./scripts/run-wasm.sh
```

---

## Android Studio & Emulators

### Setup Android Studio

1. **Download and Install**
   - Get from: https://developer.android.com/studio
   - Install with default settings

2. **Open B-Side Project**
   ```bash
   # From project root
   ./scripts/open-android-studio.sh
   
   # Or manually:
   # Android Studio > Open > Select /path/to/bside
   ```

3. **First-Time Setup in Android Studio**
   - Wait for Gradle sync to complete (~5-10 min first time)
   - Install any SDK components that Android Studio prompts for
   - Recommended: SDK Platform 34 (Android 14)

### Create Android Emulator

#### Option 1: Through Android Studio UI

1. **Open Device Manager**
   - Click device icon in toolbar, or
   - Tools > Device Manager

2. **Create Virtual Device**
   - Click **"Create Device"**
   - Select **Phone** > **Pixel 5** (recommended)
   - Click **Next**

3. **Select System Image**
   - Choose **API Level 34** (Android 14)
   - If not downloaded, click **Download** next to it
   - Click **Next**

4. **Configure AVD**
   - Name: `Pixel_5_API_34` (or your choice)
   - Graphics: **Hardware** (faster)
   - Click **Finish**

#### Option 2: Command Line

```bash
# List installed system images
sdkmanager --list | grep system-images

# Download Android 14 image (if needed)
sdkmanager "system-images;android-34;google_apis;x86_64"

# Create AVD
avdmanager create avd \
  -n Pixel_5_API_34 \
  -k "system-images;android-34;google_apis;x86_64" \
  -d pixel_5
```

### Run App in Emulator

#### Method 1: Android Studio (Easiest)

1. **Select Run Configuration**
   - Top toolbar dropdown > Select **`composeApp`**

2. **Select Device**
   - Device dropdown > Select your emulator

3. **Run**
   - Click green **Run** button (▶️)
   - Or press `Ctrl+R` (Mac: `Cmd+R`)

**Emulator will auto-start if not running**

#### Method 2: Command Line

```bash
# Start emulator in background
emulator -avd Pixel_5_API_34 &

# Wait for emulator to boot
adb wait-for-device

# Install and run app
./gradlew :composeApp:installDebug
adb shell am start -n love.bside.app/love.bside.app.MainActivity
```

### Emulator Tips

**Speed Up Emulator:**
- Use Hardware Graphics: `Graphics: Hardware - GLES 2.0`
- Allocate more RAM: Edit AVD > Advanced > RAM: 2048 MB
- Enable Device Frame: OFF (faster rendering)

**Common Emulator Commands:**
```bash
# List all AVDs
emulator -list-avds

# Start specific AVD
emulator -avd Pixel_5_API_34

# Start with wipe (clean state)
emulator -avd Pixel_5_API_34 -wipe-data

# Kill all emulators
adb devices | grep emulator | cut -f1 | xargs -I {} adb -s {} emu kill
```

**Debug Logs:**
```bash
# View app logs
adb logcat | grep "B-SIDE"

# Clear app data
adb shell pm clear love.bside.app

# Uninstall app
adb uninstall love.bside.app
```

### Accessing Local Backend from Emulator

**Important:** Android emulators can't access `localhost` directly!

**Use this URL in your emulator:**
```
http://10.0.2.2:8092  # Maps to localhost:8092 on host
```

To configure:
1. Open app settings in emulator
2. Change API URL to `http://10.0.2.2:8092`
3. Or set in app's dev config before building

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

#### Setup iOS Development

1. **Install Xcode**
   ```bash
   # From Mac App Store or developer.apple.com
   # After install, accept license:
   sudo xcodebuild -license accept
   
   # Install command line tools
   xcode-select --install
   ```

2. **Install CocoaPods**
   ```bash
   sudo gem install cocoapods
   
   # Or via Homebrew
   brew install cocoapods
   ```

3. **Setup iOS Dependencies**
   ```bash
   cd iosApp
   pod install
   cd ..
   ```

#### Open in Xcode

```bash
# IMPORTANT: Always open .xcworkspace, NOT .xcodeproj
open iosApp/iosApp.xcworkspace

# Or with just
just ios
```

#### Run in iOS Simulator

1. **In Xcode:**
   - Select scheme: **iosApp**
   - Select simulator: **iPhone 15** or **iPhone 15 Pro**
   - Click **Run** (▶️) or press `Cmd+R`

2. **Command Line:**
   ```bash
   # List simulators
   xcrun simctl list devices
   
   # Boot simulator (if needed)
   xcrun simctl boot "iPhone 15"
   
   # Build and run
   cd iosApp
   xcodebuild -workspace iosApp.xcworkspace \
              -scheme iosApp \
              -configuration Debug \
              -destination 'platform=iOS Simulator,name=iPhone 15' \
              build
   ```

#### Run on Physical iPhone

1. **Connect iPhone via USB**

2. **Trust Computer on iPhone**
   - Unlock iPhone
   - Tap "Trust" when prompted

3. **Setup Signing in Xcode**
   - Select **iosApp** target
   - Go to **Signing & Capabilities** tab
   - Select your **Team** (personal or organization)
   - Enable **"Automatically manage signing"**

4. **Run**
   - Select your iPhone from device list
   - Click **Run** (▶️)
   - First time: App may need to be trusted on iPhone
     - Settings > General > VPN & Device Management
     - Tap your developer cert > Trust

#### Accessing Local Backend from iOS

**iOS Simulator:**
- Can access `localhost` directly
- Use: `http://localhost:8092`

**Physical iPhone:**
- Can't access `localhost` on Mac
- Use your Mac's IP address: `http://192.168.1.X:8092`
- Find IP: `ifconfig | grep "inet " | grep -v 127.0.0.1`

#### iOS Build from Command Line

```bash
# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

# For device (requires signing)
./gradlew :composeApp:linkDebugFrameworkIosArm64

# Output location
ls -lh composeApp/build/bin/iosSimulatorArm64/debugFramework/
```

**Build Time:** ~5-10 minutes (first build), ~1-2 minutes (incremental)

#### Taking Screenshots

```bash
./scripts/screenshot-ios.sh
# Screenshots saved to: docs/screenshots/ios/
```

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

## Database & Migrations

### PocketBase Database Management

#### Access Admin UI

```bash
# With backend running (just up)
# Open: http://localhost:8092/_/

# Login with:
# Email: tester_admin@bside.love
# Password: password123
```

#### Database Migrations

```bash
# Check migration status
just migrate-status

# Run pending migrations
just migrate

# Create new migration
just migrate-create add_reactions_field

# Rollback last migration
just migrate-down
```

#### Schema Management

```bash
# Export current schema (creates snapshot)
just schema-export
# Saves to: pocketbase/schemas/schema_YYYYMMDD_HHMMSS.json

# Validate schema against production
just schema-validate

# Compare with specific snapshot
just schema-diff schema_20250124_120000.json
```

#### Seed Test Data

```bash
# Seed all demo data (recommended for first-time)
./scripts/seed_for_demo.sh

# This creates:
# - Test users (alice, bob, charlie, etc.)
# - Sample profiles with photos
# - Demo conversations
# - Sample messages

# Seed specific collections
./scripts/seed_users.sh          # Just users
./scripts/seed_data.sh s_profiles  # Just profiles
```

#### Reset Local Database (DESTRUCTIVE)

```bash
# ⚠️  WARNING: This deletes ALL local data!

# Stop services
just down

# Remove database
rm -rf pocketbase/pb_data/*
rm -rf pocketbase_local/pb_data/*

# Restart with fresh DB
just up

# Wait for services to start
sleep 10

# Re-seed data
./scripts/seed_for_demo.sh
```

#### Backup Database

```bash
# Backup local PocketBase data
tar -czf pocketbase-backup-$(date +%Y%m%d).tar.gz pocketbase/pb_data/

# Restore from backup
tar -xzf pocketbase-backup-20250124.tar.gz
```

#### Testing Migrations

```bash
# Test on fresh DB (safe - only affects local)
just test-migrations

# This will:
# 1. Destroy local DB
# 2. Start fresh PocketBase
# 3. Apply all migrations
# 4. Validate schema

# Quick check migration status
just test-migration-status
```

#### Production Migration (CAREFUL!)

```bash
# Full validation before deploying
just validate-all

# Apply to production (requires confirmation)
just migrate-prod
# Follow prompts to enter production URL
```

### Direct Database Queries (Advanced)

```bash
# SQLite shell access
cd pocketbase/pb_data
sqlite3 data.db

# Example queries
sqlite> SELECT * FROM _collections;
sqlite> SELECT id, email, username FROM users LIMIT 10;
sqlite> .exit
```

---

## Testing

### Run All Tests

```bash
# All tests (unit + integration)
./gradlew test

# Specific module
./gradlew :shared:test
./gradlew :composeApp:test
./gradlew :pocketbase-kt-sdk:test
```

### Unit Tests Only

```bash
# Run JVM unit tests
./gradlew :shared:jvmTest
./gradlew :composeApp:jvmTest

# With verbose output
./gradlew test --info

# Specific test class
./gradlew test --tests "ChatViewModelTest"
```

### Integration Tests (Requires Backend)

```bash
# Start backend first
just up
sleep 10

# Run integration tests
./gradlew :shared:jvmTest --tests "*Integration*"
./gradlew :shared:jvmTest --tests "*Verification*"

# Full stack test (automated)
./scripts/test-full-stack.sh
```

### Android Instrumented Tests

```bash
# Start emulator first
emulator -avd Pixel_5_API_34 &
adb wait-for-device

# Run tests
./gradlew :composeApp:connectedAndroidTest

# View results
open composeApp/build/reports/androidTests/connected/index.html
```

### Test with Coverage

```bash
# Generate coverage report
./gradlew test jacocoTestReport

# View report
open build/reports/jacoco/test/html/index.html
```

### Continuous Testing

```bash
# Auto-run tests on file changes
./gradlew test --continuous
```

---

##  🐛 Troubleshooting

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

---

## 🚀 Complete Quickstart (Copy-Paste Workflow)

### First Time Setup (10 minutes)

```bash
# 1. Clone and setup
git clone https://github.com/brentmzey/lovebside.git
cd bside
cp .env.example .env

# 2. Start backend services
./gradlew :server:shadowJar
docker-compose up -d

# 3. Wait for services to start (check health)
sleep 15
curl http://localhost:8092/api/health
curl http://localhost:8081/health

# 4. Seed test data
./scripts/seed_for_demo.sh

# Done! Backend is ready
```

### Run Android (in Android Studio)

```bash
# 1. Open project
./scripts/open-android-studio.sh

# 2. Wait for Gradle sync to complete (~5 min first time)

# 3. In Android Studio:
#    - Device Manager > Create Device > Pixel 5 > API 34
#    - Select composeApp run config
#    - Click Run ▶️

# Emulator will launch automatically!
```

### Run iOS (in Xcode)

```bash
# 1. Setup (first time only)
cd iosApp
pod install
cd ..

# 2. Open project
open iosApp/iosApp.xcworkspace

# 3. In Xcode:
#    - Select iosApp scheme
#    - Select iPhone 15 simulator
#    - Click Run ▶️ (or Cmd+R)

# Simulator will launch automatically!
```

### Run Desktop (Instant)

```bash
./gradlew :composeApp:jvmRun
# App window opens immediately!
```

### Run Web (with Hot Reload)

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
# Open http://localhost:8080
# Auto-reloads on code changes!
```

---

## 📞 Getting Help

### Resources

- **Documentation**: https://github.com/brentmzey/lovebside/tree/main/docs
- **Issues**: https://github.com/brentmzey/lovebside/issues
- **Discussions**: https://github.com/brentmzey/lovebside/discussions

### Common First-Time Issues

**Backend won't start?**
```bash
# Check Docker is running
docker ps
# If not, start Docker Desktop
```

**Android build fails?**
```bash
# Set ANDROID_HOME
export ANDROID_HOME=$HOME/Library/Android/sdk
echo 'export ANDROID_HOME=$HOME/Library/Android/sdk' >> ~/.zshrc
```

**iOS build fails?**
```bash
# Install CocoaPods
sudo gem install cocoapods
cd iosApp && pod install
```

**Emulator can't connect to backend?**
```bash
# Android: Use 10.0.2.2 instead of localhost
# iOS: Use your Mac's IP (find with: ifconfig | grep inet)
```

---

## ✅ You're All Set!

You should now be able to:
- ✅ Start the backend (PocketBase + Ktor)
- ✅ Run Android in emulator
- ✅ Run iOS in simulator
- ✅ Run Desktop app
- ✅ Run Web app with hot reload
- ✅ Manage database migrations
- ✅ Run tests
- ✅ Take screenshots

**Next Steps:**
1. Read [Architecture Guide](./ARCHITECTURE.md) to understand the codebase
2. Check [Contributing Guide](../CONTRIBUTING.md) to start contributing
3. Try [Real-Time Features Demo](./REALTIME_DEMO.md)

**Happy Coding! 🎉**

