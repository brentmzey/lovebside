# BSide Build & Run Scripts

This directory contains scripts to build, test, and run the BSide app across all platforms.

## Scripts

### 🚀 build-all.sh

Comprehensive build, test, and run script for all platforms.

**Usage:**

```bash
# Interactive mode (prompts for target)
./build-all.sh

# Build, test, and run desktop
./build-all.sh desktop

# Build, test, and run web (WebAssembly)
./build-all.sh web-wasm

# Skip tests and just run
./build-all.sh --skip-tests desktop

# Clean, build, test, and run
./build-all.sh --clean desktop

# Just build and test (no run)
./build-all.sh all
```

**Options:**

- `-h, --help` - Show help
- `-t, --skip-tests` - Skip running tests
- `-b, --skip-build` - Skip build (only run)
- `-v, --verbose` - Show verbose gradle output
- `--clean` - Clean before building

**Targets:**

- `desktop` - Desktop (JVM) app
- `web-wasm` - Web (WebAssembly) app  
- `web-js` - Web (JavaScript) app
- `android` - Android app (requires device/emulator)
- `ios` - iOS app (requires macOS & Xcode)
- `all` - Build all, no run

### 💬 demo-realtime.sh

Quick demo script for real-time messaging between two platforms.

**Usage:**

```bash
./demo-realtime.sh
```

This will:

1. Build the app
2. Start Desktop app (for User 1)
3. Start Web app in browser (for User 2)
4. You can login and chat in real-time!

**Test Accounts:**

- User 1: `test@example.com` / `test12345`
- User 2: `test2@example.com` / `test12345`

Press `Ctrl+C` to stop both apps.

## Quick Reference

### Build Only

```bash
./gradlew build
```

### Run Tests

```bash
./gradlew test
```

### Platform-Specific Commands

**Desktop:**

```bash
./gradlew :composeApp:run
```

**Web (Wasm):**

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

**Web (JS):**

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

**Android:**

```bash
./gradlew :composeApp:installDebug
```

**iOS:**

```bash
open iosApp/iosApp.xcodeproj
```

## Features

### ✅ Multi-Platform Support

- Android (mobile)
- iOS (mobile)
- Desktop (macOS, Windows, Linux)
- Web (Wasm & JS)

### ✅ Real-Time Messaging

- Instant message delivery across platforms
- Online/offline status
- Read receipts
- Typing indicators

### ✅ Offline Support

- Messages queue when offline
- Auto-sync when connection restored
- Optimistic UI updates
- Distributed caching via `t_user_property` table

## Troubleshooting

**Permission denied:**

```bash
chmod +x build-all.sh demo-realtime.sh
```

**Port 8080 already in use:**

```bash
# Find process using port 8080
lsof -i :8080

# Kill it
kill -9 <PID>
```

**Android device not found:**

```bash
# Check connected devices
adb devices

# Start emulator if needed
# (from Android Studio > AVD Manager)
```

**Gradle daemon issues:**

```bash
./gradlew --stop
./gradlew build
```

## Demo Instructions

### Real-Time Messaging Demo

1. **Run the demo script:**

   ```bash
   ./demo-realtime.sh
   ```

2. **Login on both apps:**
   - Desktop: <test@example.com>
   - Web: <test2@example.com>

3. **Create conversation** from either app

4. **Send messages** - they appear instantly!

### Offline Mode Demo

1. Start app on any platform
2. Login and open a conversation
3. **Go offline** (disconnect WiFi/network)
4. Send messages - they show as "pending"
5. **Go online** - messages sync automatically!

### Screen Recording

**macOS:**

```bash
# Built-in tool
Cmd + Shift + 5

# Or QuickTime
# File > New Screen Recording
```

**Android:**

```bash
adb shell screenrecord /sdcard/demo.mp4
adb pull /sdcard/demo.mp4
```

**Linux:**

```bash
# Using ffmpeg
ffmpeg -video_size 1920x1080 -framerate 30 -f x11grab -i :0.0 demo.mp4
```

## Architecture

The build scripts use Gradle tasks but orchestrate them for multi-platform workflows:

```
build-all.sh
├── Build Phase
│   ├── :shared:build (Kotlin Multiplatform shared code)
│   └── :composeApp:build (Compose Multiplatform UI)
├── Test Phase
│   ├── :shared:test (Unit & integration tests)
│   └── :composeApp:test (UI tests)
└── Run Phase
    ├── Desktop: :composeApp:run
    ├── Web: :composeApp:wasmJsBrowserDevelopmentRun
    └── Android: :composeApp:installDebug
```

## Next Steps

- [ ] Add CI/CD integration (GitHub Actions)
- [ ] Add release build variants
- [ ] Add performance profiling options
- [ ] Add Docker containerization for web deploy
- [ ] Add distribution packaging (AppImage, DMG, MSI)

## Resources

- [Kotlin Multiplatform Docs](https://kotlinlang.org/docs/multiplatform.html)
- [Compose Multiplatform Docs](https://www.jetbrains.com/lp/compose-multiplatform/)
- [Gradle User Guide](https://docs.gradle.org/current/userguide/userguide.html)
