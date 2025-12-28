#!/bin/bash
set -e

echo "🧹 Cleaning unstable build artifacts..."
./gradlew clean

echo "🚀 Building Shared Core..."
# "assemble" on shared builds all KMP targets (android, ios, jvm, js) configured in shared/build.gradle.kts
./gradlew :shared:assemble

echo "🧪 Running All Tests (Unit + Integration)..."
# Runs tests for ALL targets defined in shared (android, jvm, ios, js)
./gradlew :shared:allTests

echo "📱 Verifying Android App..."
./gradlew :composeApp:assembleDebug

echo "🍎 Verifying iOS Framework (Simulator)..."
# Ensures the iOS framework links correctly (critical for catching iOS-specific compilation errors)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64

echo "🖥️ Verifying Desktop (JVM) App..."
# Compiles the desktop application
./gradlew :composeApp:desktopJar

echo "🌐 Verifying Web (JS/Wasm) App..."
# Compiles the JS browser distribution
./gradlew :composeApp:jsBrowserDistribution

echo "✅ SUCCESS! All KMP targets (Android, iOS, Desktop, Web) built and tested."
echo "   - Android APK: composeApp/build/outputs/apk/debug/composeApp-debug.apk"
echo "   - Web Dist: composeApp/build/dist/js/productionExecutable"
echo "   - Test Reports: shared/build/reports/tests/"
