#!/bin/bash
set -e

JAR_FILE="server/build/libs/server-all.jar"
PID_FILE=".pids/server.pid"

# Build server if JAR doesn't exist
if [ ! -f "$JAR_FILE" ]; then
    echo "🚀 Server JAR not found. Building..."
    ./gradlew :server:shadowJar
    echo "✅ Server JAR built successfully."
fi

# Stop existing server if running
if [ -f "$PID_FILE" ]; then
    if kill -0 "$(cat "$PID_FILE")" 2>/dev/null; then
        echo "🛑 Stopping existing server..."
        kill "$(cat "$PID_FILE")"
        rm "$PID_FILE"
    fi
fi

echo "🚀 Starting B-Side Server..."

if [ "$1" == "--background" ]; then
    java -jar "$JAR_FILE" > server.log 2>&1 &
    echo $! > "$PID_FILE"
    echo "✅ Server started in background. PID: $(cat "$PID_FILE"). Log: server.log"
else
    echo "Server will run on http://localhost:8080"
    echo "Press Ctrl+C to stop"
    java -jar "$JAR_FILE"
fi