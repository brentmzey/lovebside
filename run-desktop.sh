#!/bin/bash
set -e

PID_FILE=".pids/desktop.pid"

echo "🚀 Starting B-Side Desktop App..."

if [ "$1" == "--background" ]; then
    ./gradlew :composeApp:run > desktop.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ Desktop app started in background. PID: $(cat "$PID_FILE"). Log: desktop.log"
else
    ./gradlew :composeApp:run
fi