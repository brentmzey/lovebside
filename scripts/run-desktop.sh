#!/bin/bash
set -e

cd "$(dirname "$0")/.." || exit
PID_FILE=".pids/desktop.pid"

echo "🚀 Starting B-Side Desktop App..."

if [ "$1" == "--background" ]; then
    ./gradlew :composeApp:jvmRun > desktop.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ Desktop app started in background. PID: $(cat "$PID_FILE"). Log: desktop.log"
else
    ./gradlew :composeApp:jvmRun
fi