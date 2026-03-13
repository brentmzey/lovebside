#!/usr/bin/env bash
# Platform Verification & Setup Script for B-Side
# Checks all requirements and helps you set up each target platform

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$PROJECT_ROOT"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Emoji support
CHECK="✅"
CROSS="❌"
WARN="⚠️"
INFO="ℹ️"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  B-Side Platform Verification"
echo "  Checking all target platforms on your M4 Pro Mac"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

ALL_PLATFORMS_READY=true

# ============================================================================
# 1. COMMON REQUIREMENTS
# ============================================================================

echo "━━━ Common Requirements ━━━"
echo ""

# Java
echo -n "Java 17+: "
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    if [ "$JAVA_VERSION" -ge 17 ]; then
        echo -e "${GREEN}${CHECK} Found Java $JAVA_VERSION${NC}"
        java -version 2>&1 | head -n 1 | sed 's/^/  /'
    else
        echo -e "${RED}${CROSS} Found Java $JAVA_VERSION (need 17+)${NC}"
        echo "  Fix: Install via SDKMAN or Temurin:"
        echo "       curl -s https://get.sdkman.io | bash"
        echo "       sdk install java 17.0.9-tem"
        ALL_PLATFORMS_READY=false
    fi
else
    echo -e "${RED}${CROSS} Not found${NC}"
    echo "  Fix: Install via SDKMAN: curl -s https://get.sdkman.io | bash"
    ALL_PLATFORMS_READY=false
fi

# Gradle
echo -n "Gradle: "
if [ -f "./gradlew" ]; then
    GRADLE_VERSION=$(./gradlew --version 2>/dev/null | grep "Gradle" | cut -d' ' -f2)
    echo -e "${GREEN}${CHECK} Found Gradle $GRADLE_VERSION (via wrapper)${NC}"
else
    echo -e "${RED}${CROSS} Gradle wrapper not found${NC}"
    ALL_PLATFORMS_READY=false
fi

# Node.js (for web targets)
echo -n "Node.js 18+: "
if command -v node &> /dev/null; then
    NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
    if [ "$NODE_VERSION" -ge 18 ]; then
        echo -e "${GREEN}${CHECK} Found Node $(node -v)${NC}"
    else
        echo -e "${YELLOW}${WARN} Found Node v$NODE_VERSION (need 18+)${NC}"
        echo "  Fix: Update Node via nvm or brew"
        echo "       brew upgrade node"
    fi
else
    echo -e "${YELLOW}${WARN} Not found (needed for Web targets)${NC}"
    echo "  Fix: brew install node"
fi

echo ""

# ============================================================================
# 2. DESKTOP (JVM)
# ============================================================================

echo "━━━ Desktop Target (JVM) ━━━"
echo ""

echo -n "Desktop readiness: "
if command -v java &> /dev/null; then
    echo -e "${GREEN}${CHECK} Ready${NC}"
    echo "  Run: ./scripts/run-desktop.sh"
    echo "  Or:  ./gradlew :composeApp:run"
else
    echo -e "${RED}${CROSS} Java required${NC}"
    ALL_PLATFORMS_READY=false
fi

echo ""

# ============================================================================
# 3. ANDROID
# ============================================================================

echo "━━━ Android Target ━━━"
echo ""

# Android SDK
echo -n "Android SDK: "
if [ -n "$ANDROID_HOME" ] || [ -n "$ANDROID_SDK_ROOT" ]; then
    SDK_PATH="${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
    echo -e "${GREEN}${CHECK} Found at $SDK_PATH${NC}"
    
    # Check for platform tools
    if [ -f "$SDK_PATH/platform-tools/adb" ]; then
        ADB_VERSION=$("$SDK_PATH/platform-tools/adb" version 2>&1 | head -n 1)
        echo "  $ADB_VERSION"
    fi
else
    echo -e "${YELLOW}${WARN} ANDROID_HOME not set${NC}"
    echo "  Fix: Install Android Studio, then add to ~/.zshrc or ~/.bashrc:"
    echo "       export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "       export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
fi

# ADB
echo -n "ADB (Android Debug Bridge): "
if command -v adb &> /dev/null; then
    echo -e "${GREEN}${CHECK} Found${NC}"
    
    # Check for connected devices
    DEVICES=$(adb devices | grep -v "List of devices" | grep "device$" | wc -l | xargs)
    if [ "$DEVICES" -gt 0 ]; then
        echo -e "${GREEN}  ${CHECK} $DEVICES device(s) connected${NC}"
        adb devices | grep "device$" | sed 's/^/    /'
    else
        echo -e "${YELLOW}  ${WARN} No devices/emulators connected${NC}"
        echo "      Start emulator: Android Studio → Device Manager → Run"
        echo "      Or connect device via USB with USB debugging enabled"
    fi
else
    echo -e "${YELLOW}${WARN} Not found${NC}"
    echo "  Fix: Add to PATH: export PATH=\$PATH:\$ANDROID_HOME/platform-tools"
fi

