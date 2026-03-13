# 🚀 BSide - Run All Platforms

## ONE COMMAND - Launch Everything

```bash
./launch-all.sh
```

This launches:

- ✅ iOS (Xcode)
- ✅ Android (Android Studio)
- ✅ Desktop (JVM window)
- ✅ Web (Browser)
- ✅ Server (API)

---

## Individual Platform Commands

### Android

```bash
./run-android.sh
# OR
./gradlew :composeApp:assembleDebug
open -a "Android Studio" .
# Then: Click Run ▶
```

### Desktop

```bash
./run-desktop.sh
# OR
./gradlew :composeApp:run
```

### Web

```bash
./run-web.sh
# OR  
./gradlew :composeApp:jsBrowserDevelopmentRun
```

### iOS

```bash
./run-ios.sh
# OR
open iosApp/iosApp.xcodeproj
# Then: Select simulator → Run ▶
```

### Server

```bash
./gradlew :server:run
```

---

## All Gradle Commands

```bash
# Build everything
./gradlew build

# Run specific targets
./gradlew :composeApp:run                          # Desktop
./gradlew :composeApp:installDebug                 # Android install
./gradlew :composeApp:jsBrowserDevelopmentRun      # Web dev
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64  # iOS framework
./gradlew :server:run                              # Backend

# Package for distribution
./gradlew :composeApp:packageUberJarForCurrentOS   # Desktop JAR
./gradlew :composeApp:assembleRelease              # Android APK
./gradlew :composeApp:jsBrowserProductionWebpack   # Web production
```

---

## Stop All

```bash
# Kill all running processes
pkill -f 'gradle.*run'
```

---

## Platform Matrix

| Platform | Command | Output |
|----------|---------|--------|
| **Android** | `./run-android.sh` | Emulator |
| **iOS** | `./run-ios.sh` | Simulator |
| **Desktop** | `./run-desktop.sh` | Window |
| **Web** | `./run-web.sh` | Browser |
| **Server** | `./gradlew :server:run` | <http://localhost:8080> |
| **ALL** | `./launch-all.sh` | Everything! |

**All connect to**: `https://bside.pockethost.io` 🎯
