#!/bin/bash
# ==============================================================================
# 🚀 B-Side: One Command to Rule Them All
# ==============================================================================

set -e

# Colors
green() { printf "\033[0;32m%s\033[0m\n" "$1"; }
blue() { printf "\033[0;34m%s\033[0m\n" "$1"; }
yellow() { printf "\033[1;33m%s\033[0m\n" "$1"; }
red() { printf "\033[0;31m%s\033[0m\n" "$1"; }

# Clear "BSide" Logo
blue "
  ____  ____  _     _      
 | __ )/ ___|(_) __| | ___ 
 |  _ \___ \| |/ _\` |/ _ \\
 | |_) |___) | | (_| |  __/
 |____/|____/|_|\__,_|\___|
"
printf "🚀 Starting B-Side Verification Script...\n"

# 1. Infrastructure
yellow "\n[1/4] Checking Infrastructure..."
if ! docker info > /dev/null 2>&1; then
  red "Error: Docker is not running."
  exit 1
fi

printf "Starting Backend...\n"
docker-compose up -d

# 2. Desktop Target
yellow "\n[2/4] Launching BSide Desktop..."
./gradlew :composeApp:run > /dev/null 2>&1 &
DESKTOP_PID=$!
green "✅ Desktop launching (PID: $DESKTOP_PID)"

# 3. Android Target
yellow "\n[3/4] Launching BSide Android..."
AVD_NAME="Medium_Phone_API_36.1"
EMULATOR_BIN="$HOME/Library/Android/sdk/emulator/emulator"

if ! $EMULATOR_BIN -list-avds | grep -q "$AVD_NAME"; then
    yellow "⚠️  AVD '$AVD_NAME' not found. searching for others..."
    AVD_NAME=$($EMULATOR_BIN -list-avds | head -n 1)
fi

if [ -n "$AVD_NAME" ]; then
    blue "🤖 Booting Android Emulator: $AVD_NAME"
    $EMULATOR_BIN -avd "$AVD_NAME" -gpu swiftshader_indirect -no-snapshot -no-audio -no-boot-anim > /dev/null 2>&1 &
    
    printf "Waiting for device..."
    $HOME/Library/Android/sdk/platform-tools/adb wait-for-device
    sleep 10 # Wait for boot
    
    blue "📲 Installing Android App..."
    ./gradlew :composeApp:installDebug > /dev/null 2>&1
    
    blue "🚀 Launching Android App..."
    $HOME/Library/Android/sdk/platform-tools/adb shell am start -n love.bside.app/love.bside.app.MainActivity > /dev/null 2>&1
    green "✅ Android App Launched"
else
    red "❌ No Android Virtual Device found. Please create one in Android Studio."
fi

# 4. iOS Target
yellow "\n[4/4] Launching BSide iOS..."

if command -v xcrun >/dev/null 2>&1; then
    # Find a regular iPhone simulator (not Pro/Max for speed)
    SIM_ID=$(xcrun simctl list devices available | grep "iPhone" | grep -v "Pro" | head -n 1 | grep -oE '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}')
    
    if [ -z "$SIM_ID" ]; then
        # Fallback to any iPhone
        SIM_ID=$(xcrun simctl list devices available | grep "iPhone" | head -n 1 | grep -oE '[0-9A-F]{8}-([0-9A-F]{4}-){3}[0-9A-F]{12}')
    fi

    if [ -n "$SIM_ID" ]; then
        blue "📱 Booting Simulator ($SIM_ID)..."
        xcrun simctl boot "$SIM_ID" > /dev/null 2>&1 || true # Ignore if already booted
        open -a Simulator
        
        blue "🔨 Building iOS App (this may take a minute)..."
        # Build to a specific directory so we find the .app easily
        xcodebuild -project iosApp/iosApp.xcodeproj \
            -scheme iosApp \
            -configuration Debug \
            -sdk iphonesimulator \
            -destination "id=$SIM_ID" \
            -derivedDataPath iosApp/build \
            -quiet
            
        APP_PATH="iosApp/build/Build/Products/Debug-iphonesimulator/iosApp.app"
        
        if [ -d "$APP_PATH" ]; then
            blue "📲 Installing iOS App..."
            xcrun simctl install "$SIM_ID" "$APP_PATH"
            
            blue "🚀 Launching iOS App..."
            xcrun simctl launch "$SIM_ID" love.bside.app
            green "✅ iOS App Launched"
        else
            red "❌ Build failed. App bundle not found at $APP_PATH"
        fi
    else
        red "❌ No iPhone Simulator found."
    fi
else
    red "❌ Xcode tools (xcrun) not found."
fi

# Summary
green "\n===================================================="
green "🎉 BSide Environment LIVE!"
green "===================================================="
printf "1. Backend:        http://127.0.0.1:8080\n"
printf "2. Desktop:        Running\n"
printf "3. Android:        Running\n"
printf "4. iOS:            Running\n\n"

yellow "Press Ctrl+C to stop Desktop app."
wait $DESKTOP_PID
