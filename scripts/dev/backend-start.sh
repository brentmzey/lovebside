#!/bin/bash
#
# backend-start.sh - Start just the backend services (PocketBase + Ktor Server)
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT" || exit 1

echo -e "${BOLD}🐳 Starting B-Side Backend Services${NC}\n"

# Build server JAR if needed
if [ ! -f "server/build/libs/server-all.jar" ]; then
    echo -e "${CYAN}📦 Building server JAR...${NC}"
    ./gradlew :server:shadowJar
fi

# Start Docker services
echo -e "${CYAN}🚀 Starting Docker Compose...${NC}"
docker-compose up -d --build

echo -e "${YELLOW}⏳ Waiting for services to be ready...${NC}\n"

# Wait for PocketBase (max 60s)
MAX_WAIT=60
WAIT_COUNT=0
echo -n "Waiting for PocketBase "
until curl -f http://localhost:8092/api/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo -e "\n${RED}❌ PocketBase failed to start${NC}"
        echo -e "${YELLOW}Check logs: docker logs bside-pocketbase${NC}"
        exit 1
    fi
    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done
echo -e " ${GREEN}✅${NC}"

# Wait for Ktor Server (max 60s)
WAIT_COUNT=0
echo -n "Waiting for Ktor Server "
until curl -f http://localhost:8081/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo -e "\n${RED}❌ Server failed to start${NC}"
        echo -e "${YELLOW}Check logs: docker logs bside-server${NC}"
        exit 1
    fi
    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done
echo -e " ${GREEN}✅${NC}\n"

# Success!
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}✅ Backend services are running!${NC}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${BOLD}📍 Endpoints:${NC}"
echo -e "  ${CYAN}PocketBase:${NC}      http://localhost:8092"
echo -e "  ${CYAN}PocketBase Admin:${NC}  http://localhost:8092/_/"
echo -e "  ${CYAN}Ktor API:${NC}        http://localhost:8081"
echo -e "  ${CYAN}Health Check:${NC}    http://localhost:8081/health\n"

echo -e "${BOLD}🔑 Admin Credentials:${NC}"
echo -e "  Email:    tester_admin@bside.love"
echo -e "  Password: password123\n"

echo -e "${BOLD}📊 Useful Commands:${NC}"
echo -e "  ${CYAN}docker logs -f bside-pocketbase${NC}  # View PocketBase logs"
echo -e "  ${CYAN}docker logs -f bside-server${NC}      # View Server logs"
echo -e "  ${CYAN}docker-compose ps${NC}                # Check status"
echo -e "  ${CYAN}docker-compose down${NC}              # Stop services"
echo -e "  ${CYAN}just stop${NC}                        # Stop everything\n"

echo -e "${YELLOW}💡 Press Ctrl+C to stop all services...${NC}"
echo ""

# Cleanup function
cleanup() {
    echo -e "\n${RED}🛑 Shutting down...${NC}"
    ./scripts/stop-all.sh
    exit 0
}

# Trap signals
trap cleanup SIGINT SIGTERM

# Wait indefinitely
sleep infinity &
wait $!
