#!/bin/bash
# Opens the current project in Android Studio on macOS

PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "🚀 Opening Android Studio..."
open -a "Android Studio" "$PROJECT_DIR"
