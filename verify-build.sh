#!/bin/bash

# Build Verification Script for B-Side Kotlin Multiplatform App
# This script verifies that all platform targets compile successfully

set -e  # Exit on error

echo "🚀 B-Side Build Verification Script"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to print colored output
print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${YELLOW}ℹ️  $1${NC}"
}

print_section() {
    echo ""
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "$1"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# Check prerequisites
print_section "1. Checking Prerequisites"

if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -n 1)
    print_success "Java found: $JAVA_VERSION"
else
    print_error "Java not found. Please install JDK 11 or higher."
    exit 1
fi

if [ -f "./gradlew" ]; then
    print_success "Gradle wrapper found"
else
    print_error "Gradle wrapper not found. Are you in the project root?"
    exit 1
fi

# Make gradlew executable
chmod +x ./gradlew

# Clean build
print_section "2. Cleaning Previous Build"
print_info "Running: ./gradlew clean"
./gradlew clean --console=plain --no-daemon > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "Clean completed"
else
    print_error "Clean failed"
    exit 1
fi

# Build commonMain
print_section "3. Building Common Main (Shared Code)"
print_info "Running: ./gradlew :shared:compileCommonMainKotlinMetadata"
./gradlew :shared:compileCommonMainKotlinMetadata --console=plain --no-daemon
if [ $? -eq 0 ]; then
    print_success "Common Main compiled successfully"
else
    print_error "Common Main compilation failed"
    exit 1
fi

# Build Android
print_section "4. Building Android Target"
print_info "Running: ./gradlew :shared:assembleDebug"
./gradlew :shared:assembleDebug --console=plain --no-daemon > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "Android Debug build successful"
else
    print_error "Android build failed"
    exit 1
fi

# Build JVM
print_section "5. Building JVM/Desktop Target"
print_info "Running: ./gradlew :shared:compileKotlinJvm"
./gradlew :shared:compileKotlinJvm --console=plain --no-daemon > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "JVM build successful"
else
    print_error "JVM build failed"
    exit 1
fi

# Build JS
print_section "6. Building JavaScript Target"
print_info "Running: ./gradlew :shared:compileKotlinJs"
./gradlew :shared:compileKotlinJs --console=plain --no-daemon > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "JavaScript build successful"
else
    print_error "JavaScript build failed"
    exit 1
fi

# Build WasmJS
print_section "7. Building WebAssembly Target"
print_info "Running: ./gradlew :shared:compileKotlinWasmJs"
./gradlew :shared:compileKotlinWasmJs --console=plain --no-daemon > /dev/null 2>&1
if [ $? -eq 0 ]; then
    print_success "WebAssembly build successful"
else
    print_error "WebAssembly build failed"
    exit 1
fi

# Build iOS (only on macOS)
if [[ "$OSTYPE" == "darwin"* ]]; then
    print_section "8. Building iOS Targets"
    
    print_info "Building iOS Simulator ARM64..."
    ./gradlew :shared:compileKotlinIosSimulatorArm64 --console=plain --no-daemon > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_success "iOS Simulator ARM64 build successful"
    else
        print_error "iOS Simulator ARM64 build failed"
    fi
    
    print_info "Building iOS ARM64..."
    ./gradlew :shared:compileKotlinIosArm64 --console=plain --no-daemon > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_success "iOS ARM64 build successful"
    else
        print_error "iOS ARM64 build failed"
    fi
    
    print_info "Building iOS X64..."
    ./gradlew :shared:compileKotlinIosX64 --console=plain --no-daemon > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        print_success "iOS X64 build successful"
    else
        print_error "iOS X64 build failed"
    fi
else
    print_info "Skipping iOS builds (not on macOS)"
fi

# Summary
print_section "Build Verification Complete"
echo ""
print_success "All platform targets compiled successfully! 🎉"
echo ""
echo "Platform Status:"
echo "  ✅ Common Main (Shared Code)"
echo "  ✅ Android (Debug)"
echo "  ✅ JVM/Desktop"
echo "  ✅ JavaScript/Web"
echo "  ✅ WebAssembly"
if [[ "$OSTYPE" == "darwin"* ]]; then
    echo "  ✅ iOS (arm64, simulator arm64, x64)"
else
    echo "  ⊘ iOS (not tested - not on macOS)"
fi
echo ""
echo "Next Steps:"
echo "  1. Review HANDOFF_SUMMARY.md for architecture overview"
echo "  2. Check PRODUCTIONALIZATION_ROADMAP.md for next tasks"
echo "  3. Test PocketBase connection"
echo "  4. Initialize DI in applications"
echo "  5. Write integration tests"
echo ""
print_success "Build verification successful!"
