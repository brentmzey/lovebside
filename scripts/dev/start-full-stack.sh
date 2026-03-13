#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Bside Full Stack Orchestration Script
# ============================================================================
# 
# This script orchestrates the complete Bside stack with:
# - Database (PocketBase) with migrations
# - Backend API (Ktor Server)
# - Cache/Queue (Redis)
# - Reverse Proxy (Nginx)
# - Monitoring (Prometheus + Grafana + GoAccess)
# - Health checks and graceful startup
#
# Usage:
#   ./start-full-stack.sh                 # Start with monitoring
#   ./start-full-stack.sh --basic         # Start without monitoring
#   ./start-full-stack.sh --production    # Use production compose file
#   ./start-full-stack.sh --stop          # Stop all services
#   ./start-full-stack.sh --restart       # Restart all services
#   ./start-full-stack.sh --logs [service]# Follow logs
#
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Configuration
COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.full.yml}"
MODE="full"
ACTION="start"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --basic)
            COMPOSE_FILE="docker-compose.yml"
            MODE="basic"
            shift
            ;;
        --production)
            COMPOSE_FILE="docker-compose.production.yml"
            MODE="production"
            shift
            ;;
        --stop)
            ACTION="stop"
            shift
            ;;
        --restart)
            ACTION="restart"
            shift
            ;;
        --logs)
            ACTION="logs"
            SERVICE="${2:-}"
            shift 2
            ;;
        --help|-h)
            cat << EOF
Bside Full Stack Orchestration

Usage:
  $0 [OPTIONS]

Options:
  --basic         Use basic stack (no monitoring)
  --production    Use production stack configuration
  --stop          Stop all services
  --restart       Restart all services
  --logs [svc]    Follow logs (optionally specify service)
  --help, -h      Show this help message

Examples:
  $0                          # Start full stack with monitoring
  $0 --basic                  # Start without monitoring stack
  $0 --logs server            # View server logs
  $0 --stop                   # Stop everything

EOF
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            exit 1
            ;;
    esac
done

