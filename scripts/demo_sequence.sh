#!/bin/bash
# demo_sequence.sh - Orchestrate the full B-Side Demo
# Usage: ./scripts/demo_sequence.sh

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
BOLD='\033[1m'
NC='\033[0m'

echo -e "${BOLD}${BLUE}🎬 B-Side Demo Sequence Initiated${NC}"
echo "========================================"

# 1. Backend Check
echo -e "\n${BOLD}1. Checking Backend Services...${NC}"
if curl -s http://localhost:8092/api/health > /dev/null; then
    echo -e "${GREEN}✅ Backend is running.${NC}"
else
    echo -e "${YELLOW}⚠️  Backend not running. Starting...${NC}"
    ./scripts/backend-start.sh
fi

# 2. Android Emulator Check
echo -e "\n${BOLD}2. Checking Android Environment...${NC}"
if adb devices | grep -q "device$"; then
    echo -e "${GREEN}✅ Android device/emulator connected.${NC}"
else
    echo -e "${YELLOW}⚠️  No Android device found.${NC}"
    echo "Attempting to start 'Pixel_5_API_34'..."
    # Try to start in background
    emulator -avd Pixel_5_API_34 &
    echo "Waiting for device..."
    adb wait-for-device
    echo -e "${GREEN}✅ Android Emulator started.${NC}"
fi

# 3. iOS Simulator Check
echo -e "\n${BOLD}3. Checking iOS Environment...${NC}"
if xcrun simctl list devices | grep "(Booted)" | grep -q "iPhone"; then
    echo -e "${GREEN}✅ iOS Simulator is running.${NC}"
else
    echo -e "${YELLOW}⚠️  No iOS Simulator running.${NC}"
    echo "Booting iPhone 15..."
    xcrun simctl boot "iPhone 15" || xcrun simctl boot "iPhone 15 Pro"
    echo -e "${GREEN}✅ iOS Simulator booted.${NC}"
fi

# 4. Launch Clients
echo -e "\n${BOLD}4. Launching Clients...${NC}"

# Web
echo -e "${BLUE}➡️  Launching Web Client...${NC}"
./scripts/run-web.sh --background

# Desktop
echo -e "${BLUE}➡️  Launching Desktop Client...${NC}"
./scripts/run-desktop.sh --background

# Android
echo -e "${BLUE}➡️  Launching Android Client...${NC}"
./scripts/run-android.sh

# iOS
echo -e "${BLUE}➡️  Launching iOS Client...${NC}"
./scripts/run-ios-cli.sh

echo "========================================"
echo -e "${GREEN}${BOLD}🎉 DEMO SEQUENCE COMPLETE${NC}"
echo "========================================"
echo "All apps should be running."
echo ""
echo "📝 Commands for Screenshots:"
echo "  ./scripts/screenshot-android.sh docs/screenshots/demo_android.png"
echo "  ./scripts/screenshot-ios.sh docs/screenshots/demo_ios.png"
echo ""
echo "Press Enter to stop all background processes (Web/Desktop)..."
read

echo "Stopping..."
./scripts/stop-all.sh
echo "Done."
