#!/bin/bash

cd "$(dirname "$0")/.." || exit

echo "🔍 Verifying Kotlin Multiplatform Targets..."
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Track results
PASSED=0
FAILED=0

test_target() {
    local name=$1
    local command=$2
    
    echo -n "Testing $name... "
    
    if $command > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PASSED${NC}"
        PASSED=$((PASSED + 1))
    else
        echo -e "${RED}❌ FAILED${NC}"
        FAILED=$((FAILED + 1))
    fi
}

echo "Building individual targets..."
echo ""

test_target "Android Debug APK    " "./gradlew :composeApp:assembleDebug --quiet"
test_target "Desktop JVM JAR      " "./gradlew :composeApp:jvmJar --quiet"
test_target "JavaScript (Web)     " "./gradlew :composeApp:compileKotlinJs --quiet"
test_target "iOS Simulator Arm64  " "./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64 --quiet"
test_target "iOS Device Arm64     " "./gradlew :composeApp:linkDebugFrameworkIosArm64 --quiet"
test_target "PocketBase SDK       " "./gradlew :pocketbase-kt-sdk:assemble --quiet"
test_target "Server (Ktor API)    " "./gradlew :server:installDist --quiet"

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Results: ${GREEN}$PASSED passed${NC}, ${RED}$FAILED failed${NC}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 All Kotlin Multiplatform targets are ready!${NC}"
    echo ""
    echo "To run the apps:"
    echo "  • Android:  ./run-android.sh"
    echo "  • Desktop:  ./run-desktop.sh"
    echo "  • Web:      ./run-web.sh"
    echo "  • iOS:      ./run-ios.sh"
    exit 0
else
    echo -e "${RED}⚠️  Some targets failed. Check the build output above.${NC}"
    exit 1
fi