# Functions
print_header() {
    echo ""
    echo -e "${PURPLE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${PURPLE}  $1${NC}"
    echo -e "${PURPLE}═══════════════════════════════════════════════════════${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

check_health() {
    local service=$1
    local url=$2
    local max_attempts=30
    local attempt=1

    echo -n "⏳ Waiting for $service to be healthy..."
    
    while [ $attempt -le $max_attempts ]; do
        if curl -sf "$url" > /dev/null 2>&1; then
            echo ""
            print_success "$service is healthy!"
            return 0
        fi
        echo -n "."
        sleep 2
        attempt=$((attempt + 1))
    done

    echo ""
    print_error "$service failed to start (timeout)"
    return 1
}

check_prerequisites() {
    print_header "Checking Prerequisites"
    
    local missing=0
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker is not installed"
        missing=1
    else
        print_success "Docker is installed"
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose is not installed"
        missing=1
    else
        print_success "Docker Compose is installed"
    fi
    
    if [ ! -f ".env" ]; then
        print_warning ".env file not found, copying from .env.example"
        cp .env.example .env
    else
        print_success ".env file exists"
    fi
    
    if [ $missing -eq 1 ]; then
        print_error "Missing prerequisites. Please install required tools."
        exit 1
    fi
}

cleanup_conflicts() {
    print_header "Sanitizing Environment"
    
    local NETWORK_NAME="bside_bside-network"
    
    # Check if our target network exists
    if docker network inspect "$NETWORK_NAME" >/dev/null 2>&1; then
        # Find any containers (zombies) still attached to it
        local ZOMBIES=$(docker network inspect "$NETWORK_NAME" -f '{{range .Containers}}{{.Name}} {{end}}')
        
        if [ -n "$ZOMBIES" ]; then
            print_warning "Found lingering containers locking the network: $ZOMBIES"
            print_info "Force removing zombies to clear the path..."
            # Force remove them
            echo "$ZOMBIES" | xargs docker rm -f >/dev/null 2>&1
            print_success "Conflicts resolved"
        fi
    fi
}

build_backend() {
    print_header "Building Backend"
    
    print_info "Building Ktor backend JAR..."
    if ./gradlew :server:clean :server:shadowJar --no-daemon; then
        print_success "Backend JAR built successfully"
    else
        print_error "Failed to build backend JAR"
        exit 1
    fi
}

build_frontend() {
    print_header "Building Frontend (Web)"
    
    print_info "Building Compose Web App (JS - Development)..."
    if ./gradlew :composeApp:jsBrowserDevelopmentDistribution --no-daemon; then
        print_success "Frontend Web App built successfully"
    else
        print_error "Failed to build Frontend Web App"
        exit 1
    fi
}

start_stack() {
    print_header "Starting Bside Stack ($MODE mode)"
    
    # Pre-flight cleanup
    cleanup_conflicts
    
    # Stop any existing containers
    print_info "Stopping existing containers..."
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    
    # Build Docker images
    print_info "Building Docker images..."
    docker-compose -f "$COMPOSE_FILE" build
    
    # Start infrastructure services
    print_header "Starting Infrastructure Layer"
    
    print_info "Starting Redis..."
    docker-compose -f "$COMPOSE_FILE" up -d redis
    sleep 3
    docker-compose -f "$COMPOSE_FILE" exec -T redis redis-cli ping > /dev/null && print_success "Redis is running" || print_error "Redis failed to start"
    
    # Start database
    print_header "Starting Database Layer"
    
    print_info "Starting PocketBase..."
    docker-compose -f "$COMPOSE_FILE" up -d pocketbase
    check_health "PocketBase" "http://localhost:8092/api/health" || exit 1
    
    print_info "Running database migrations..."
    sleep 2
    print_info "Migrations run automatically on PocketBase startup"
    
    # Start backend
    print_header "Starting Application Layer"
    
    print_info "Starting Backend Server..."
    docker-compose -f "$COMPOSE_FILE" up -d server
    check_health "Backend Server" "http://localhost:8081/health" || exit 1
    
    # Start reverse proxy
    print_header "Starting Routing Layer"
    
    print_info "Starting Nginx..."
    docker-compose -f "$COMPOSE_FILE" up -d nginx
    check_health "Nginx" "http://localhost:8082/health" || exit 1
    
    # Start monitoring (if full mode)
    if [ "$MODE" = "full" ]; then
        print_header "Starting Monitoring Layer"
        
        print_info "Starting Prometheus..."
        docker-compose -f "$COMPOSE_FILE" up -d prometheus redis-exporter node-exporter cadvisor
        sleep 3
        
        print_info "Starting Elasticsearch & Kibana..."
        docker-compose -f "$COMPOSE_FILE" up -d elasticsearch kibana
        
        print_info "Starting Jaeger & OTEL Collector..."
        docker-compose -f "$COMPOSE_FILE" up -d jaeger otel-collector
        
        print_info "Starting Grafana..."
        docker-compose -f "$COMPOSE_FILE" up -d grafana
        sleep 3
        
        print_info "Starting GoAccess..."
        docker-compose -f "$COMPOSE_FILE" up -d goaccess
        
        print_info "Starting Redis Commander..."
        docker-compose -f "$COMPOSE_FILE" up -d redis-commander
    fi
    
    # Print status
    print_success_dashboard
}

print_success_dashboard() {
    print_header "Bside Stack is RUNNING! 🎉"
    
    cat << EOF
${GREEN}Core Services:${NC}
  🌐 Main Application:        http://localhost:8082
  ⚙️  Backend API:             http://localhost:8081
  📊 PocketBase:               http://localhost:8092
  🔧 PocketBase Admin:         http://localhost:8092/_/
  🗄️  Redis:                   localhost:6379

${CYAN}API Endpoints:${NC}
  Backend API:                http://localhost:8082/api/v1/
  PocketBase API:             http://localhost:8082/api/pb/
  Health Check:               http://localhost:8082/health
  File Uploads:               http://localhost:8082/api/pb/files/

EOF

    if [ "$MODE" = "full" ]; then
        cat << EOF
${PURPLE}Monitoring & Observability:${NC}
  📊 Grafana (Dashboards):     http://localhost:3000 (admin/admin)
  📈 Prometheus (Metrics):     http://localhost:9090
  🔍 Kibana (Logs/Search):     http://localhost:5601
  🕸️  Jaeger (Tracing):        http://localhost:16686
  📊 GoAccess (Logs):          http://localhost:7817
  🗄️  Redis Commander:         http://localhost:8083

${YELLOW}Metrics Endpoints:${NC}
  Backend Metrics:            http://localhost:8081/metrics
  Redis Metrics:              http://localhost:9121/metrics
  Node Metrics:               http://localhost:9100/metrics
  Container Metrics:          http://localhost:8084/metrics
  OTEL Metrics:               http://localhost:8888/metrics
  
EOF
    fi

    cat << EOF
${BLUE}Useful Commands:${NC}
  View logs:                  docker-compose -f $COMPOSE_FILE logs -f [service]
  Stop stack:                 docker-compose -f $COMPOSE_FILE down
  Restart service:            docker-compose -f $COMPOSE_FILE restart [service]
  View status:                docker-compose -f $COMPOSE_FILE ps

${BLUE}Quick Tests:${NC}
  curl http://localhost:8082/health
  curl http://localhost:8082/api/pb/health
  curl http://localhost:8081/health

${YELLOW}Press Ctrl+C to stop following logs, or use:${NC}
  $0 --stop

EOF
}

stop_stack() {
    print_header "Stopping Bside Stack"
    
    print_info "Stopping all services..."
    docker-compose -f "$COMPOSE_FILE" down
    
    print_success "Stack stopped successfully"
}

restart_stack() {
    print_header "Restarting Bside Stack"
    
    print_info "Restarting all services..."
    docker-compose -f "$COMPOSE_FILE" restart
    
    print_success "Stack restarted successfully"
}

follow_logs() {
    print_header "Following Logs"
    
    if [ -n "$SERVICE" ]; then
        print_info "Following logs for: $SERVICE"
        docker-compose -f "$COMPOSE_FILE" logs -f "$SERVICE"
    else
        print_info "Following logs for all services"
        docker-compose -f "$COMPOSE_FILE" logs -f
    fi
}

# Main execution
case "$ACTION" in
    start)
        check_prerequisites
        build_backend
        build_frontend
        start_stack
        follow_logs
        ;;
    stop)
        stop_stack
        ;;
    restart)
        restart_stack
        ;;
    logs)
        follow_logs
        ;;
    *)
        print_error "Unknown action: $ACTION"
        exit 1
        ;;
esac
