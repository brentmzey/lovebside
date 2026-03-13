#!/usr/bin/env bash
# Screenshot capture helper for Android
# Usage: ./scripts/screenshot-android.sh <output-file>

set -e

OUTPUT_FILE=${1:-"screenshot.png"}

echo "📱 Taking Android screenshot..."

# Check if device is connected
if ! adb devices | grep -q "device$"; then
  echo "❌ No Android device/emulator found"
  echo "   Run: adb devices"
  exit 1
fi

# Take screenshot
adb shell screencap -p /sdcard/temp_screenshot.png

# Pull to local
adb pull /sdcard/temp_screenshot.png "$OUTPUT_FILE"

# Clean up
adb shell rm /sdcard/temp_screenshot.png

echo "✅ Screenshot saved: $OUTPUT_FILE"

# Show file info
ls -lh "$OUTPUT_FILE"
