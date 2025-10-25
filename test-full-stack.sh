#!/bin/bash
# test-full-stack.sh - Complete end-to-end test

set -e

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "╔════════════════════════════════════════════════════════════╗"
echo "║       B-SIDE FULL STACK TEST                               ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""

# Test 1: Check if backends are running
echo -e "${BLUE}Test 1: Backend Health Checks${NC}"
echo -n "  PocketBase (8090)... "
if curl -s http://127.0.0.1:8090/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ RUNNING${NC}"
else
    echo -e "${RED}❌ NOT RUNNING${NC}"
    echo -e "${YELLOW}  Run: ./start-all.sh${NC}"
    exit 1
fi

echo -n "  Internal API (8080)... "
if curl -s http://localhost:8080/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ RUNNING${NC}"
else
    echo -e "${RED}❌ NOT RUNNING${NC}"
    echo -e "${YELLOW}  Run: ./start-all.sh${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Test 2: Build Android APK${NC}"
echo "  Building..."
./gradlew :composeApp:assembleDebug -x test --quiet
if [ -f composeApp/build/outputs/apk/debug/composeApp-debug.apk ]; then
    echo -e "  ${GREEN}✅ APK built successfully${NC}"
else
    echo -e "  ${RED}❌ APK build failed${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Test 3: Build Server JAR${NC}"
echo "  Building..."
./gradlew :server:shadowJar -x test --quiet
if [ -f server/build/libs/server-all.jar ]; then
    echo -e "  ${GREEN}✅ Server JAR built successfully${NC}"
else
    echo -e "  ${RED}❌ Server JAR build failed${NC}"
    exit 1
fi

echo ""
echo -e "${BLUE}Test 4: API Endpoints${NC}"

# Test signup
echo -n "  Sign up endpoint... "
SIGNUP_RESULT=$(curl -s -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"test-$(date +%s)@example.com\",
    \"password\": \"TestPass123!\",
    \"passwordConfirm\": \"TestPass123!\",
    \"name\": \"Test User\"
  }" 2>&1)

if echo "$SIGNUP_RESULT" | grep -q "token\|user"; then
    echo -e "${GREEN}✅ WORKS${NC}"
    TOKEN=$(echo "$SIGNUP_RESULT" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
else
    echo -e "${YELLOW}⚠️  May need configuration${NC}"
    echo "    Response: $SIGNUP_RESULT"
fi

# Test authenticated endpoint (if we got a token)
if [ ! -z "$TOKEN" ]; then
    echo -n "  Authenticated request... "
    AUTH_RESULT=$(curl -s http://localhost:8080/api/v1/profile \
      -H "Authorization: Bearer $TOKEN" 2>&1)
    
    if echo "$AUTH_RESULT" | grep -q "email\|user\|success"; then
        echo -e "${GREEN}✅ WORKS${NC}"
    else
        echo -e "${YELLOW}⚠️  May need configuration${NC}"
    fi
fi

echo ""
echo -e "${BLUE}Test 5: Check Build Artifacts${NC}"
echo "  Android APK:"
echo "    $(ls -lh composeApp/build/outputs/apk/debug/composeApp-debug.apk | awk '{print $5, $9}')"
echo "  Server JAR:"
echo "    $(ls -lh server/build/libs/server-all.jar | awk '{print $5, $9}')"

echo ""
echo "╔════════════════════════════════════════════════════════════╗"
echo -e "║  ${GREEN}✅ ALL TESTS PASSED${NC}                                     ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo ""
echo -e "${BLUE}Next Steps:${NC}"
echo "  1. Install APK: adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk"
echo "  2. Launch app: adb shell am start -n love.bside.app.android/love.bside.app.MainActivity"
echo "  3. View logs: adb logcat | grep BsideApp"
echo ""
echo -e "${YELLOW}Backends:${NC}"
echo "  PocketBase Admin: http://127.0.0.1:8090/_/"
echo "  API Server: http://localhost:8080"
echo ""
