#!/bin/bash

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

PID_DIR=".pids"
STOPPED_SOMETHING=false

echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}🛑 Stopping all B-Side processes${NC}"
echo -e "${BLUE}========================================${NC}\n"

# Stop processes based on PID files
if [ -d "$PID_DIR" ]; then
    for pid_file in "$PID_DIR"/*.pid; do
        if [ -f "$pid_file" ]; then
            pid=$(cat "$pid_file")
            process_name=$(basename "$pid_file" .pid)
            if kill -0 "$pid" 2>/dev/null; then
                echo -e "${RED}🛑 Stopping $process_name (PID: $pid)...${NC}"
                kill "$pid" 2>/dev/null || kill -9 "$pid" 2>/dev/null
                STOPPED_SOMETHING=true
            fi
            rm -f "$pid_file"
        fi
    done
fi

# Fallback: Kill anything on the server port
if lsof -ti:8080 >/dev/null 2>&1; then
    echo -e "${RED}📡 Killing processes on port 8080 (fallback)...${NC}"
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    STOPPED_SOMETHING=true
fi

# Stop Gradle daemons
if pgrep -f "GradleDaemon" >/dev/null 2>&1; then
    echo -e "${RED}🐘 Stopping Gradle daemons...${NC}"
    ./gradlew --stop > /dev/null 2>&1 || true
    STOPPED_SOMETHING=true
fi

if [ "$STOPPED_SOMETHING" = true ]; then
    echo -e "\n${GREEN}✅ All B-Side processes have been stopped.${NC}"
else
    echo -e "${YELLOW}ℹ️ No running B-Side processes found.${NC}"
fi

echo -e "${BLUE}========================================${NC}\n"