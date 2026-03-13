#!/usr/bin/env bash
# Screenshot capture helper for iOS Simulator
# Usage: ./scripts/screenshot-ios.sh <output-file>

set -e

OUTPUT_FILE=${1:-"screenshot.png"}

echo "🍎 Taking iOS screenshot..."

# Check if simulator is running
if ! xcrun simctl list devices | grep -q "Booted"; then
  echo "❌ No iOS simulator is running"
  echo "   Start simulator from Xcode or run:"
  echo "   open -a Simulator"
  exit 1
fi

# Take screenshot
xcrun simctl io booted screenshot "$OUTPUT_FILE"

echo "✅ Screenshot saved: $OUTPUT_FILE"

# Show file info
ls -lh "$OUTPUT_FILE"
