# B-Side Development Workflow

This guide details how to build, run, and inspect the B-Side application across all platforms and verify backend algorithms.

## Prerequisites

- **Java JDK 17+**
- **Docker** (for local PocketBase & auxiliary services)
- **Android Studio** (for Android Emulator)
- **Xcode** (for iOS Simulator - macOS only)
- **Node.js** (for web target)

## 1. Backend Setup

The backend (PocketBase) must be running for any client to function.

```bash
# Start PocketBase (Docker)
./scripts/run-server.sh
```
*   **Console**: http://127.0.0.1:8090/_/
*   **API**: http://127.0.0.1:8090/api/

## 2. Running Clients

Use the provided helper scripts to run specific platforms.

### Android
Ensure an Android Emulator is running or a device is connected via ADB.
```bash
./scripts/run-android.sh
```

### iOS (macOS only)
Ensure Xcode and iPhone Simulator are installed.
```bash
./scripts/run-ios.sh
```

### Desktop (JVM)
Runs the app as a native window on your computer.
```bash
./scripts/run-desktop.sh
```

### Web (Wasm/Js)
Runs the app in your default browser.
```bash
./scripts/run-web.sh
```

## 3. Verification & Testing

### Testing Matching Algorithm
To verify the affinity matching engine (`cron_matching.ts`), use the automated test script. This script:
1.   seeds test users (Alice, Bob, Charlie) with specific interests and locations.
2.  Triggers the matching algorithm via API.
3.  Verifies that compatible users matched and incompatible ones did not.

```bash
./scripts/test-algo-full.sh
```

### Analyzing Logs
- **Backend Logs**: View the terminal where `./scripts/run-server.sh` is running.
- **Client Logs**:
    - **Android**: `adb logcat | grep "co.touchlab"` (or package name)
    - **iOS**: View in Xcode Console.

## 4. Project Tracking (Code-HQ)

We use `Code-HQ` structure (`.code-hq/`) to track progress.

### Updating Status
Edit `CODEHQ.md` to update high-level status.
Edit `.code-hq/entities/tasks.md` to update specific task statuses.

### CLI Usage (Optional)
If you have the `code-hq` CLI installed:
```bash
code-hq status
```
