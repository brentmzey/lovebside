# B-Side

B-Side is a modern, multiplatform application built with Kotlin Multiplatform, Compose Multiplatform, and Ktor. It targets Android, iOS, Desktop, and Web from a single codebase.

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**
- **Android Studio** (for Android development)
- **Xcode** (for iOS development on macOS)
- **adb** (Android Debug Bridge) available in your PATH

## Project Structure

- `composeApp/`: The shared Compose Multiplatform UI code for all targets.
- `server/`: The Ktor backend server.
- `shared/`: Shared business logic and data models for the client and server.
- `iosApp/`: The Xcode project for the iOS application.
- `pocketbase/`: The PocketBase database schema and executable.
- `scripts/`: Contains helper scripts for the project.

## Getting Started

This project uses a set of streamlined shell scripts for building and running the various application targets.

### 1. Start All Core Services

To start the backend server, the desktop app, and the web app simultaneously, run:

```bash
./start-all.sh
```

This will:
1.  Start the backend server in the background.
2.  Wait for the server to become available.
3.  Start the desktop and web apps in the background.

To stop all running processes, simply press `Ctrl+C` in the terminal where `start-all.sh` is running, or run:

```bash
./stop-all.sh
```

### 2. Run Individual Platforms

You can also run each platform target individually.

- **Backend Server:**
  ```bash
  ./run-server.sh
  ```

- **Desktop App (JVM):**
  ```bash
  ./run-desktop.sh
  ```

- **Web App (Wasm):**
  ```bash
  ./run-web.sh
  ```

- **Android App:**
  Ensure you have an emulator running or a device connected, then run:
  ```bash
  ./run-android.sh
  ```

- **iOS App (macOS only):**
  This will open the project in Xcode, where you can run it on a simulator or device.
  ```bash
  ./run-ios.sh
  ```

## Testing

To run the available tests for the project, use the following scripts:

- **Full Stack Test (Server + DB):**
  ```bash
  ./test-full-stack.sh
  ```

- **Server + DB Test:**
  ```bash
  ./test-server-db.sh
  ```

## Additional Documentation

For more detailed information, refer to the following documents:

- `POCKETBASE_SCHEMA.md`: The schema for the PocketBase database.
- `SHARED_TYPES_GUIDE.md`: A guide to the shared data types between the client and server.
- `DESIGN_SYSTEM.md`: An overview of the design system used in the Compose UI.