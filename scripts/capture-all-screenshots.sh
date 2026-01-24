#!/usr/bin/env bash
# Capture screenshots for all platforms
# Usage: ./scripts/capture-all-screenshots.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
SCREENSHOT_DIR="$ROOT_DIR/docs/screenshots"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}📸 Screenshot Capture Workflow${NC}"
echo ""

# Create directories
echo "📁 Creating screenshot directories..."
mkdir -p "$SCREENSHOT_DIR"/{android,ios,web,desktop}/{chat,reactions,profile,settings,auth}
mkdir -p "$SCREENSHOT_DIR"/baselines
echo -e "${GREEN}✓ Directories created${NC}"
echo ""

# Android
echo -e "${BLUE}1. Android Screenshots${NC}"
if command -v adb &> /dev/null && adb devices | grep -q "device$"; then
  echo "   Device found! Ready to capture."
  echo "   Run: ./scripts/screenshot-android.sh docs/screenshots/android/chat/main.png"
else
  echo -e "   ${YELLOW}⚠ No Android device connected${NC}"
  echo "   Skip or connect device and run: adb devices"
fi
echo ""

# iOS
echo -e "${BLUE}2. iOS Screenshots${NC}"
if [[ "$OSTYPE" == "darwin"* ]]; then
  if xcrun simctl list devices | grep -q "Booted"; then
    echo "   Simulator running! Ready to capture."
    echo "   Run: ./scripts/screenshot-ios.sh docs/screenshots/ios/chat/main.png"
  else
    echo -e "   ${YELLOW}⚠ No iOS simulator running${NC}"
    echo "   Start simulator: open -a Simulator"
  fi
else
  echo "   ⊘ iOS only available on macOS"
fi
echo ""

# Desktop
echo -e "${BLUE}3. Desktop Screenshots${NC}"
echo "   Start app: ./gradlew :composeApp:run"
echo "   Capture:"
if [[ "$OSTYPE" == "darwin"* ]]; then
  echo "     macOS: ⌘⇧4, then Space, click window"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
  echo "     Linux: gnome-screenshot -w -f screenshot.png"
else
  echo "     Windows: Win+Shift+S"
fi
echo ""

# Web
echo -e "${BLUE}4. Web Screenshots${NC}"
echo "   Start server: ./gradlew :composeApp:jsBrowserDevelopmentRun"
echo "   Open: http://localhost:8080"
echo "   Capture: F12 → Ctrl+Shift+P → 'screenshot'"
echo ""

# Summary
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}📸 Screenshot directories ready!${NC}"
echo -e "${GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo "📂 Location: $SCREENSHOT_DIR"
echo ""
echo "💡 Tips:"
echo "  • Name files descriptively: conversation-list.png, message-compose.png"
echo "  • Capture light and dark modes if applicable"
echo "  • Include error states and edge cases"
echo "  • Use consistent aspect ratios per platform"
echo ""
echo "After capturing, optimize images:"
echo "  find docs/screenshots -name '*.png' -exec optipng {} \\;"
echo ""