echo ""
echo "  Build Android:"
echo "    ./gradlew :composeApp:assembleDebug"
echo "  Install on device:"
echo "    ./scripts/run-android.sh"
echo "    # or: ./gradlew :composeApp:installDebug"
echo ""

# ============================================================================
# 4. iOS
# ============================================================================

echo "━━━ iOS Target (macOS only) ━━━"
echo ""

if [ "$(uname)" = "Darwin" ]; then
    # Xcode
    echo -n "Xcode: "
    if command -v xcodebuild &> /dev/null; then
        XCODE_VERSION=$(xcodebuild -version | head -n 1)
        echo -e "${GREEN}${CHECK} $XCODE_VERSION${NC}"
        xcodebuild -version | tail -n +2 | sed 's/^/  /'
        
        # Xcode command line tools
        if xcode-select -p &> /dev/null; then
            echo -e "${GREEN}  ${CHECK} Command Line Tools installed${NC}"
        else
            echo -e "${YELLOW}  ${WARN} Command Line Tools not installed${NC}"
            echo "      Fix: xcode-select --install"
        fi
    else
        echo -e "${YELLOW}${WARN} Not found${NC}"
        echo "  Fix: Install from Mac App Store"
        echo "       https://apps.apple.com/app/xcode/id497799835"
    fi
    
    # CocoaPods
    echo -n "CocoaPods: "
    if command -v pod &> /dev/null; then
        POD_VERSION=$(pod --version 2>/dev/null || echo "installed but broken")
        if [ "$POD_VERSION" != "installed but broken" ]; then
            echo -e "${GREEN}${CHECK} Found v$POD_VERSION${NC}"
        else
            echo -e "${YELLOW}${WARN} Installed but may need reinstall${NC}"
            echo "  Fix: sudo gem install cocoapods"
        fi
    else
        echo -e "${YELLOW}${WARN} Not found (may be needed for iOS dependencies)${NC}"
        echo "  Fix: sudo gem install cocoapods"
    fi
    
    # iOS Simulators
    echo -n "iOS Simulators: "
    if command -v xcrun &> /dev/null; then
        SIM_COUNT=$(xcrun simctl list devices available | grep "iPhone" | wc -l | xargs)
        if [ "$SIM_COUNT" -gt 0 ]; then
            echo -e "${GREEN}${CHECK} Found $SIM_COUNT available${NC}"
            echo "  List all: xcrun simctl list devices | grep iPhone"
        else
            echo -e "${YELLOW}${WARN} No iPhone simulators found${NC}"
            echo "  Fix: Xcode → Settings → Platforms → Download iOS Simulator"
        fi
    fi
    
    echo ""
    echo "  Build iOS:"
    echo "    cd iosApp && xcodebuild -workspace iosApp.xcworkspace -scheme iosApp"
    echo "  Run on simulator:"
    echo "    ./scripts/run-ios.sh"
    echo "    # or open iosApp/iosApp.xcworkspace in Xcode and run"
    echo ""
else
    echo -e "${YELLOW}${INFO} iOS builds require macOS${NC}"
    echo ""
fi

# ============================================================================
# 5. WEB (Kotlin/JS & Wasm)
# ============================================================================

echo "━━━ Web Targets (Kotlin/JS & Wasm) ━━━"
echo ""

echo -n "Web readiness: "
if command -v node &> /dev/null && command -v java &> /dev/null; then
    echo -e "${GREEN}${CHECK} Ready${NC}"
    echo "  Run JS version:   ./scripts/run-web.sh"
    echo "  Or manually:      ./gradlew :composeApp:wasmJsBrowserDevelopmentRun"
    echo "  Access at:        http://localhost:8080"
else
    echo -e "${YELLOW}${WARN} Needs Node.js and Java${NC}"
fi

echo ""

# ============================================================================
# 6. BACKEND SERVER
# ============================================================================

echo "━━━ Backend Server (Ktor) ━━━"
echo ""

echo -n "Server readiness: "
if command -v java &> /dev/null; then
    echo -e "${GREEN}${CHECK} Ready${NC}"
    echo "  Run server:       ./scripts/run-server.sh"
    echo "  Or manually:      ./gradlew :server:run"
    echo "  API endpoint:     http://localhost:8081"
else
    echo -e "${RED}${CROSS} Java required${NC}"
fi

# PocketBase
echo -n "PocketBase: "
if [ -f "pocketbase/pocketbase" ]; then
    echo -e "${GREEN}${CHECK} Found${NC}"
    echo "  Run PocketBase:   cd pocketbase && ./pocketbase serve"
    echo "  Admin UI:         http://127.0.0.1:8090/_/"
    echo "  API:              http://127.0.0.1:8090/api/"
else
    echo -e "${YELLOW}${WARN} Binary not found${NC}"
    echo "  Download: https://pocketbase.io/docs/"
fi

echo ""

# ============================================================================
# 7. VERIFICATION COMMANDS
# ============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Verification Commands"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "Quick compile check (1-2 min):"
echo "  ./scripts/verify-targets.sh"
echo ""

