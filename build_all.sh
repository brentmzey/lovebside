#!/bin/bash
set -e

echo "🧹 Cleaning unstable build artifacts..."
./gradlew clean

echo "🚀 Building Shared Logic (JVM)..."
# Using assembleJvm to avoid iOS klib resolution issues caused by local kover cache
./gradlew :shared:assembleJvm

echo "🧪 Running Critical Architecture Verification..."
echo "   (Verifies Real-Time Messaging & Maps Integration)"
./gradlew :shared:jvmTest

echo "📚 Verifying API SDK..."
./gradlew :bside-api:assemble

echo "🤖 Building Android App..."
./gradlew :composeApp:assembleDebug

echo "✅ SUCCESS! App is built, tested, and ready."
echo "   - Android APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk"
echo "   - Tests: shared/build/reports/tests/jvmTest/index.html"
