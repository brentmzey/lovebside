#!/bin/bash
set -e

cd "$(dirname "$0")/.." || exit
PID_FILE=".pids/wasm.pid"

echo "🕸️ Starting B-Side WASM App..."

if [ "$1" == "--background" ]; then
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous > wasm.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ WASM app started in background. PID: $(cat "$PID_FILE"). Log: wasm.log"
    echo "Access at: http://localhost:8080"
else
    echo "Access at: http://localhost:8080"
    echo "Press Ctrl+C to stop"
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
fi
