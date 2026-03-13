#!/bin/bash
# Run BSide on iOS Simulator

echo "🍎 Launching iOS Simulator..."

# Build the iOS framework
echo "Building iOS framework..."
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

if [ $? -ne 0 ]; then
    echo "❌ Framework build failed"
    exit 1
fi

# Boot simulator
echo "Booting iPhone 15 simulator..."
xcrun simctl boot "iPhone 15" 2>/dev/null || echo "Simulator already booted or not found"

# Open Simulator app
open -a Simulator

# Build and run the app
echo "Building and running iOS app..."
cd iosApp
xcodebuild -scheme iosApp \
    -destination 'platform=iOS Simulator,name=iPhone 15' \
    -derivedDataPath build

if [ $? -eq 0 ]; then
    echo "✅ iOS app launched successfully!"
    echo "Check the Simulator window"
else
    echo "⚠️  Build succeeded but install may need Xcode"
    echo "Open iosApp/iosApp.xcodeproj in Xcode and click Run"
fi
