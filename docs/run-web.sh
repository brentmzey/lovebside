#!/bin/bash
# Run Web only

echo "🌐 Launching Web app..."

echo "Starting dev server..."
./gradlew :composeApp:jsBrowserDevelopmentRun

echo "Browser should open automatically to http://localhost:8080"
