#!/usr/bin/env bash
set -euo pipefail

# Bside Full Stack Startup Script
# This script starts the entire Bside stack with proper orchestration

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "🚀 Starting Bside Full Stack..."
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to check if a service is healthy
check_health() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo "⏳ Waiting for $service to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo -e "${GREEN}✅ $service is healthy!${NC}"
            return 0
        fi
        echo "   Attempt $attempt/$max_attempts..."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo -e "${RED}❌ $service failed to start${NC}"
    return 1
}

# Clean up function
cleanup() {
    echo ""
    echo "🛑 Stopping services..."
    docker-compose down
    exit 0
}

trap cleanup SIGINT SIGTERM

# Step 1: Stop any existing containers
echo "📦 Cleaning up existing containers..."
docker-compose down -v 2>/dev/null || true

# Step 2: Build the backend JAR
echo ""
echo "🏗️  Building Backend JAR..."
./gradlew :server:clean :server:shadowJar || {
    echo -e "${RED}❌ Failed to build server JAR${NC}"
    exit 1
}

# Step 3: Build Docker images
echo ""
echo "🔨 Building Docker images..."
docker-compose build

# Step 4: Start infrastructure services first (Redis)
echo ""
echo "🔧 Starting infrastructure services..."
docker-compose up -d redis

# Wait for Redis
check_health "Redis" "http://localhost:6379" || {
    echo "Redis failed to start, using ping instead..."
    docker-compose exec -T redis redis-cli ping > /dev/null || exit 1
}

# Step 5: Start PocketBase
echo ""
echo "📊 Starting PocketBase..."
docker-compose up -d pocketbase

# Wait for PocketBase to be healthy
check_health "PocketBase" "http://localhost:8092/api/health" || exit 1

# Step 6: Run migrations
echo ""
echo "🔄 Running database migrations..."
sleep 3  # Give PocketBase a moment to fully initialize

echo "   Migrations will run automatically on PocketBase startup"
echo "   Check logs: docker-compose logs pocketbase"

# Step 7: Start Backend Server
echo ""
echo "⚙️  Starting Ktor Backend Server..."
docker-compose up -d server

# Wait for Server
check_health "Backend Server" "http://localhost:8081/health" || exit 1

# Step 8: Start Nginx
echo ""
echo "🌐 Starting Nginx Reverse Proxy..."
docker-compose up -d nginx

# Wait for Nginx
check_health "Nginx" "http://localhost:8082/health" || exit 1

# Step 8: Start GoAccess (optional)
echo ""
echo "📊 Starting GoAccess Log Analyzer..."
docker-compose up -d goaccess || echo "⚠️  GoAccess optional, continuing..."

# Final status check
echo ""
echo "═══════════════════════════════════════════════════════"
echo -e "${GREEN}✅ Bside Full Stack is RUNNING!${NC}"
echo "═══════════════════════════════════════════════════════"
echo ""
echo "📋 Service URLs:"
echo "   🌐 Nginx (Main Entry):     http://localhost:8082"
echo "   ⚙️  Backend API:            http://localhost:8081"
echo "   📊 PocketBase:              http://localhost:8092"
echo "   🔧 PocketBase Admin:        http://localhost:8092/_/"
echo "   📈 GoAccess Dashboard:      http://localhost:7817"
echo ""
echo "📋 API Endpoints:"
echo "   PocketBase API:             http://localhost:8082/api/pb/"
echo "   Backend API:                http://localhost:8082/api/v1/"
echo "   File Uploads:               http://localhost:8082/api/pb/files/"
echo ""
echo "🧪 Test the stack:"
echo "   curl http://localhost:8082/health"
echo "   curl http://localhost:8082/api/pb/health"
echo ""
echo "📝 View logs:"
echo "   docker-compose logs -f [service]"
echo "   Available services: redis, pocketbase, server, nginx, goaccess"
echo ""
echo "🛑 Stop the stack:"
echo "   docker-compose down"
echo "   docker-compose down -v  (with volume cleanup)"
echo ""
echo "Press Ctrl+C to stop all services..."
echo ""

# Follow logs
docker-compose logs -f
