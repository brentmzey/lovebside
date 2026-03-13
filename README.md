# B-Side: Kotlin Multiplatform Messaging App

![B-Side Banner](docs/archive/test_image.png)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.20-7f52ff?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose_Multiplatform-1.7.0-4285F4?logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android_|_iOS_|_Desktop_|_Web-orange)](https://kotlinlang.org/docs/multiplatform.html)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Buy Me a Coffee](https://img.shields.io/badge/Buy%20Me%20a%20Coffee-Donate-yellow?logo=buy-me-a-coffee&logoColor=white)](https://buymeacoffee.com/brentmzey)
[![GitHub Sponsors](https://img.shields.io/github/sponsors/brentmzey?color=ea4aaa&logo=github)](https://github.com/sponsors/brentmzey)

**B-Side** is a production-ready messaging application built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**. It demonstrates a "Supercharged" architecture sharing **100% of business logic** and **99% of UI code** across Android, iOS, Desktop, and Web.

---

## 🚀 Quick Start (The Golden Path)

Get up and running with a single command.

### 1. Prerequisites
- **JDK 17+** (Eclipse Temurin 21 recommended)
- **Docker & Docker Compose** (for Backend services)
- **Just** (Command runner) - [Install Guide](https://github.com/casey/just)
    - *Mac:* `brew install just`
    - *Windows:* `choco install just`
- **Node.js 20+** (for Web target)

### 2. Clone & Run
```bash
# 1. Clone the repository
git clone https://github.com/your-org/bside.git
cd bside

# 2. Start everything (Backend + All Clients)
just start

# OR, if you use direnv/source .envrc, simply:
start
```

**Note:** This project uses a clean directory structure. Core commands are located in `bin/` and are automatically added to your `PATH` if you source `.envrc`.

**This intelligent script will:**
1. 🐳 Start PocketBase & Ktor Server (Docker)
2. 🖥️ Launch Desktop Client
3. 🌐 Launch Web Client
4. 📱 Launch Android/iOS apps (if emulators/simulators are active)

---

## 🛠️ Building & Running

We provide simplified `just` commands for daily development, and standard Gradle commands for distribution builds.

| Platform | Run (Dev) | Build (Distributable) | Output Location |
| :--- | :--- | :--- | :--- |
| **Backend Stack** | `just up` | `./gradlew :server:shadowJar` | `server/build/libs/` |
| **Desktop** | `just desktop` | `./gradlew :composeApp:createDistributable` | `composeApp/build/compose/binaries/` |
| **Web** | `just web` | `./gradlew :composeApp:jsBrowserProductionWebpack` | `composeApp/build/dist/js/` |
| **Android** | `just android` | `./gradlew :composeApp:assembleRelease` | `composeApp/build/outputs/apk/` |
| **iOS** | `just ios` | `./gradlew :composeApp:linkReleaseFrameworkIosArm64` | `composeApp/build/bin/iosArm64/` |

### Detailed Platform Instructions

<details>
<summary><strong>🤖 Android</strong></summary>

1. Open `android/` in Android Studio.
2. Select a run configuration and device.
3. Click **Run**.
- Or use CLI: `./scripts/run-android.sh`
</details>

<details>
<summary><strong>🍎 iOS (macOS only)</strong></summary>

1. Ensure Xcode 15+ is installed.
2. Run `just ios` to open the Xcode workspace.
3. Select a Simulator or Device and click **Run**.
- Or use CLI: `./scripts/run-ios.sh`
</details>

<details>
<summary><strong>🖥️ Desktop (JVM)</strong></summary>

- **Hot Reload:** `just desktop-hot` (Experimental)
- **Standard:** `just desktop`
- **Native Installers:**
    - Mac (DMG): `./gradlew :composeApp:packageDmg`
    - Windows (MSI): `./gradlew :composeApp:packageMsi`
    - Linux (DEB): `./gradlew :composeApp:packageDeb`
</details>

<details>
<summary><strong>🌐 Web (JS/Wasm)</strong></summary>

- **Dev Server:** `just web` (starts at http://localhost:8080)
- **Production Build:** The output in `composeApp/build/dist/js/productionExecutable/` can be served by any static host (Nginx, Netlify, Vercel).
</details>

---

## 🚢 Deployment

Detailed deployment guides are available for each component.

### 1. Database (PocketBase)
We use **PocketHost** for production database hosting or a self-hosted Docker container.
- **Migration Workflow:**
    1. `just test-migrations` (Validate locally)
    2. `just migrate-prod` (Apply to production)
- [👉 **View Deployment Workflow**](docs/DEPLOYMENT_WORKFLOW.md)

### 2. Backend API (Ktor)
The Ktor server acts as the API Gateway and Auth/Validation layer.
- **Containerize:** `docker build -t bside-server .`
- **Deploy:** Supports AWS ECS, Google Cloud Run, or any Docker-compatible host.
- [👉 **View AWS Setup Guide**](docs/AWS_CDN_SETUP.md)

### 3. Frontend Distribution
- **Android:** Distribute `.apk` or `.aab` via Google Play Console.
- **iOS:** Archive via Xcode for TestFlight/App Store.
- **Web:** Deploy the `build/dist/js/productionExecutable` folder to Netlify/Vercel.
- **Desktop:** Distribute platform-specific installers (.dmg, .msi, .deb).
- [👉 **View Distribution Guide**](docs/DISTRIBUTION.md)

---

## ✨ Key Messaging Features

B-Side features a robust, real-time communication engine powered by **Server-Sent Events (SSE)** and a state-driven **Representative UI** architecture.

- **🚀 Instant Delivery**: Zero-latency message syncing across all platforms.
- **💬 Real-time Typing**: Live "typing..." indicators for active conversations.
- **✅ Read Receipts**: Visual status tracking for message delivery and viewing (✓/✓✓).
- **🧵 Threaded Replies**: Contextual message threading for organized discussions.
- **🎭 Expressive Reactions**: Tap-to-react emoji support on every message.

![Messaging Demo](docs/videos/realtime_messaging.gif)

---

## 🏗️ Architecture Overview

B-Side uses a centralized architecture where **Shared Code** drives specific **UI implementations**.

```mermaid
graph TD
    subgraph "Shared (Kotlin Multiplatform)"
        Logic[Business Logic & ViewModels]
        Repo[Repositories]
        Net[API Client]
    end

    subgraph "UI Layer (Compose Multiplatform)"
        Android[Android UI]
        iOS[iOS UI]
        Desktop[Desktop UI]
        Web[Web UI]
    end

    subgraph "Backend"
        Ktor[Ktor Server]
        PB[(PocketBase DB)]
    end

    Android & iOS & Desktop & Web --> Logic
    Logic --> Repo
    Repo --> Net
    Net --> Ktor
    Ktor --> PB
```

### Key Technologies
- **UI:** Jetpack Compose Multiplatform
- **Navigation:** Voyager
- **Dependency Injection:** Koin
- **Async:** Kotlin Coroutines & Flow
- **Network:** Ktor Client
- **Database:** PocketBase (Real-time)

---

## 📂 Project Structure

| Directory | Description |
| :--- | :--- |
| **`composeApp`** | Main UI application (Compose Multiplatform). Contains `commonMain`, `androidMain`, `iosMain`, `jvmMain`, `webMain`. |
| **`shared`** | Core business logic, repositories, and ViewModels. Shared across all platforms. |
| **`server`** | Ktor backend service acting as the API gateway. |
| **`pocketbase`** | Database schema, migrations, and types. |
| **`scripts`** | Automation scripts for building, running, and testing. |
| **`docs`** | [Comprehensive documentation library](docs/). |

---

## 🧩 Project Management

We track project progress, stories, and tasks using a hybrid approach:

- **Code-First Tracking**: High-level epics, stories, and architectural decisions are documented directly in the codebase under the [`.code_hq`](.code_hq) directory. This ensures that documentation lives alongside the code.
- **Task Execution**: Detailed task tracking and sprint planning are managed via **Notion / JIRA**.
- **Agent Handoff**: We use `.code_hq/CONTEXT.md` and `.code_hq/STORIES.md` to maintain context for AI agents and human developers alike.

Check [`.code_hq/PROJECT_MANAGEMENT.md`](.code_hq/PROJECT_MANAGEMENT.md) for a comprehensive overview of the project status.

---

## 📚 Documentation & Resources

### 🚀 Getting Started
- **[⚡ Quick Start (Backend & UIs)](docs/QUICK_START_BACKEND.md)** - **START HERE!** Get everything running in 2 minutes
- **[📄 Cheatsheet](docs/CHEATSHEET.txt)** - Single-page command reference
- **[📊 Startup Flowchart](docs/STARTUP_FLOWCHART.md)** - Visual architecture and flow diagrams

### 🔧 Development
- **[Local Development Guide](docs/LOCAL_DEVELOPMENT.md)** - Complete development workflow
- **[Setup Guide](docs/setup/PLATFORM_SETUP_GUIDE.md)** - Detailed environment setup
- **[Build & Test Guide](docs/BUILD_AND_TEST_GUIDE.md)** - Deep dive into build artifacts and testing
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

### 📖 Reference
- **[Design System](docs/reference/DESIGN_SYSTEM.md)** - UI components and styling
- **[PocketBase Schema](docs/reference/POCKETBASE_SCHEMA.md)** - Database structure
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design and decisions
- **[API Documentation](docs/API.md)** - Backend API reference

### 🚀 Deployment
- **[Deployment Workflow](docs/DEPLOYMENT_WORKFLOW.md)** - Production deployment guide
- **[AWS CDN Setup](docs/AWS_CDN_SETUP.md)** - CDN configuration
- **[Distribution Guide](docs/DISTRIBUTION.md)** - App store and package distribution

---

## 💖 Support the Project

If you find **B-Side** helpful and want to support its development, consider sponsoring or buying me a coffee! Your support helps maintain the project and add new features.

- [**GitHub Sponsors**](https://github.com/sponsors/brentmzey)
- [**Buy Me a Coffee**](https://buymeacoffee.com/brentmzey)

---

> Built with ❤️ by the B-Side Team.