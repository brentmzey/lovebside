#!/bin/bash
set -e

echo "🚀 Building Backend & Common Logic..."
./gradlew :shared:assemble

echo "🤖 Building Android App..."
./gradlew :composeApp:assembleDebug

echo "🍎 Building iOS Framework..."
./gradlew :composeApp:assembleIosX64

echo "✅ Build Complete!"
