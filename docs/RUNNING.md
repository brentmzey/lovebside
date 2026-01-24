# Running B-Side

This guide covers how to run the backend services and valid client targets.

## Quick Start (Using Just)

If you have `just` installed, you can use the following commands:

```bash
# 1. Start Backend Stack (PocketBase + Ktor Server)
just up

# 2. Run Clients (New Terminal Tabs)
just web      # Web Browser (Hot Reload)
just desktop  # Desktop App (Hot Reload)
just android  # Android Emulator
just ios      # iOS Simulator (via Xcode)
```

---

## Backend Services

### Docker Compose Stack (Recommended)

This runs both PocketBase and the Ktor Server in a unified network.

```bash
docker-compose up --build
```

- **PocketBase**: [http://127.0.0.1:8090/_/](http://127.0.0.1:8090/_/)
- **Ktor Server**: [http://127.0.0.1:8080](http://127.0.0.1:8080)

### Manual Setup

If you prefer running services individually:

**1. PocketBase**

```bash
./scripts/setup_dev_env.sh
```

**2. Ktor Server**

```bash
./gradlew :server:run
```

---

## Client Targets

### Web (Browser)

Starts a local development server with webpack hot module replacement.

```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```

* **Hot Reload**: Configured automatically. Changes apply instantly.

### Desktop (JVM)

Runs the desktop application.

```bash
./gradlew :composeApp:jvmRun
```

* **Hot Reload**: To use JetBrains Compose Hot Reload:

    ```bash
    ./gradlew :composeApp:hotRunJvm
    ```

### Android

Builds and installs the debug APK on a connected device/emulator.

```bash
./gradlew :composeApp:installDebug
```

* Launch "composeApp" manually if it doesn't open automatically.

### iOS

The iOS app is a native Xcode project that consumes the shared code framework.

1. Open `iosApp/iosApp.xcodeproj` in Xcode.
2. Select your target simulator.
3. Press **Run** (`Cmd+R`).
