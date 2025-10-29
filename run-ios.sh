#!/bin/bash

echo "🍎 Starting B-Side iOS App..."
echo ""

# Check if we're on macOS
if [[ "$OSTYPE" != "darwin"* ]]; then
    echo "❌ Error: iOS development requires macOS"
    exit 1
fi

# Check if Xcode is installed
if ! command -v xcodebuild &> /dev/null; then
    echo "❌ Error: Xcode not found. Please install Xcode from the App Store"
    exit 1
fi

echo "Opening Xcode workspace..."
echo ""
echo "In Xcode:"
echo "  1. Select a simulator (e.g., iPhone 15)"
echo "  2. Click the Run button (▶️)"
echo ""

# Try to open the workspace
if [ -e "iosApp/iosApp.xcworkspace" ]; then
    open iosApp/iosApp.xcworkspace
elif [ -e "iosApp/iosApp.xcodeproj" ]; then
    open iosApp/iosApp.xcodeproj
else
    echo "⚠️  iOS project files not found. Building framework first..."
    ./gradlew :composeApp:iosSimulatorArm64MainBinaries
    
    if [ -e "iosApp/iosApp.xcworkspace" ]; then
        open iosApp/iosApp.xcworkspace
    elif [ -e "iosApp/iosApp.xcodeproj" ]; then
        open iosApp/iosApp.xcodeproj
    else
        echo "❌ Could not find or create iOS project"
        exit 1
    fi
fi
