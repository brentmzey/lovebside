#!/bin/bash
# Run Android only

echo "🤖 Building and launching Android..."

# Build APK
./gradlew :composeApp:assembleDebug

# Check for adb
if ! command -v adb &> /dev/null; then
    echo "Installing adb..."
    brew install android-platform-tools
fi

# Use Android Studio (recommended)
echo "Opening Android Studio..."
open -a "Android Studio" .

echo ""
echo "In Android Studio:"
echo "  1. Wait for Gradle sync"
echo "  2. Select 'composeApp' configuration"
echo "  3. Select Android emulator"
echo "  4. Click ▶ Run"
echo ""
echo "OR install manually:"
echo "  adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk"
echo "  adb shell am start -n love.bside.app/.MainActivity"
