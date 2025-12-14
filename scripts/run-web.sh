#!/bin/bash
set -e

cd "$(dirname "$0")/.." || exit
PID_FILE=".pids/web.pid"

GRADLE_TASK=":composeApp:jsBrowserDevelopmentRun"

echo "🌐 Starting B-Side Web App (Wasm JS)..."

if [ "$1" == "--background" ]; then
    ./gradlew ${GRADLE_TASK} --continuous > web.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ Web app started in background. PID: $(cat "$PID_FILE"). Log: web.log"
    echo "Access at: http://localhost:8090"
else
    echo "Access at: http://localhost:8090"
    echo "Press Ctrl+C to stop"
    ./gradlew ${GRADLE_TASK} --continuous
fi