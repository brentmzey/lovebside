#!/bin/bash
set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🚀 B-Side Multiplatform Launcher${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Ensure cleanup happens on exit
trap './stop-all.sh' INT TERM EXIT

# Start the server in the background
./run-server.sh --background

# Wait for the server to be ready
echo -n "Waiting for server to be ready on http://localhost:8080"
for i in {1..20}; do
    if curl --output /dev/null --silent --fail http://localhost:8080/health; then
        echo -e "\n${GREEN}✅ Server is ready!${NC}\n"
        break
    fi
    echo -n "."
    sleep 1
done

if ! curl --output /dev/null --silent --fail http://localhost:8080/health; then
    echo -e "\n${RED}❌ Server failed to start. Check server.log for details.${NC}"
    exit 1
fi

# Start frontend apps
./run-desktop.sh --background
echo ""
./run-web.sh --background

echo -e "\n${GREEN}✅ All core services started!${NC}"
echo -e "- ${BLUE}Backend Server:${NC} http://localhost:8080"
echo -e "- ${BLUE}Desktop App:${NC} Running in background (see desktop.log)"
echo -e "- ${BLUE}Web App:${NC} http://localhost:8080 (see web.log)"

echo -e "\nRun individual scripts for other platforms (e.g., ./run-android.sh)"

echo -e "\n${YELLOW}Press Ctrl+C to stop all processes.${NC}\n"

# Tail the server log to keep the script running and show server output
tail -f server.log