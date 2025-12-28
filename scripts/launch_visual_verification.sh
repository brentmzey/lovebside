#!/bin/bash

echo "=== B-Side Visual Verification Launcher ==="
echo "This script attempts to launch Desktop and Web targets for visual inspection."
echo "For Android/iOS, please ensure emulators are running."

# 1. Desktop
echo "🚀 Launching Desktop Application..."
./gradlew desktopRun > /dev/null 2>&1 &
DESKTOP_PID=$!
echo "   Desktop launching (PID: $DESKTOP_PID)..."

# 2. Web
echo "🚀 Launching Web Application..."
./gradlew jsBrowserRun > /dev/null 2>&1 &
WEB_PID=$!
echo "   Web launching (PID: $WEB_PID)..."

# 3. Instructions
echo ""
echo "=== SCREENSHOT INSTRUCTIONS ==="
echo "1. Desktop: A JVM window should appear. Win+Shift+S (Win) or Cmd+Shift+4 (Mac) to capture."
echo "2. Web: Your default browser should open. Use browser dev tools (F12) to emulate mobile view if needed."
echo "3. Android: Run ./scripts/run-android.sh. Use Cmd+Shift+4 to capture emulator."
echo "4. iOS: Run ./scripts/run-ios.sh. Use Cmd+Shift+4 to capture simulator."
echo ""
echo "Press ENTER to stop Web/Desktop processes when done."
read

kill $DESKTOP_PID
kill $WEB_PID
echo "✅ Processes stopped."
