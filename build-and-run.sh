#!/bin/bash
# Simple BSide Build & Run Script

echo "🚀 Building BSide for all platforms..."

# Build everything (skip tests for speed)
./gradlew assemble -x test

if [ $? -eq 0 ]; then
    echo "✅ Build successful!"
    echo ""
    echo "📱 Launch commands:"
    echo ""
    echo "  Android:  ./gradlew :composeApp:installDebug && adb shell am start -n love.bside.app/.MainActivity"
    echo "  iOS:      open iosApp/iosApp.xcodeproj"
    echo "  Desktop:  ./gradlew :composeApp:run"
    echo "  Server:   ./gradlew :server:run"
    echo ""
else
    echo "❌ Build failed"
    exit 1
fi
