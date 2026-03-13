#!/bin/bash
# Complete Observability Stack Startup Script
# Supports: development, staging, production

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_NAME="${1:-development}"

# Support both formats: "development" or ".env.observability.development"
if [[ "$ENV_NAME" == .env.observability.* ]]; then
    ENV_FILE="$ENV_NAME"
else
    ENV_FILE=".env.observability.${ENV_NAME}"
fi

DOCKER_COMPOSE_FILE="docker-compose.observability.yml"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed"
        exit 1
    fi
    
    if [ ! -f "$ENV_FILE" ]; then
        log_error "Environment file not found: $ENV_FILE"
        log_info "Available environment files:"
        ls -1 .env.observability.* 2>/dev/null || echo "No environment files found"
        log_info ""
        log_info "Usage: $0 [development|staging|production]"
        log_info "Example: $0 development"
        exit 1
    fi
    
    log_success "Prerequisites check passed"
}

# Create required directories
create_directories() {
    log_info "Creating required directories..."
    
    mkdir -p \
        observability/{otel,prometheus,loki,promtail,tempo,alertmanager,grafana/{provisioning/{datasources,dashboards,notifiers},dashboards}} \
        logs
    
    log_success "Directories created"
}

# Create Docker network
create_network() {
    log_info "Creating Docker network..."
    
    if ! docker network inspect bside-network &> /dev/null; then
        docker network create bside-network
        log_success "Created bside-network"
    else
        log_info "Network bside-network already exists"
    fi
}

# Start observability stack
start_stack() {
    log_info "Starting observability stack..."
    log_info "Using environment file: $ENV_FILE"
    
    docker-compose -f "$DOCKER_COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    
    log_success "Observability stack started"
}

# Wait for services to be healthy
wait_for_services() {
    log_info "Waiting for services to be healthy..."
    
    local services=(
        "otel-collector:13133"
        "prometheus:9090"
        "grafana:3000"
        "loki:3100"
        "tempo:3200"
        "jaeger:16686"
    )
    
    for service_endpoint in "${services[@]}"; do
        IFS=':' read -r service port <<< "$service_endpoint"
        
        log_info "Waiting for $service..."
        timeout=60
        elapsed=0
        
        while [ $elapsed -lt $timeout ]; do
            if curl -sf "http://localhost:$port" &> /dev/null || \
               curl -sf "http://localhost:$port/health" &> /dev/null || \
               curl -sf "http://localhost:$port/ready" &> /dev/null || \
               curl -sf "http://localhost:$port/-/healthy" &> /dev/null; then
                log_success "$service is ready"
                break
            fi
            
            sleep 2
            elapsed=$((elapsed + 2))
        done
        
        if [ $elapsed -ge $timeout ]; then
            log_warning "$service may not be ready (timeout reached)"
        fi
    done
}

# Display service URLs
display_urls() {
    log_info "Observability Stack URLs:"
    echo ""
    echo -e "${GREEN}Visualization & Dashboards:${NC}"
    echo "  Grafana:          http://localhost:3000 (admin/admin)"
    echo "  Jaeger UI:        http://localhost:16686"
    echo ""
    echo -e "${GREEN}Metrics & Monitoring:${NC}"
    echo "  Prometheus:       http://localhost:9090"
    echo "  AlertManager:     http://localhost:9093"
    echo "  cAdvisor:         http://localhost:8084"
    echo ""
    echo -e "${GREEN}Logs:${NC}"
    echo "  Loki:             http://localhost:3100"
    echo ""
    echo -e "${GREEN}Tracing:${NC}"
    echo "  Tempo:            http://localhost:3200"
    echo "  OTLP gRPC:        localhost:4317"
    echo "  OTLP HTTP:        localhost:4318"
    echo ""
    echo -e "${GREEN}Exporters:${NC}"
    echo "  Node Exporter:    http://localhost:9100"
    echo "  Redis Exporter:   http://localhost:9121"
    echo ""
}

# Display quick start guide
display_quick_start() {
    echo ""
    log_info "Quick Start Commands:"
    echo ""
    echo "  View logs:        docker-compose -f $DOCKER_COMPOSE_FILE logs -f"
    echo "  Stop stack:       docker-compose -f $DOCKER_COMPOSE_FILE down"
    echo "  Restart:          docker-compose -f $DOCKER_COMPOSE_FILE restart"
    echo "  Status:           docker-compose -f $DOCKER_COMPOSE_FILE ps"
    echo ""
    echo "  Import dashboards: ./scripts/import-grafana-dashboards.sh"
    echo "  Test alerts:       ./scripts/test-alerts.sh"
    echo "  Health check:      ./scripts/health-check-observability.sh"
    echo ""
}

# Main execution
main() {
    clear
    echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║   Bside Observability Stack Launcher            ║${NC}"
    echo -e "${BLUE}║   Complete Monitoring, Logging, and Tracing     ║${NC}"
    echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
    echo ""
    
    check_prerequisites
    create_directories
    create_network
    start_stack
    wait_for_services
    
    echo ""
    log_success "Observability stack is running!"
    echo ""
    
    display_urls
    display_quick_start
    
    log_info "All services are accessible. Happy monitoring!"
}

# Run main function
main "$@"
