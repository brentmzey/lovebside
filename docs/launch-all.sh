#!/bin/bash
# Master script to run ALL BSide platforms simultaneously

set -e  # Exit on error

echo "🚀 BSide Multi-Platform Launcher"
echo "================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Create logs directory
mkdir -p logs

echo "Building all targets..."
./gradlew assemble -x test

echo ""
echo "Launching platforms in separate processes..."
echo ""

# 1. iOS Simulator
echo -e "${BLUE}📱 iOS${NC} - Opening Xcode..."
open iosApp/iosApp.xcodeproj 2>>logs/ios.log &
echo "   → Manual: Select iPhone simulator in Xcode → Click Run"

# 2. Android Emulator
echo -e "${BLUE}🤖 Android${NC} - Opening Android Studio..."
open -a "Android Studio" . 2>>logs/android.log &
echo "   → Manual: Select composeApp → emulator → Click Run"

# 3. Desktop App
echo -e "${BLUE}🖥️  Desktop${NC} - Launching JVM app..."
./gradlew :composeApp:run > logs/desktop.log 2>&1 &
DESKTOP_PID=$!
echo "   → PID: $DESKTOP_PID (check logs/desktop.log)"

# 4. Web Browser
echo -e "${BLUE}🌐 Web${NC} - Starting dev server..."
./gradlew :composeApp:jsBrowserDevelopmentRun > logs/web.log 2>&1 &
WEB_PID=$!
echo "   → PID: $WEB_PID (will open in browser)"

# 5. Backend Server
echo -e "${BLUE}🔧 Server${NC} - Starting Ktor server..."
./gradlew :server:run > logs/server.log 2>&1 &
SERVER_PID=$!
echo "   → PID: $SERVER_PID (http://localhost:8080)"

echo ""
echo "⏳ Waiting for services to start..."
sleep 5

# Check what's running
echo ""
echo -e "${GREEN}✅ Active Processes:${NC}"
ps aux | grep -E "(composeApp:run|jsBrowserDevelopmentRun|server:run)" | grep -v grep | awk '{print "   - "$2, $11, $12, $13}'

echo ""
echo -e "${YELLOW}📋 Next Steps:${NC}"
echo "   1. iOS: In Xcode → Select simulator → Click Run ▶"
echo "   2. Android: In Android Studio → Click Run ▶"
echo "   3. Desktop: Window should appear automatically"
echo "   4. Web: Browser should open to http://localhost:8080"
echo "   5. Server: API at http://localhost:8080"
echo ""
echo -e "${GREEN}🎯 All platforms use backend: https://bside.pockethost.io${NC}"
echo ""
echo "To stop all: pkill -f 'gradle.*run'"
