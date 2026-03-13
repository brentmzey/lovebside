#!/bin/bash

# spark_all.sh - The minimized, robust startup script for BSide
# Starts Backend -> Verifies Health -> Launches Clients Sequentially/Interactively

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

print_info() { echo -e "${BLUE}ℹ $1${NC}"; }
print_success() { echo -e "${GREEN}✓ $1${NC}"; }
print_warn() { echo -e "${YELLOW}⚠ $1${NC}"; }
print_error() { echo -e "${RED}✗ $1${NC}"; }

# 1. Check Pre-requisites
print_info "Checking environment..."
if ! docker info > /dev/null 2>&1; then
    print_error "Docker is not running. Please start Docker Desktop."
    exit 1
fi

# 2. Start Backend
print_info "Building Server JAR..."
./gradlew :server:shadowJar

print_info "Starting Backend Services (PocketBase + Ktor)..."
# Use existing Justfile command which runs docker-compose
# using --detach to run in background, but we will monitor it
docker-compose up -d --build

# 3. Wait for Health
print_info "Waiting for services to be healthy..."
timeout=60
counter=0
pb_healthy=false
server_healthy=false

while [ $counter -lt $timeout ]; do
    if ! $pb_healthy && curl -s http://localhost:8092/api/health > /dev/null; then
        print_success "PocketBase is UP (http://localhost:8092)"
        pb_healthy=true
    fi
    
    # Check Ktor server (port 8081 mapped to 8080)
    if ! $server_healthy && curl -s http://localhost:8081/health > /dev/null; then
        print_success "Ktor Server is UP (http://localhost:8081)"
        server_healthy=true
    fi

    if $pb_healthy && $server_healthy; then
        break
    fi

    sleep 2
    counter=$((counter+2))
    echo -n "."
done
echo ""

if ! $pb_healthy || ! $server_healthy; then
    print_error "Timed out waiting for services."
    print_warn "PocketBase status: $pb_healthy"
    print_warn "Server status: $server_healthy"
    print_info "Check logs with: docker-compose logs -f"
    exit 1
fi

print_success "All Backend Services Ready!"

# 4. Seeding (Optional)
if [ "$1" == "--seed" ] || [ "$2" == "--seed" ]; then
    print_info "Seeding database..."
    if [ -f "./scripts/seed_data.sh" ]; then
        ./scripts/seed_data.sh
    else
        print_warn "Seed script not found."
    fi
fi

# 5. Launch Clients (Interactive or All)
launch_client() {
    client=$1
    case $client in
        "web")
             print_info "Launching Web Client..."
             ./gradlew :composeApp:jsBrowserDevelopmentRun --continuous &
             ;;
        "desktop")
             print_info "Launching Desktop Client..."
             ./gradlew :composeApp:run &
             ;;
        *)
             print_error "Unknown client: $client"
             ;;
    esac
}

if [ "$1" == "--all" ]; then
    launch_client "web"
    launch_client "desktop"
elif [ "$1" == "--backend" ]; then
    echo "Backend running."
else
    echo ""
    echo "Which clients would you like to launch?"
    echo "1) Web (Browser)"
    echo "2) Desktop (JVM)"
    echo "3) Both"
    echo "4) None (Backend only)"
    read -p "Enter choice [1-4]: " choice
    
    case $choice in
        1) launch_client "web" ;;
        2) launch_client "desktop" ;;
        3) 
           launch_client "web"
           launch_client "desktop" 
           ;;
        4) echo "Backend running.";;
        *) echo "Invalid choice.";;
    esac
fi

print_success "🚀 Startup process complete. Clients launching in background."
