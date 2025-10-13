# B-Side - Enterprise-Ready Kotlin Multiplatform App

## ✅ Current Status (October 13, 2025)

**BUILD STATUS**: ✅ **SUCCESS** - All platform targets compile successfully!

### Working Targets
- ✅ Android (Debug & Release)
- ✅ iOS (arm64, simulator arm64, x64)
- ✅ JVM/Desktop
- ✅ JavaScript/Web
- ✅ WebAssembly (WasmJS - experimental)

### Recent Fixes
- Fixed Koin DI to work across all platforms with platform-specific abstraction
- Resolved Logger expect/actual implementation issues
- Added Compose Material Icons Extended to commonMain
- Fixed repository dependency injection (HttpClient vs PocketBaseClient)
- Updated deprecated APIs (String.capitalize, Icons)
- Refactored all components to use platform-agnostic DI

📖 **See [COMPILATION_FIX_SUMMARY.md](./COMPILATION_FIX_SUMMARY.md) for complete details**

### Quick Build
```bash
# Build all platforms
./gradlew :shared:assemble

# Android Debug
./gradlew :shared:assembleDebug
```

---

## Overview

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), and Server. The application has been **productionalized** with enterprise-level features including robust error handling, logging, validation, caching, network resilience, and polished UI components.

## 🌟 Enterprise Features

### Core Infrastructure
- ✅ **Type-Safe Error Handling**: Result monad with comprehensive AppException hierarchy
- ✅ **Multi-Platform Logging**: Platform-specific loggers with environment-aware levels
- ✅ **Configuration Management**: Environment-specific configs with feature flags
- ✅ **Network Resilience**: Automatic retry with exponential backoff, smart error mapping
- ✅ **Input Validation**: Comprehensive validators for forms and data
- ✅ **Caching System**: In-memory cache with TTL support
- ✅ **Professional UI Components**: Loading, error, empty states, validated forms
- ✅ **Material Design 3 Theme**: Complete light/dark theme with 35+ color tokens

### Architecture
- Clean Architecture with clear separation of concerns
- Repository pattern for data layer
- Use case pattern for business logic
- MVVM for presentation layer
- Dependency Injection (Koin for most platforms, manual DI for WasmJS)

### Developer Experience
- Type-safe APIs throughout
- Extension functions for common operations
- Composable and reusable components
- Comprehensive documentation (see `DEVELOPER_GUIDE.md`)
- Testing-ready architecture

### User Experience
- Consistent Material Design 3 UI
- Smooth loading and error states
- Inline validation with helpful messages
- Retry mechanisms for failed operations
- Responsive and accessible design

## 📚 Documentation

- **[PRODUCTIONALIZATION.md](./PRODUCTIONALIZATION.md)**: Complete overview of enterprise features
- **[DEVELOPER_GUIDE.md](./DEVELOPER_GUIDE.md)**: Quick start guide and code examples
- **[KMP_HIERARCHY_BEST_PRACTICES.md](./KMP_HIERARCHY_BEST_PRACTICES.md)**: Kotlin Multiplatform hierarchy guide
- **[KMP_HIERARCHY_VERIFICATION.md](./KMP_HIERARCHY_VERIFICATION.md)**: Hierarchy template verification steps
- **[README.md](./README.md)**: This file - project overview and setup

## 🚀 Project Structure

* [/composeApp](./composeApp/src) is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
    - [commonMain](./composeApp/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
      For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./composeApp/src/iosMain/kotlin) folder would be the right place for such calls.
      Similarly, if you want to edit the Desktop (JVM) specific part, the [jvmMain](./composeApp/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/iosApp](./iosApp/iosApp) contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

* [/shared](./shared/src) is for the code that will be shared between all targets in the project.
  The most important subfolder is [commonMain](./shared/src/commonMain/kotlin). If preferred, you
  can add code to the platform-specific folders here too.

## 🏗️ Kotlin Multiplatform Configuration

### ✅ Default Hierarchy Template Applied

This project uses Kotlin's **Default Hierarchy Template** for optimal code sharing across platforms. This means:

- **Automatic source set hierarchy** - intermediate source sets are created automatically
- **Better code sharing** - common code for similar platforms (e.g., all iOS targets share `iosMain`)
- **No manual configuration** - follows Kotlin best practices out of the box
- **Future-proof** - adapts to new targets automatically

### Source Set Hierarchy

```
                commonMain
                    │
    ┌───────────────┼───────────────┬─────────┬─────────┐
    │               │               │         │         │
androidMain    nativeMain       jvmMain   jsMain  wasmJsMain
                    │
              ┌─────┴─────┐
              │           │
          appleMain       │
              │           │
          iosMain     (linuxMain)
              │
    ┌─────────┼─────────┐
    │         │         │
iosArm64  iosSim    iosX64
```

### Where to Put Your Code

| Code Type | Location | Available On |
|-----------|----------|--------------|
| Business logic | `commonMain/` | All platforms |
| iOS shared | `iosMain/` | All iOS targets (device + simulators) |
| Android only | `androidMain/` | Android only |
| Desktop only | `jvmMain/` | JVM/Desktop only |
| Web only | `jsMain/` or `wasmJsMain/` | Web browsers |
| Server only | `server/src/main/kotlin` | Backend server |

**Key Benefits:**
- ✅ No hierarchy template warnings
- ✅ Automatic intermediate source sets (`iosMain`, `appleMain`, `nativeMain`)
- ✅ Maximum code reuse across similar platforms
- ✅ Cleaner build configuration

See [KMP_HIERARCHY_BEST_PRACTICES.md](./KMP_HIERARCHY_BEST_PRACTICES.md) for detailed examples and patterns.

### Build and Run Android Application

To build and run the development version of the Android app, use the run configuration from the run widget
in your IDE’s toolbar or build it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:assembleDebug
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:assembleDebug
  ```

### Build and Run Desktop (JVM) Application

To build and run the development version of the desktop app, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :composeApp:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :composeApp:run
  ```

### Build and Run Server

To build and run the development version of the server, use the run configuration from the run widget
in your IDE’s toolbar or run it directly from the terminal:

- on macOS/Linux
  ```shell
  ./gradlew :server:run
  ```
- on Windows
  ```shell
  .\gradlew.bat :server:run
  ```

### Build and Run Web Application

To build and run the development version of the web app, use the run configuration from the run widget
in your IDE's toolbar or run it directly from the terminal:

- for the Wasm target (faster, modern browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
      ```
- for the JS target (slower, supports older browsers):
    - on macOS/Linux
      ```shell
      ./gradlew :composeApp:jsBrowserDevelopmentRun
      ```
    - on Windows
      ```shell
      .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
      ```

### Build and Run iOS Application

To build and run the development version of the iOS app, use the run configuration from the run widget
in your IDE’s toolbar or open the [/iosApp](./iosApp) directory in Xcode and run it from there.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).