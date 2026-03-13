#!/usr/bin/env bash
set -euo pipefail

# ============================================================================
# Bside Complete System Startup
# Unified script to launch full stack + observability
# ============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
ENV="${1:-development}"
MODE="${2:-full}"  # full, app-only, observability-only

print_banner() {
    clear
    cat << "EOF"
    ╔═══════════════════════════════════════════════════════════════════╗
    ║                                                                   ║
    ║        ██████╗ ███████╗██╗██████╗ ███████╗                       ║
    ║        ██╔══██╗██╔════╝██║██╔══██╗██╔════╝                       ║
    ║        ██████╔╝███████╗██║██║  ██║█████╗                         ║
    ║        ██╔══██╗╚════██║██║██║  ██║██╔══╝                         ║
    ║        ██████╔╝███████║██║██████╔╝███████╗                       ║
    ║        ╚═════╝ ╚══════╝╚═╝╚═════╝ ╚══════╝                       ║
    ║                                                                   ║
    ║            Complete System Launcher v2.0                         ║
    ║                                                                   ║
    ╚═══════════════════════════════════════════════════════════════════╝

EOF
}

print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error() { echo -e "${RED}❌ $1${NC}"; }
print_warning() { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info() { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_step() { echo -e "${PURPLE}▶ $1${NC}"; }

# Check prerequisites
check_prerequisites() {
    print_step "Checking prerequisites..."
    
    local missing=0
    
    if ! command -v docker &> /dev/null; then
        print_error "Docker not installed"
        missing=1
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_error "Docker Compose not installed"
        missing=1
    fi
    
    if ! command -v java &> /dev/null; then
        print_error "Java not installed (needed for Gradle)"
        missing=1
    fi
    
    if [ $missing -eq 1 ]; then
        print_error "Prerequisites missing. Install required tools first."
        exit 1
    fi
    
    print_success "All prerequisites met"
}

# Clean up conflicting containers
cleanup_conflicts() {
    print_step "Cleaning up conflicts..."
    
    # Stop any orphaned containers
    docker ps -a --filter "name=bside-" --filter "status=exited" -q | xargs -r docker rm -f 2>/dev/null || true
    
    # Remove networks if empty
    docker network prune -f 2>/dev/null || true
    
    print_success "Cleanup complete"
}

# Build backend
build_backend() {
    print_step "Building backend (Ktor)..."
    
    if ./gradlew :server:clean :server:shadowJar --no-daemon --quiet; then
        print_success "Backend built"
    else
        print_error "Backend build failed"
        exit 1
    fi
}

# Build frontend
build_frontend() {
    print_step "Building frontend (Compose Web)..."
    
    if ./gradlew :composeApp:jsBrowserDevelopmentDistribution --no-daemon --quiet; then
        print_success "Frontend built"
    else
        print_warning "Frontend build failed (non-critical)"
    fi
}

# Start application stack
start_app_stack() {
    print_step "Starting application stack..."
    
    # Use the full compose file
    local COMPOSE_FILE="docker-compose.full.yml"
    
    # Stop existing
    docker-compose -f "$COMPOSE_FILE" down --remove-orphans 2>/dev/null || true
    
    # Start services in order
    print_info "Starting Redis..."
    docker-compose -f "$COMPOSE_FILE" up -d redis
    sleep 2
    
    print_info "Starting PocketBase..."
    docker-compose -f "$COMPOSE_FILE" up -d pocketbase
    sleep 3
    
    print_info "Starting Backend Server..."
    docker-compose -f "$COMPOSE_FILE" up -d server
    sleep 3
    
    print_info "Starting Nginx..."
    docker-compose -f "$COMPOSE_FILE" up -d nginx
    sleep 2
    
    print_success "Application stack started"
}

# Start observability stack (working services only)
start_observability_stack() {
    print_step "Starting observability stack..."
    
    local OBS_FILE="docker-compose.observability.yml"
    local ENV_FILE=".env.observability.$ENV"
    
    if [ ! -f "$ENV_FILE" ]; then
        print_warning "Environment file not found: $ENV_FILE"
        return 1
    fi
    
    # Start stable services only (skip otel-collector, jaeger, promtail for now)
    print_info "Starting Prometheus..."
    docker-compose -f "$OBS_FILE" --env-file "$ENV_FILE" up -d prometheus node-exporter redis-exporter cadvisor
    sleep 3
    
    print_info "Starting Grafana..."
    docker-compose -f "$OBS_FILE" --env-file "$ENV_FILE" up -d grafana
    sleep 3
    
    print_info "Starting Loki (logs)..."
    docker-compose -f "$OBS_FILE" --env-file "$ENV_FILE" up -d loki
    sleep 2
    
    print_info "Starting Tempo (traces)..."
    docker-compose -f "$OBS_FILE" --env-file "$ENV_FILE" up -d tempo
    sleep 2
    
    print_info "Starting AlertManager..."
    docker-compose -f "$OBS_FILE" --env-file "$ENV_FILE" up -d alertmanager
    
    print_success "Observability stack started"
}

# Health checks
check_health() {
    print_step "Running health checks..."
    
    local all_healthy=true
    
    # Check PocketBase
    if curl -sf http://localhost:8092/api/health > /dev/null 2>&1; then
        print_success "PocketBase: healthy"
    else
        print_warning "PocketBase: not responding"
        all_healthy=false
    fi
    
    # Check Backend
    if curl -sf http://localhost:8081/health > /dev/null 2>&1; then
        print_success "Backend: healthy"
    else
        print_warning "Backend: not responding"
        all_healthy=false
    fi
    
    # Check Nginx
    if curl -sf http://localhost:8082/health > /dev/null 2>&1; then
        print_success "Nginx: healthy"
    else
        print_warning "Nginx: not responding"
        all_healthy=false
    fi
    
    # Check Prometheus
    if curl -sf http://localhost:9090/-/healthy > /dev/null 2>&1; then
        print_success "Prometheus: healthy"
    else
        print_warning "Prometheus: not responding"
        all_healthy=false
    fi
    
    # Check Grafana
    if curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
        print_success "Grafana: healthy"
    else
        print_warning "Grafana: not responding"
        all_healthy=false
    fi
    
    if $all_healthy; then
        print_success "All services healthy!"
    else
        print_warning "Some services need attention"
    fi
}

# Print dashboard
print_dashboard() {
    echo ""
    echo -e "${GREEN}╔═══════════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║                                                                   ║${NC}"
    echo -e "${GREEN}║                 🎉 Bside Stack is RUNNING! 🎉                    ║${NC}"
    echo -e "${GREEN}║                                                                   ║${NC}"
    echo -e "${GREEN}╚═══════════════════════════════════════════════════════════════════╝${NC}"
    echo ""
    
    cat << EOF
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${CYAN}                    APPLICATION SERVICES                           ${NC}
${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

  ${GREEN}🌐 Main Application${NC}      http://localhost:8082
  ${GREEN}⚙️  Backend API${NC}           http://localhost:8081
  ${GREEN}📊 PocketBase${NC}            http://localhost:8092
  ${GREEN}🔧 PocketBase Admin${NC}      http://localhost:8092/_/
  ${GREEN}🗄️  Redis${NC}                localhost:6379

${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${YELLOW}                  MONITORING & OBSERVABILITY                      ${NC}
${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

  ${PURPLE}📊 Grafana (Dashboards)${NC}  http://localhost:3000 ${CYAN}(admin/admin)${NC}
  ${PURPLE}📈 Prometheus (Metrics)${NC}  http://localhost:9090
  ${PURPLE}📋 Loki (Logs)${NC}           http://localhost:3100
  ${PURPLE}🔍 Tempo (Traces)${NC}        http://localhost:3200
  ${PURPLE}🚨 AlertManager${NC}          http://localhost:9093
  ${PURPLE}📊 cAdvisor${NC}              http://localhost:8084

${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}                     USEFUL COMMANDS                               ${NC}
${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

  ${CYAN}View all logs:${NC}           docker-compose -f docker-compose.full.yml logs -f
  ${CYAN}View service logs:${NC}       docker-compose -f docker-compose.full.yml logs -f [service]
  ${CYAN}Stop everything:${NC}         docker-compose -f docker-compose.full.yml down
  ${CYAN}Restart service:${NC}         docker-compose -f docker-compose.full.yml restart [service]
  ${CYAN}Check status:${NC}            docker-compose -f docker-compose.full.yml ps

${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}
${BLUE}                      QUICK TESTS                                  ${NC}
${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

  ${GREEN}curl http://localhost:8082/health${NC}
  ${GREEN}curl http://localhost:8081/health${NC}
  ${GREEN}curl http://localhost:8092/api/health${NC}

${YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}

${GREEN}✨ Open Grafana at http://localhost:3000 to see real-time metrics!${NC}
${GREEN}✨ Your messaging backend is ready for real-time connections!${NC}

${YELLOW}Press Ctrl+C to stop, or use: docker-compose -f docker-compose.full.yml down${NC}

EOF
}

# Main execution
main() {
    print_banner
    
    print_info "Starting Bside Complete System"
    print_info "Environment: $ENV"
    print_info "Mode: $MODE"
    echo ""
    
    check_prerequisites
    cleanup_conflicts
    
    if [ "$MODE" == "observability-only" ]; then
        start_observability_stack
    elif [ "$MODE" == "app-only" ]; then
        build_backend
        build_frontend
        start_app_stack
    else
        # Full mode
        build_backend
        build_frontend
        start_app_stack
        sleep 2
        start_observability_stack
    fi
    
    echo ""
    sleep 3
    check_health
    
    echo ""
    print_dashboard
    
    # Follow logs
    print_info "Following logs (Ctrl+C to exit)..."
    sleep 2
    docker-compose -f docker-compose.full.yml logs -f
}

# Handle Ctrl+C gracefully
trap 'echo -e "\n${YELLOW}Stopping log stream... Stack is still running.${NC}\n"; exit 0' INT

# Run
main
