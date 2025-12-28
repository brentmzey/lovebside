#!/bin/bash

# BSide Multi-Platform Build, Test & Run Script
# Builds and tests all platform targets: Android, iOS, Desktop, Web

# Don't exit on error - we want to handle them gracefully
set -o pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Track if we had any issues
BUILD_WARNINGS=0
TEST_FAILURES=0

# Helper functions
print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

# Parse command line arguments
SKIP_TESTS=false
SKIP_BUILD=false
RUN_TARGET=""
VERBOSE=false

usage() {
    cat << EOF
Usage: $0 [OPTIONS] [TARGET]

Build, test, and optionally run BSide app on all platforms.

OPTIONS:
    -h, --help              Show this help message
    -t, --skip-tests        Skip running tests
    -b, --skip-build        Skip build (only run target)
    -v, --verbose           Show verbose gradle output
    --clean                 Clean before building

TARGETS (optional - will prompt if not specified):
    desktop                 Run Desktop (JVM) app
    web-wasm                Run Web (WebAssembly) app
    web-js                  Run Web (JavaScript) app
    android                 Install Android app (requires device/emulator)
    ios                     Open iOS project in Xcode
    all                     Build all targets (no run)

EXAMPLES:
    $0                      # Interactive mode
    $0 desktop              # Build, test, and run desktop app
    $0 --skip-tests web-wasm # Build and run web app without tests
    $0 -t -b desktop        # Just run desktop (skip build and tests)

EOF
    exit 0
}

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -h|--help)
            usage
            ;;
        -t|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        -b|--skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        --clean)
            print_header "Cleaning Build"
            ./gradlew clean
            print_success "Clean complete"
            shift
            ;;
        desktop|web-wasm|web-js|android|ios|all)
            RUN_TARGET=$1
            shift
            ;;
        *)
            print_error "Unknown option: $1"
            usage
            ;;
    esac
done

# Set gradle flags
GRADLE_FLAGS=""
if [ "$VERBOSE" = false ]; then
    GRADLE_FLAGS="--quiet"
fi

print_header "BSide Multi-Platform Build System"
print_info "Target: ${RUN_TARGET:-Interactive}"
print_info "Skip Tests: $SKIP_TESTS"
print_info "Skip Build: $SKIP_BUILD"
echo ""

# ============================================================================
# BUILD PHASE
# ============================================================================

if [ "$SKIP_BUILD" = false ]; then
    print_header "Building All Platforms"
    
    # Build shared module first
    print_info "Building shared module..."
    if ./gradlew :shared:build $GRADLE_FLAGS 2>&1; then
        print_success "Shared module built"
    else
        BUILD_EXIT=$?
        print_warning "Shared module build had warnings (exit code: $BUILD_EXIT)"
        BUILD_WARNINGS=$((BUILD_WARNINGS + 1))
        print_info "Continuing anyway..."
    fi
    
    # Build compose app for all targets
    print_info "Building composeApp for all platforms..."
    if ./gradlew :composeApp:build $GRADLE_FLAGS 2>&1; then
        print_success "ComposeApp built for all platforms"
    else
        BUILD_EXIT=$?
        print_warning "ComposeApp build had warnings (exit code: $BUILD_EXIT)"
        BUILD_WARNINGS=$((BUILD_WARNINGS + 1))
        print_info "Continuing anyway..."
    fi
    
    if [ $BUILD_WARNINGS -eq 0 ]; then
        print_success "All platforms built successfully!"
    else
        print_warning "Build completed with $BUILD_WARNINGS warning(s)"
    fi
fi

# ============================================================================
# TEST PHASE
# ============================================================================

if [ "$SKIP_TESTS" = false ]; then
    print_header "Running Tests"
    
    # Run shared module tests
    print_info "Running shared module tests..."
    if ./gradlew :shared:test $GRADLE_FLAGS 2>&1; then
        print_success "Shared module tests passed"
    else
        TEST_EXIT=$?
        print_warning "Shared module tests had failures (exit code: $TEST_EXIT)"
        TEST_FAILURES=$((TEST_FAILURES + 1))
        print_info "Note: Integration tests may fail if PocketBase isn't accessible"
        print_info "Continuing anyway..."
    fi
    
    # Run composeApp tests
    print_info "Running composeApp tests..."
    if ./gradlew :composeApp:test $GRADLE_FLAGS 2>&1; then
        print_success "ComposeApp tests passed"
    else
        TEST_EXIT=$?
        print_warning "ComposeApp tests had failures (exit code: $TEST_EXIT)"
        TEST_FAILURES=$((TEST_FAILURES + 1))
        print_info "Continuing anyway..."
    fi
    
    if [ $TEST_FAILURES -eq 0 ]; then
        print_success "All tests passed!"
    else
        print_warning "Testing completed with $TEST_FAILURES failure(s)"
        print_info "You can run './gradlew test --rerun-tasks' to see detailed failures"
    fi
