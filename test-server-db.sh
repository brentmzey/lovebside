#!/bin/bash
# Test server and database connectivity

set -e

echo "========================================"
echo "B-Side Server & Database Test"
echo "========================================"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if ports are available
check_port() {
    local port=$1
    if lsof -ti:$port > /dev/null 2>&1; then
        echo -e "${RED}✗ Port $port is already in use${NC}"
        echo "  Kill the process with: kill -9 \$(lsof -ti:$port)"
        return 1
    fi
    return 0
}

# Start PocketBase
start_pocketbase() {
    echo "1. Starting PocketBase on port 8090..."
    cd pocketbase
    if [ ! -f "./pocketbase" ]; then
        echo -e "${RED}✗ PocketBase binary not found${NC}"
        echo "  Download from: https://pocketbase.io/docs/"
        exit 1
    fi
    
    ./pocketbase serve --http=0.0.0.0:8090 > ../pocketbase.log 2>&1 &
    POCKETBASE_PID=$!
    cd ..
    
    # Wait for PocketBase to start
    echo "  Waiting for PocketBase to start..."
    for i in {1..10}; do
        if curl -s http://localhost:8090/api/health > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓ PocketBase started (PID: $POCKETBASE_PID)${NC}"
            return 0
        fi
        sleep 1
    done
    
    echo -e "  ${YELLOW}⚠ PocketBase may not be fully ready yet${NC}"
    return 0
}

# Build and start server
start_server() {
    echo ""
    echo "2. Building Ktor server..."
    ./gradlew :server:build -x test --quiet
    if [ $? -ne 0 ]; then
        echo -e "${RED}✗ Server build failed${NC}"
        return 1
    fi
    echo -e "  ${GREEN}✓ Server built${NC}"
    
    echo "  Starting server on port 8080..."
    ./gradlew :server:run > server.log 2>&1 &
    SERVER_PID=$!
    
    # Wait for server to start
    echo "  Waiting for server to start..."
    for i in {1..15}; do
        if curl -s http://localhost:8080/health > /dev/null 2>&1; then
            echo -e "  ${GREEN}✓ Server started (PID: $SERVER_PID)${NC}"
            return 0
        fi
        sleep 1
    done
    
    echo -e "  ${YELLOW}⚠ Server may not be fully ready yet${NC}"
    return 0
}

# Test endpoints
test_endpoints() {
    echo ""
    echo "3. Testing API endpoints..."
    
    # Health check
    echo -n "  Health endpoint... "
    if curl -s http://localhost:8080/health | grep -q "ok"; then
        echo -e "${GREEN}✓ PASS${NC}"
    else
        echo -e "${RED}✗ FAIL${NC}"
    fi
    
    # API root
    echo -n "  API root... "
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/)
    if [ "$STATUS" = "200" ] || [ "$STATUS" = "404" ]; then
        echo -e "${GREEN}✓ PASS${NC}"
    else
        echo -e "${RED}✗ FAIL (HTTP $STATUS)${NC}"
    fi
}

# Cleanup
cleanup() {
    echo ""
    echo "4. Cleaning up..."
    
    if [ ! -z "$SERVER_PID" ]; then
        echo "  Stopping server (PID: $SERVER_PID)..."
        kill $SERVER_PID 2>/dev/null || true
        wait $SERVER_PID 2>/dev/null || true
    fi
    
    if [ ! -z "$POCKETBASE_PID" ]; then
        echo "  Stopping PocketBase (PID: $POCKETBASE_PID)..."
        kill $POCKETBASE_PID 2>/dev/null || true
        wait $POCKETBASE_PID 2>/dev/null || true
    fi
    
    # Also kill any remaining processes on these ports
    lsof -ti:8080 | xargs kill -9 2>/dev/null || true
    lsof -ti:8090 | xargs kill -9 2>/dev/null || true
    
    echo -e "${GREEN}✓ Cleanup complete${NC}"
    
    echo ""
    echo "Logs saved to:"
    echo "  - server.log"
    echo "  - pocketbase.log"
}

# Set trap to cleanup on exit
trap cleanup EXIT INT TERM

# Main execution
main() {
    # Check ports
    if ! check_port 8080; then
        exit 1
    fi
    
    if ! check_port 8090; then
        exit 1
    fi
    
    # Start services
    start_pocketbase
    start_server
    
    # Test
    test_endpoints
    
    echo ""
    echo "========================================"
    echo "Test complete!"
    echo "========================================"
    echo ""
    echo "Press Enter to stop services and exit..."
    read
}

main
