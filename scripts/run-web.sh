#!/bin/bash
set -e

cd "$(dirname "$0")/.." || exit
PID_FILE=".pids/web.pid"

echo "🌐 Starting B-Side Web App (JS)..."

if [ "$1" == "--background" ]; then
    ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous > web.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ Web app started in background. PID: $(cat "$PID_FILE"). Log: web.log"
    echo "Access at: http://localhost:8080"
else
    echo "Access at: http://localhost:8080"
    echo "Press Ctrl+C to stop"
    ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
fi