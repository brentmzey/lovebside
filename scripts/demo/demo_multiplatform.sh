#!/bin/bash

# BSide Multi-Platform Real-Time Messaging Demo
# This script helps launch the app on multiple platforms for testing real-time messaging

set -e

COLOR_GREEN='\033[0;32m'
COLOR_BLUE='\033[0;34m'
COLOR_YELLOW='\033[1;33m'
COLOR_RED='\033[0;31m'
COLOR_RESET='\033[0m'

print_header() {
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
    echo -e "${COLOR_BLUE}$1${COLOR_RESET}"
    echo -e "${COLOR_BLUE}========================================${COLOR_RESET}"
}

print_info() {
    echo -e "${COLOR_GREEN}[INFO]${COLOR_RESET} $1"
}

print_warn() {
    echo -e "${COLOR_YELLOW}[WARN]${COLOR_RESET} $1"
}

print_error() {
    echo -e "${COLOR_RED}[ERROR]${COLOR_RESET} $1"
}

print_header "BSide Multi-Platform Demo Launcher"

echo ""
print_info "This script will help you launch the BSide app on multiple platforms"
print_info "for testing real-time messaging between users."
echo ""

# Check if PocketBase is running
print_info "Checking if PocketBase is running..."
if ! curl -s http://localhost:8090/api/health > /dev/null 2>&1; then
    print_error "PocketBase is not running!"
    print_info "Please start PocketBase first:"
    echo "  cd pocketbase && ./pocketbase serve"
    exit 1
fi
print_info "✓ PocketBase is running"

echo ""
print_header "Platform Selection"
echo "Which platforms would you like to launch?"
echo "1) Desktop (JVM)"
echo "2) Web (Browser)"
echo "3) Android (Emulator)"
echo "4) All available platforms"
echo "5) Custom selection"
read -p "Enter choice [1-5]: " platform_choice

launch_desktop() {
    print_info "Launching Desktop app..."
    ./gradlew :composeApp:run &
    DESKTOP_PID=$!
    print_info "Desktop app launched (PID: $DESKTOP_PID)"
}

launch_web() {
    print_info "Launching Web app..."
    print_info "Building web bundle..."
    ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous &
    WEB_PID=$!
    print_info "Web app will be available at http://localhost:8080"
    print_info "Web server launched (PID: $WEB_PID)"
}

launch_android() {
    print_info "Launching Android app..."
    print_info "Checking for Android emulators..."
    
    # Check if emulator is running
    ADB_DEVICES=$(adb devices | grep "emulator" | wc -l)
    
    if [ "$ADB_DEVICES" -eq "0" ]; then
        print_warn "No Android emulator detected"
        print_info "Available emulators:"
        emulator -list-avds
        echo ""
        read -p "Enter emulator name to start (or 'skip' to skip Android): " emulator_name
        
        if [ "$emulator_name" != "skip" ]; then
            print_info "Starting emulator: $emulator_name"
            emulator -avd "$emulator_name" &
            EMULATOR_PID=$!
            print_info "Waiting for emulator to boot..."
            adb wait-for-device
            sleep 10
        else
            return
        fi
    fi
    
    print_info "Installing Android app..."
    ./gradlew :composeApp:installDebug
    
    print_info "Launching Android app..."
    adb shell am start -n love.bside.app/.MainActivity
    
    print_info "✓ Android app launched"
}

case $platform_choice in
    1)
        launch_desktop
        ;;
    2)
        launch_web
        ;;
    3)
        launch_android
        ;;
    4)
        print_info "Launching all platforms..."
        launch_desktop
        sleep 2
        launch_web
        sleep 2
        launch_android
        ;;
    5)
        echo "Select platforms to launch (y/n):"
        read -p "Desktop? [y/n]: " desktop_choice
        read -p "Web? [y/n]: " web_choice
        read -p "Android? [y/n]: " android_choice
        
        [ "$desktop_choice" = "y" ] && launch_desktop && sleep 2
        [ "$web_choice" = "y" ] && launch_web && sleep 2
        [ "$android_choice" = "y" ] && launch_android
        ;;
    *)
        print_error "Invalid choice"
        exit 1
        ;;
esac

echo ""
print_header "Demo Instructions"
echo ""
print_info "Testing Real-Time Messaging:"
echo "  1. Login with different users on each platform"
echo "  2. Start a conversation between them"
echo "  3. Send messages - they should appear in real-time"
echo "  4. Test threading by replying to messages"
echo ""
print_info "Testing Offline Mode:"
echo "  1. Disable network on one platform (airplane mode / disconnect wifi)"
echo "  2. Send messages - they should queue locally"
echo "  3. Re-enable network"
echo "  4. Messages should auto-sync to server"
echo ""
print_info "For screen recording:"
echo "  - macOS: Cmd+Shift+5 for screen recording"
echo "  - Android: 'adb shell screenrecord /sdcard/demo.mp4'"
echo "  - Web: Use Chrome DevTools or OBS"
echo ""

print_info "Press Ctrl+C to stop all processes"

# Wait for user interrupt
trap 'print_info "Stopping all processes..."; kill $DESKTOP_PID $WEB_PID $EMULATOR_PID 2>/dev/null; exit 0' INT TERM

wait