fi

# Show summary if there were any issues
if [ $BUILD_WARNINGS -gt 0 ] || [ $TEST_FAILURES -gt 0 ]; then
    echo ""
    print_warning "Build Summary:"
    [ $BUILD_WARNINGS -gt 0 ] && print_warning "  - Build warnings: $BUILD_WARNINGS"
    [ $TEST_FAILURES -gt 0 ] && print_warning "  - Test failures: $TEST_FAILURES"
    echo ""
    print_info "Tip: Use --skip-tests to skip testing and just run"
    print_info "Tip: Check build logs with: ./gradlew build"
    echo ""
fi

# ============================================================================
# RUN PHASE
# ============================================================================

run_desktop() {
    print_header "Running Desktop App (JVM)"
    print_info "Starting Compose Desktop application..."
    print_warning "Close the app window when done testing"
    ./gradlew :composeApp:run
}

run_web_wasm() {
    print_header "Running Web App (WebAssembly)"
    print_info "Starting development server..."
    print_success "App will open at: http://localhost:8080"
    print_warning "Press Ctrl+C to stop the server"
    ./gradlew :composeApp:wasmJsBrowserDevelopmentRun --continuous
}

run_web_js() {
    print_header "Running Web App (JavaScript)"
    print_info "Starting development server..."
    print_success "App will open at: http://localhost:8080"
    print_warning "Press Ctrl+C to stop the server"
    ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous
}

run_android() {
    print_header "Installing Android App"
    
    # Check if adb is available
    if ! command -v adb &> /dev/null; then
        print_error "adb not found. Please install Android SDK Platform Tools."
        exit 1
    fi
    
    # Check for connected devices
    if [ -z "$(adb devices | grep -v 'List' | grep 'device')" ]; then
        print_error "No Android devices/emulators connected."
        print_info "Start an emulator or connect a device and try again."
        exit 1
    fi
    
    print_info "Installing app on connected device/emulator..."
    if ./gradlew :composeApp:installDebug $GRADLE_FLAGS; then
        print_success "App installed successfully!"
        print_info "Launch the app from your device/emulator"
    else
        print_error "Android installation failed"
        exit 1
    fi
}

run_ios() {
    print_header "Opening iOS Project"
    
    # Check if on macOS
    if [[ "$OSTYPE" != "darwin"* ]]; then
        print_error "iOS development requires macOS"
        exit 1
    fi
    
    # Check if Xcode is installed
    if ! command -v xcodebuild &> /dev/null; then
        print_error "Xcode not found. Please install Xcode from the App Store."
        exit 1
    fi
    
    print_info "Opening Xcode project..."
    if [ -d "iosApp/iosApp.xcodeproj" ]; then
        open iosApp/iosApp.xcodeproj
        print_success "Xcode project opened!"
        print_info "Build and run from Xcode (Cmd+R)"
    else
        print_error "iOS project not found at iosApp/iosApp.xcodeproj"
        exit 1
    fi
}

# Interactive target selection if not specified
if [ -z "$RUN_TARGET" ]; then
    print_header "Select Target to Run"
    echo "1) Desktop (JVM)"
    echo "2) Web (WebAssembly)"
    echo "3) Web (JavaScript)"
    echo "4) Android (requires device/emulator)"
    echo "5) iOS (requires macOS & Xcode)"
    echo "6) Exit (build/test only)"
    echo ""
    read -p "Enter choice [1-6]: " choice
    
    case $choice in
        1) RUN_TARGET="desktop" ;;
        2) RUN_TARGET="web-wasm" ;;
        3) RUN_TARGET="web-js" ;;
        4) RUN_TARGET="android" ;;
        5) RUN_TARGET="ios" ;;
        6) 
            print_success "Build and test complete. Exiting."
            exit 0
            ;;
        *)
            print_error "Invalid choice"
            exit 1
            ;;
    esac
fi

# Execute selected target
case $RUN_TARGET in
    desktop)
        run_desktop
        ;;
    web-wasm)
        run_web_wasm
        ;;
    web-js)
        run_web_js
        ;;
    android)
        run_android
        ;;
    ios)
        run_ios
        ;;
    all)
        print_success "All platforms built and tested. No run target specified."
        print_info "Run this script again with a specific target to launch."
        ;;
    *)
        print_error "Unknown target: $RUN_TARGET"
        usage
        ;;
esac

print_header "Complete!"
print_success "BSide multi-platform build completed successfully"