echo "Test individual platforms:"
echo "  ./gradlew :shared:compileKotlinJvm          # Desktop/Server"
echo "  ./gradlew :composeApp:compileDebugKotlinAndroid  # Android"
echo "  ./gradlew :composeApp:compileKotlinJs       # Web (JS)"
if [ "$(uname)" = "Darwin" ]; then
    echo "  ./gradlew :shared:compileKotlinIosSimulatorArm64  # iOS (M-series Mac)"
fi
echo ""

echo "Full build (5-10 min first time):"
echo "  ./gradlew build"
echo ""

echo "Run all tests:"
echo "  ./gradlew test"
echo ""

# ============================================================================
# 8. SETUP RECOMMENDATIONS
# ============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Setup Recommendations for M4 Pro"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "1. Enable parallel builds (fast on M4 Pro):"
echo "   echo 'org.gradle.parallel=true' >> gradle.properties"
echo "   echo 'org.gradle.workers.max=8' >> gradle.properties"
echo ""

echo "2. Use Gradle daemon:"
echo "   echo 'org.gradle.daemon=true' >> gradle.properties"
echo ""

echo "3. Set up direnv (optional but convenient):"
echo "   brew install direnv"
echo "   echo 'eval \"\$(direnv hook zsh)\"' >> ~/.zshrc"
echo "   direnv allow"
echo ""

echo "4. Install ktlint for code formatting:"
echo "   brew install ktlint"
echo ""

# ============================================================================
# 9. PLATFORM-SPECIFIC INSTRUCTIONS
# ============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Platform-Specific Setup"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "${YELLOW}Android Setup Needed:${NC}"
    echo "  1. Download Android Studio: https://developer.android.com/studio"
    echo "  2. Install Android SDK via Android Studio"
    echo "  3. Add to ~/.zshrc or ~/.bashrc:"
    echo "       export ANDROID_HOME=\$HOME/Library/Android/sdk"
    echo "       export PATH=\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/emulator"
    echo "  4. Reload shell: source ~/.zshrc"
    echo "  5. Create/start an emulator in Android Studio → Device Manager"
    echo ""
fi

if [ "$(uname)" = "Darwin" ] && ! command -v xcodebuild &> /dev/null; then
    echo "${YELLOW}iOS Setup Needed:${NC}"
    echo "  1. Install Xcode from Mac App Store"
    echo "  2. Install Command Line Tools: xcode-select --install"
    echo "  3. Accept license: sudo xcodebuild -license accept"
    echo "  4. Install CocoaPods: sudo gem install cocoapods"
    echo "  5. Set up iOS simulator in Xcode → Settings → Platforms"
    echo ""
fi

if ! command -v node &> /dev/null; then
    echo "${YELLOW}Web Setup Needed:${NC}"
    echo "  Install Node.js 18+:"
    echo "    brew install node"
    echo "  Or use nvm for version management:"
    echo "    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.0/install.sh | bash"
    echo "    nvm install 20"
    echo ""
fi

# ============================================================================
# 10. FINAL STATUS
# ============================================================================

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "  Summary"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

READY_PLATFORMS=()
NEEDS_SETUP=()

# Desktop
if command -v java &> /dev/null; then
    READY_PLATFORMS+=("Desktop")
else
    NEEDS_SETUP+=("Desktop")
fi

# Android
if [ -n "$ANDROID_HOME" ] || [ -n "$ANDROID_SDK_ROOT" ]; then
    if command -v adb &> /dev/null; then
        READY_PLATFORMS+=("Android")
    else
        NEEDS_SETUP+=("Android (ADB)")
    fi
else
    NEEDS_SETUP+=("Android")
fi

# iOS
if [ "$(uname)" = "Darwin" ]; then
    if command -v xcodebuild &> /dev/null; then
        READY_PLATFORMS+=("iOS")
    else
        NEEDS_SETUP+=("iOS")
    fi
fi

# Web
if command -v node &> /dev/null && command -v java &> /dev/null; then
    READY_PLATFORMS+=("Web")
else
    NEEDS_SETUP+=("Web")
fi

# Server
if command -v java &> /dev/null; then
    READY_PLATFORMS+=("Server")
else
    NEEDS_SETUP+=("Server")
fi

echo "Ready Platforms (${#READY_PLATFORMS[@]}):"
for platform in "${READY_PLATFORMS[@]}"; do
    echo -e "  ${GREEN}${CHECK} $platform${NC}"
done

echo ""

if [ ${#NEEDS_SETUP[@]} -gt 0 ]; then
    echo "Needs Setup (${#NEEDS_SETUP[@]}):"
    for platform in "${NEEDS_SETUP[@]}"; do
        echo -e "  ${YELLOW}${WARN} $platform${NC}"
    done
    echo ""
    echo "See instructions above for each platform."
else
    echo -e "${GREEN}${CHECK} All platforms ready!${NC}"
fi

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

echo "Next steps:"
echo "  1. Fix any missing requirements listed above"
echo "  2. Run quick verification: ./scripts/verify-targets.sh"
echo "  3. Start developing: ./scripts/run-desktop.sh"
echo ""

if [ "$ALL_PLATFORMS_READY" = false ]; then
    exit 1
fi
