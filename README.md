# B-Side - Kotlin Multiplatform Application

A modern multiplatform application built with Kotlin Multiplatform, Compose Multiplatform, and Ktor.

## 🚀 Quick Start

This project now mirrors the end-to-end target coverage recommended in the [Kotlin Multiplatform quickstart](https://kotlinlang.org/docs/multiplatform/quickstart.html#run-the-sample-apps), so every sample app (Android, iOS, Desktop, JVM Server, Kotlin/JS Web, and experimental Wasm) can be launched from either the provided scripts or Android Studio run configurations.

### Development Scripts

All development scripts are located in `./scripts/` and are executable. See [scripts/README.md](./scripts/README.md) for comprehensive documentation.

#### Building & Running

**Run individual platforms:**
```bash
./scripts/run-desktop.sh    # Fastest iteration - JVM Desktop
./scripts/run-web.sh        # Browser-based web app with hot-reload (Kotlin/JS)
./scripts/run-wasm.sh       # Experimental WebAssembly build from Compose
./scripts/run-android.sh    # Android device/emulator
./scripts/run-ios.sh        # iOS Simulator/device (macOS only)
./scripts/run-server.sh     # Ktor backend server
```

**Build all targets:**
```bash
./scripts/build-all.sh       # Full Gradle build (all platforms)
./scripts/verify-targets.sh  # Quick compilation test (no artifacts)
```

**Launch everything:**
```bash
./scripts/start-all.sh       # Server + Desktop + Web (background)
./scripts/stop-all.sh        # Stop all background processes
```

#### Testing & Debugging

```bash
./scripts/test-full-stack.sh # Integration tests across entire stack
./scripts/test-server-db.sh  # Backend & database tests
```

#### Platform-Specific Notes

- **Desktop** (`run-desktop.sh`) - Fastest feedback loop, best for UI development
- **Web** (`run-web.sh`) - Hot-reload enabled, access at http://localhost:8080
- **Android** (`run-android.sh`) - Requires `adb` and connected device/emulator
- **iOS** (`run-ios.sh`) - Requires Xcode on macOS, opens Simulator
- **Server** (`run-server.sh`) - Auto-builds JAR if missing, supports background mode

All scripts support being run from anywhere in the project when using direnv (see below).

### Using direnv (Optional but Recommended)

For automatic PATH setup when entering the project directory:

```bash
# Install direnv
brew install direnv  # macOS
# or apt install direnv  # Linux

# Enable it in your shell
echo 'eval "$(direnv hook bash)"' >> ~/.bashrc   # for Bash
# or
echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc     # for Zsh

# Reload shell config
source ~/.bashrc  # or source ~/.zshrc

# Allow project (from project root)
cd /path/to/bside
direnv allow
```

Now all scripts are available without the `./scripts/` prefix:
```bash
run-desktop.sh      # Instead of ./scripts/run-desktop.sh
build-all.sh        # Instead of ./scripts/build-all.sh
verify-targets.sh   # Instead of ./scripts/verify-targets.sh
```

**Without direnv:** Simply run scripts with `./scripts/script-name.sh` from project root.

## 💰 Cost Optimization

**GitHub Actions have been disabled** to reduce costs. Workflow files are archived in `./docs/disabled-github-actions/`.

**Development workflow:**
1. Use `./scripts/verify-targets.sh` to test builds locally before pushing
2. Run `./scripts/test-full-stack.sh` for integration testing
3. Develop with platform-specific scripts (e.g., `run-desktop.sh`) to avoid full rebuilds
4. Only enable GitHub Actions when needed for releases

**Time savings:** Building a single target takes 30-90 seconds vs 5-10+ minutes for all targets.

## 📚 Documentation

- **[scripts/README.md](./scripts/README.md)** - Comprehensive guide to all development scripts and workflows
- **[docs/GRADLE_BUILD_ROADMAP.md](./docs/GRADLE_BUILD_ROADMAP.md)** - Roadmap to `gradle build` success
- **[docs/DESIGN_SYSTEM.md](./docs/DESIGN_SYSTEM.md)** - UI/UX design guidelines
- **[docs/SHARED_TYPES_GUIDE.md](./docs/SHARED_TYPES_GUIDE.md)** - Cross-platform type safety
- **[docs/POCKETBASE_SCHEMA.md](./docs/POCKETBASE_SCHEMA.md)** - Database schema
- **[docs/BUILD_STATUS.md](./docs/BUILD_STATUS.md)** - CI/CD configuration (archived)
- **[docs/disabled-github-actions/](./docs/disabled-github-actions/)** - Archived CI workflows

## 🏗️ Project Structure

```
bside/
├── composeApp/          # Compose Multiplatform UI (Android, iOS, Desktop, Web)
├── server/              # Ktor backend server
├── shared/              # Shared Kotlin code across all platforms
├── iosApp/              # iOS-specific wrapper (Swift/SwiftUI)
├── pocketbase/          # PocketBase database
│
├── scripts/             # 🔧 Development & build scripts (executable)
│   ├── README.md        # Comprehensive script documentation
│   ├── run-*.sh         # Platform-specific runners
│   ├── build-all.sh     # Full Gradle build
│   ├── start-all.sh     # Launch all services
│   ├── stop-all.sh      # Stop all services
│   ├── verify-targets.sh # Build verification
│   └── test-*.sh        # Testing utilities
│
├── docs/                # 📚 Project documentation
│   ├── BUILD_STATUS.md        # CI/CD configuration
│   ├── DESIGN_SYSTEM.md       # UI/UX guidelines
│   ├── POCKETBASE_SCHEMA.md   # Database schema
│   ├── SHARED_TYPES_GUIDE.md  # Type safety guide
│   └── disabled-github-actions/ # Archived CI workflows
│
├── .envrc               # direnv config (auto-adds scripts to PATH)
├── .sdkmanrc            # SDKMAN Java version management
├── README.md            # This file
└── build.gradle.kts     # Root Gradle configuration
```

## 🎯 Supported Platforms

- ✅ Android (Phone, Tablet)
- ✅ iOS (iPhone, iPad)
- ✅ Desktop (macOS, Windows, Linux via JVM)
- ✅ Web (Kotlin/JS in browser)
- ✅ WebAssembly (experimental Compose/Wasm target)
- ✅ Backend (Ktor server)

## 🛠️ Requirements

- **Temurin JDK 17 LTS (Adoptium)** - Kotlin compilation + Compose multiplatform tooling (JDK 21 works too)
- **Gradle** - Build tool (wrapper included)
- **Android SDK** - For Android builds
- **Xcode** - For iOS builds (macOS only)
- **Node.js** - For web target (Kotlin/JS)

## ❤️ Proust Questionnaire Experience

- New **Proust** tab inside `composeApp` streams prompts directly from PocketBase using the shared `pocketbase-kt-sdk` client.
- The SDK now defaults to Temurin Java 17 LTS toolchains and automatically falls back to **smart client polling** whenever SSE is blocked, so desktop/mobile/web targets stay in sync.
- Answers are drafted locally and can be reviewed before submission; realtime status (SSE vs polling) is surfaced via Compose chips for quick diagnostics.
- To try it: `./scripts/run-desktop.sh` → switch to the **Proust** tab → begin answering questions; updates propagate live when prompts change in PocketBase.

## 📖 Common Workflows

### UI Development (Fastest Iteration)
```bash
./scripts/run-desktop.sh
# Edit Compose code → auto-recompile → see changes immediately
```

### Web Development with Hot-Reload
```bash
./scripts/run-web.sh
# Access http://localhost:8080
# Changes reload automatically in browser
```

### Mobile Testing

**Android:**
```bash
./scripts/run-android.sh
# Builds and installs on connected device/emulator
```

**iOS (macOS only):**
```bash
./scripts/run-ios.sh
# Opens Xcode project - select simulator and run
```

### Full Stack Development
```bash
# Terminal 1: Backend with live logs
./scripts/run-server.sh

# Terminal 2: Frontend of choice
./scripts/run-desktop.sh
# or
./scripts/run-web.sh

# OR run everything at once (background mode)
./scripts/start-all.sh
# Stop with: ./scripts/stop-all.sh
```

### Pre-Commit Validation
```bash
# Quick: Verify all targets compile (1-2 min)
./scripts/verify-targets.sh

# Thorough: Run full test suite (5-10 min)
./scripts/test-full-stack.sh

# Full build: Create all artifacts
./scripts/build-all.sh
```

### Debugging

**Desktop debugging:** Run with IntelliJ IDEA debugger
- Open project in IDEA
- Set breakpoints in shared code
- Run `composeApp` JVM target with debugger attached

**Web debugging:** Browser DevTools
```bash
./scripts/run-web.sh
# Open http://localhost:8080
# Use browser DevTools (F12) for debugging
```

**Server debugging:**
```bash
# Run server with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 \
     -jar server/build/libs/server-all.jar
# Attach debugger to port 5005
```

## 🏗️ Building with System Gradle

The project is configured to work with your system-installed Gradle (9.2.0+):

```bash
# Full build (all targets + tests)
gradle build

# Fast compile-only build
gradle assemble

# Clean rebuild with tests
gradle clean build
```

**Note:** A plain `gradle build` now covers every Kotlin Multiplatform target, including the production web webpack bundle. See [docs/GRADLE_BUILD_ROADMAP.md](./docs/GRADLE_BUILD_ROADMAP.md) for continuing optimizations.

**All Kotlin Multiplatform compiler backends are supported:**
- ✅ **Android** - ARM & x86 (debug & release APKs)
- ✅ **iOS** - ARM64 device + Simulator frameworks
- ✅ **JVM Desktop** - macOS, Windows, Linux JARs
- ✅ **JavaScript** - Browser-based web application (dev builds)
- ✅ **Server** - Ktor backend (JVM)

The wrapper (`./gradlew`) is synchronized with your system Gradle version for consistency.

## 🔍 Troubleshooting

**Scripts not found?**
```bash
# Option 1: Use direnv
direnv allow

# Option 2: Add to PATH manually
export PATH="$PATH:$(pwd)/scripts"

# Option 3: Run with full path
./scripts/run-desktop.sh
```

**Port conflicts?**
```bash
# Stop all B-Side processes
./scripts/stop-all.sh

# Or manually kill port 8080
lsof -ti:8080 | xargs kill -9
```

**Build failures?**
```bash
# Clean build
gradle clean

# Verify environment  
./scripts/verify-targets.sh

# Build without tests
gradle assemble
```

## 📄 License

See [LICENSE](./LICENSE) file for details.

---

**For detailed script documentation and development workflows, see [scripts/README.md](./scripts/README.md)**
