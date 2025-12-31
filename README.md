# B-Side: Kotlin Multiplatform Messaging App

An advanced messaging application built with Kotlin Multiplatform (KMP), Compose Multiplatform, and PocketBase.

## 🚀 Triple Test Verification Guide

To verify the integrity of the application across all platforms ("Triple Test"), follow these steps.

### Prerequisites

- JDK 17+ (Eclipse Temurin 21 recommended)
- Docker & Docker Compose
- Android Studio (for Android)
- Xcode (for iOS)
- CocoaPods (`sudo gem install cocoapods`)

---

## 🚀 One Command to Rule Them All

We have verified a single script to build, check infrastructure, and verify all targets.

```bash
./start-everything.sh
```

*This script starts Docker, waits for the backend, launches the Desktop app, and opens Android Studio & Xcode for you.*

---

## 🛠️ Manual Verification Guide

If you prefer manual control or need to debug specific targets, follow these steps.

### 1. **Run Backend (Infrastructure)**

The server and database must be running for any client to function.

```bash
# Start PocketBase and Ktor Server
docker-compose up -d --build
```

**Verification:**

- **PocketBase Admin:** Open [http://127.0.0.1:8090/_/](http://127.0.0.1:8090/_/)
- **Ktor Server:** Open [http://127.0.0.1:8080/health](http://127.0.0.1:8080/health) (Should return `OK`)

---

### 2. **Run Desktop (Rapid Testing)**

The Desktop target is the fastest way to verify Logic and UI without emulator overhead.

```bash
# Run the Desktop Application
./gradlew :composeApp:run
```

---

### 3. **Run Mobile Targets**

#### **Android**

```bash
# Install and run on connected device/emulator
./gradlew :composeApp:installDebug
```

*Open Android Studio, select `composeApp`, and press Run (Shift+F10).*

#### **iOS**

```bash
# Build the iOS Framework (checking for linker errors)
./gradlew :composeApp:iosArm64Binaries
```

*Open `iosApp/iosApp.xcodeproj` in Xcode and press Run (Cmd+R).*

#### **Web (Wasm/JS)**

*(Currently experimental/disabled in build.gradle.kts)*

```bash
# Run Web Dev Server
./gradlew :composeApp:jsBrowserRun
```

---

### 4. **Run All Tests (CI Check)**

To verify code integrity without UI:

```bash
# Run Unit Tests for Common and JVM Logic
./gradlew test
```

---

## 🏗️ Architecture & Type System

This project uses **Kotlin Multiplatform (KMP)** to share 100% of the business logic and 99% of the UI.

### Type Exports

We export our Kotlin Type System to other languages to ensure type safety across boundaries.

#### **TypeScript (Web/Node)**

- **Tool:** internal Gradle task (via KJS / Kotlin Wrappers)
- **Output:** `build/js/packages/bside/kotlin/bside.d.ts`
- **Usage:** Frontend web apps import these definitions to ensure API contract matching.

#### **Swift / Objective-C (iOS)**

- **Tool:** Kotlin Native Compiler (cinterop)
- **Output:** `Shared.framework` (Header files)
- **Usage:** iOS native code (SwiftUI) sees Kotlin classes as Obj-C compatible classes (e.g., `SharedAuthRepository`).

#### **Java (Server/Android)**

- **Tool:** Kotlin JVM Compiler
- **Output:** `.class` / `.jar` files
- **Usage:** Seamless interop. Kotlin classes appear as standard Java classes.

---

## 📂 Project Structure

- **`composeApp`**: Main UI application (Compose Multiplatform).
- **`shared`**: Core business logic, repositories, and ViewModels.
- **`server`**: Ktor backend service.
- **`iosApp`**: Thin iOS entry point.
- **`docs/`**: Deployment scripts, legacy documentation, and guides.
