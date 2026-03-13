#!/bin/bash
# run-ios-cli.sh - Build and Run iOS app on Simulator (CLI only)

set -e

cd "$(dirname "$0")/.." || exit

APP_NAME="love.bside.app"
BUNDLE_ID="love.bside.app"
SIMULATOR_NAME="iPhone 15"

echo "🍎 Preparing to run iOS app on Simulator..."

# 1. Find Booted Simulator
BOOTED_UUID=$(xcrun simctl list devices | grep "(Booted)" | grep "iPhone" | head -1 | grep -oE "[0-9A-F-]{36}")

if [ -z "$BOOTED_UUID" ]; then
    echo "⚠️  No booted iPhone simulator found."
    echo "Attempting to boot $SIMULATOR_NAME..."
    
    # Check if Simulator exists
    if ! xcrun simctl list devices | grep -q "$SIMULATOR_NAME"; then
        echo "❌ Simulator '$SIMULATOR_NAME' not found."
        echo "Available simulators:"
        xcrun simctl list devices | grep "iPhone"
        exit 1
    fi
    
    xcrun simctl boot "$SIMULATOR_NAME"
    
    # Wait for boot
    echo "Waiting for simulator to boot..."
    xcrun simctl bootstatus "$SIMULATOR_NAME"
    
    BOOTED_UUID=$(xcrun simctl list devices | grep "(Booted)" | grep "iPhone" | head -1 | grep -oE "[0-9A-F-]{36}")
fi

echo "✅ Using Simulator: $BOOTED_UUID"

# 2. Build App using Gradle
# This builds the .app bundle. 
# Note: KMP standard task for simulator build is usually linkDebugFrameworkIosSimulatorArm64
# But that produces a framework, not a .app. 
# To get a .app, we usually need Xcode project build.

echo "🏗️  Building iOS App via Xcode..."
# Check if xcworkspace exists
WORKSPACE="iosApp/iosApp.xcworkspace"
SCHEME="iosApp"

if [ ! -d "$WORKSPACE" ]; then
    echo "❌ Workspace not found at $WORKSPACE"
    exit 1
fi

# Build .app
xcodebuild -workspace "$WORKSPACE" \
    -scheme "$SCHEME" \
    -configuration Debug \
    -destination "platform=iOS Simulator,id=$BOOTED_UUID" \
    -derivedDataPath build/ios_build \
    quiet

APP_PATH=$(find build/ios_build -name "*.app" | head -1)

if [ -z "$APP_PATH" ]; then
    echo "❌ Build failed: No .app found."
    exit 1
fi

echo "✅ Build successful: $APP_PATH"

# 3. Install and Launch
echo "📲 Installing app..."
xcrun simctl install "$BOOTED_UUID" "$APP_PATH"

echo "🚀 Launching app..."
xcrun simctl launch "$BOOTED_UUID" "$BUNDLE_ID"

echo "✅ iOS App launched!"
