#!/bin/bash

# Quick Demo Script - Run two platforms side-by-side for real-time messaging demo

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

print_header() {
    echo -e "\n${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║  $1${NC}"
    echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}\n"
}

print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠ $1${NC}"; }

print_header "BSide Real-Time Messaging Demo"

echo -e "${CYAN}This script will:"
echo -e "  1. Build the app"
echo -e "  2. Run Desktop app (User 1)"
echo -e "  3. Open Web app in browser (User 2)"
echo -e "  4. You can chat between them in real-time!${NC}\n"

read -p "Continue? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 0
fi

# Build first
print_info "Building app (this may take a minute)..."
if ./gradlew :composeApp:build --quiet 2>&1 | grep -q "BUILD SUCCESSFUL"; then
    print_success "Build successful!"
else
    # Try anyway
    print_warning "Build had some warnings, continuing..."
fi

# Start web server in background
print_info "Starting Web server (User 2)..."
print_success "Web app will open at: http://localhost:8080"
./gradlew :composeApp:wasmJsBrowserDevelopmentRun &
WEB_PID=$!

# Wait for web server to start
sleep 5

# Open browser
if command -v open &> /dev/null; then
    open http://localhost:8080
fi

print_success "Web app started (User 2)"
print_info "Login as: test2@example.com / test12345"
echo ""

# Start desktop app
print_info "Starting Desktop app (User 1)..."
print_info "Login as: test@example.com / test12345"
echo ""

./gradlew :composeApp:run &
DESKTOP_PID=$!

# Cleanup function
cleanup() {
    echo ""
    print_warning "Shutting down..."
    kill $WEB_PID 2>/dev/null || true
    kill $DESKTOP_PID 2>/dev/null || true
    print_success "Demo stopped"
    exit 0
}

trap cleanup SIGINT SIGTERM

print_header "Demo Running!"
echo -e "${GREEN}Both apps are now running:${NC}"
echo -e "  ${BLUE}Desktop${NC}: User 1 (test@example.com)"
echo -e "  ${BLUE}Web${NC}:     User 2 (test2@example.com)"
echo ""
echo -e "${YELLOW}Try this:${NC}"
echo "  1. Login on both apps"
echo "  2. Create a conversation"
echo "  3. Send messages from either app"
echo "  4. Watch them appear instantly on the other!"
echo ""
echo -e "${CYAN}Press Ctrl+C to stop both apps${NC}\n"

# Wait for processes
wait
