#!/bin/bash
#
# dev-start.sh - Robust local development startup script
# 
# This script starts the B-Side stack in the correct order with proper health checks
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT" || exit 1

echo -e "${CYAN}${BOLD}"
cat << "EOF"
  ██████╗       ███████╗██╗██████╗ ███████╗
  ██╔══██╗      ██╔════╝██║██╔══██╗██╔════╝
  ██████╔╝█████╗███████╗██║██║  ██║█████╗  
  ██╔══██╗╚════╝╚════██║██║██║  ██║██╔══╝  
  ██████╔╝      ███████║██║██████╔╝███████╗
  ╚═════╝       ╚══════╝╚═╝╚═════╝ ╚══════╝

  🚀 Local Development Startup
EOF
echo -e "${NC}\n"

# Check prerequisites
echo -e "${BOLD}📋 Checking Prerequisites...${NC}"

# Check Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker is not installed. Please install Docker Desktop.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker found${NC}"

# Check Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ docker-compose is not installed. Please install it.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ docker-compose found${NC}"

# Check Java
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java is not installed. Please install JDK 17+${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | head -n 1 | awk -F '"' '{print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java version must be 17 or higher (found: $JAVA_VERSION)${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Java $JAVA_VERSION found${NC}"

# Check Node
if ! command -v node &> /dev/null; then
    echo -e "${YELLOW}⚠️  Node.js not found. Web target may not work.${NC}"
else
    echo -e "${GREEN}✅ Node.js found${NC}"
fi

echo ""

# Step 1: Build Server JAR
echo -e "${BOLD}🔨 Step 1/5: Building Server JAR...${NC}"
if [ ! -f "server/build/libs/server-all.jar" ]; then
    echo -e "${CYAN}Building Ktor server...${NC}"
    ./gradlew :server:shadowJar
    echo -e "${GREEN}✅ Server JAR built${NC}"
else
    echo -e "${GREEN}✅ Server JAR already exists (skipping build)${NC}"
fi
echo ""

# Step 2: Start Docker Services
echo -e "${BOLD}🐳 Step 2/5: Starting Backend Services (Docker)...${NC}"

# Clean up any existing containers
echo -e "${CYAN}Cleaning up existing containers...${NC}"
docker-compose down > /dev/null 2>&1 || true

# Start services
echo -e "${CYAN}Starting PocketBase and Ktor Server...${NC}"
docker-compose up -d --build

echo -e "${YELLOW}Waiting for services to be healthy...${NC}"

# Wait for PocketBase
MAX_WAIT=60
WAIT_COUNT=0
until curl -f http://localhost:8092/api/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo -e "${RED}❌ PocketBase failed to start after ${MAX_WAIT}s${NC}"
        echo -e "${YELLOW}Check logs with: docker logs bside-pocketbase${NC}"
        exit 1
    fi
    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done
echo ""
echo -e "${GREEN}✅ PocketBase is running at http://localhost:8092${NC}"

# Wait for Server
WAIT_COUNT=0
until curl -f http://localhost:8081/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo -e "${RED}❌ Ktor Server failed to start after ${MAX_WAIT}s${NC}"
        echo -e "${YELLOW}Check logs with: docker logs bside-server${NC}"
        exit 1
    fi
    echo -n "."
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done
echo ""
echo -e "${GREEN}✅ Ktor Server is running at http://localhost:8081${NC}"

echo ""

# Step 3: Launch Desktop App (Optional)
echo -e "${BOLD}🖥️  Step 3/5: Desktop App${NC}"
read -p "Launch Desktop app? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${CYAN}Starting Desktop app in background...${NC}"
    nohup ./gradlew :composeApp:jvmRun > desktop.log 2>&1 &
    DESKTOP_PID=$!
    echo -e "${GREEN}✅ Desktop app starting (PID: $DESKTOP_PID, logs: desktop.log)${NC}"
else
    echo -e "${YELLOW}⏭️  Skipped Desktop app${NC}"
fi
echo ""

# Step 4: Launch Web App (Optional)
echo -e "${BOLD}🌐 Step 4/5: Web App${NC}"
read -p "Launch Web app? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${CYAN}Starting Web dev server in background...${NC}"
    nohup ./gradlew :composeApp:jsBrowserDevelopmentRun > web.log 2>&1 &
    WEB_PID=$!
    echo -e "${GREEN}✅ Web dev server starting (PID: $WEB_PID, logs: web.log)${NC}"
    echo -e "${CYAN}📍 Web app will be at: http://localhost:8080 (may take 30-60s to build)${NC}"
else
    echo -e "${YELLOW}⏭️  Skipped Web app${NC}"
fi
echo ""

# Step 5: Mobile Apps Info
echo -e "${BOLD}📱 Step 5/5: Mobile Apps${NC}"
echo -e "${CYAN}Android:${NC}"
echo -e "  1. Open Android Studio or start emulator"
echo -e "  2. Run: ${BOLD}just android${NC}"
echo -e "  3. Or: ${BOLD}./gradlew :composeApp:installDebug${NC}"

if [[ "$OSTYPE" == "darwin"* ]]; then
    echo -e "\n${CYAN}iOS:${NC}"
    echo -e "  1. Run: ${BOLD}just ios${NC}"
    echo -e "  2. Or manually: ${BOLD}open iosApp/iosApp.xcodeproj${NC}"
fi
echo ""

# Summary
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}${BOLD}🎉 Backend is ready!${NC}"
echo -e "${GREEN}${BOLD}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"

echo -e "${BOLD}📍 Running Services:${NC}"
echo -e "  ${CYAN}PocketBase:${NC}    http://localhost:8092"
echo -e "  ${CYAN}PocketBase Admin:${NC} http://localhost:8092/_/"
echo -e "  ${CYAN}Ktor Server:${NC}   http://localhost:8081"
if [ -n "$WEB_PID" ]; then
    echo -e "  ${CYAN}Web App:${NC}       http://localhost:8080 ${YELLOW}(building...)${NC}"
fi

echo ""
echo -e "${BOLD}🔑 Admin Credentials:${NC}"
echo -e "  Email:    tester_admin@bside.love"
echo -e "  Password: password123"

echo ""
echo -e "${BOLD}📊 Useful Commands:${NC}"
echo -e "  View PocketBase logs:  ${CYAN}docker logs -f bside-pocketbase${NC}"
echo -e "  View Server logs:      ${CYAN}docker logs -f bside-server${NC}"
echo -e "  Stop everything:       ${CYAN}just stop${NC}"
echo -e "  Restart backend:       ${CYAN}docker-compose restart${NC}"

echo ""
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
