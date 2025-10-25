#!/bin/bash

echo "🤖 Starting B-Side Android App..."
echo ""

# Check if adb is available
if ! command -v adb &> /dev/null; then
    echo "❌ Error: adb not found. Please install Android SDK Platform Tools"
    exit 1
fi

# Check for connected devices
DEVICES=$(adb devices | grep -v "List" | grep "device" | wc -l)

if [ $DEVICES -eq 0 ]; then
    echo "❌ No Android devices or emulators found"
    echo ""
    echo "Please either:"
    echo "  1. Start an Android emulator from Android Studio"
    echo "  2. Connect a physical device with USB debugging enabled"
    echo ""
    exit 1
fi

echo "Found $DEVICES device(s)"
echo ""
echo "Building and installing app..."

./gradlew :composeApp:installDebug

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ App installed successfully!"
    echo "Opening app on device..."
    adb shell am start -n love.bside.app/love.bside.app.MainActivity
else
    echo ""
    echo "❌ Build failed. Check the output above for errors."
    exit 1
fi
