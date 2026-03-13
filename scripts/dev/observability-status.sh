#!/bin/bash
# Observability Stack Status and Management Script

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# Configuration
COMPOSE_FILE="docker-compose.observability.yml"

echo "╔══════════════════════════════════════════════════╗"
echo "║   Bside Observability Stack Status               ║"
echo "╚══════════════════════════════════════════════════╝"
echo ""

# Function to check if service is healthy
check_service() {
    local service=$1
    local port=$2
    local endpoint=$3
    
    echo -e "${CYAN}┌─ $service${NC}"
    
    if curl -sf "http://localhost:$port$endpoint" > /dev/null 2>&1; then
        echo -e "├─ ${GREEN}✓${NC} Service: Running"
        echo -e "├─ ${GREEN}✓${NC} Health: OK"
        echo -e "└─ URL: ${BLUE}http://localhost:$port${NC}"
    else
        echo -e "├─ ${RED}✗${NC} Service: Not responding"
        echo -e "└─ URL: ${YELLOW}http://localhost:$port${NC} (check if container is running)"
    fi
    echo ""
}

# Check Docker
echo -e "${BLUE}[1/9]${NC} Docker Status"
if docker info > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Docker is running"
else
    echo -e "${RED}✗${NC} Docker is not running"
    exit 1
fi
echo ""

# Check containers
echo -e "${BLUE}[2/9]${NC} Container Status"
docker-compose -f $COMPOSE_FILE ps --format "table {{.Name}}\t{{.Status}}\t{{.Ports}}" 2>/dev/null || \
    echo -e "${YELLOW}⚠${NC} Observability stack not started"
echo ""

# Check services
echo -e "${BLUE}[3/9]${NC} Service Health Checks"
echo ""

# Prometheus
check_service "Prometheus (Metrics)" 9090 "/-/healthy"

# Grafana  
check_service "Grafana (Dashboards)" 3000 "/api/health"

# Jaeger
check_service "Jaeger (Tracing)" 16686 "/"

# AlertManager (if running)
if docker ps | grep -q alertmanager; then
    check_service "AlertManager (Alerts)" 9093 "/-/healthy"
fi

# OpenTelemetry Collector (if running)
if docker ps | grep -q otel-collector; then
    check_service "OTEL Collector" 13133 "/"
fi

# Loki (if running)
if docker ps | grep -q loki; then
    check_service "Loki (Logs)" 3100 "/ready"
fi

# Tempo (if running)
if docker ps | grep -q tempo; then
    check_service "Tempo (Traces)" 3200 "/ready"
fi

echo -e "${BLUE}[4/9]${NC} Metrics Status"
if curl -sf http://localhost:9090/api/v1/query?query=up > /dev/null 2>&1; then
    TARGET_COUNT=$(curl -sf http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"health":"up"' | wc -l | tr -d ' ')
    echo -e "${GREEN}✓${NC} Prometheus is scraping metrics"
    echo -e "  └─ Active targets: $TARGET_COUNT"
else
    echo -e "${YELLOW}⚠${NC} Prometheus metrics not available"
fi
echo ""

echo -e "${BLUE}[5/9]${NC} Grafana Dashboards"
if curl -sf http://localhost:3000/api/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Grafana is accessible"
    echo -e "  ├─ URL: ${BLUE}http://localhost:3000${NC}"
    echo -e "  ├─ Default credentials: ${YELLOW}admin / admin${NC}"
    echo -e "  └─ ${CYAN}Tip: Change password on first login${NC}"
else
    echo -e "${YELLOW}⚠${NC} Grafana not accessible"
fi
echo ""

echo -e "${BLUE}[6/9]${NC} Distributed Tracing"
if curl -sf http://localhost:16686/ > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} Jaeger UI is accessible"
    echo -e "  ├─ URL: ${BLUE}http://localhost:16686${NC}"
    echo -e "  ├─ Collector: localhost:14268 (HTTP)"
    echo -e "  ├─ Collector: localhost:14250 (gRPC)"
    echo -e "  └─ Zipkin: localhost:9411"
else
    echo -e "${YELLOW}⚠${NC} Jaeger not accessible"
fi
echo ""

echo -e "${BLUE}[7/9]${NC} Resource Usage"
echo -e "${CYAN}Container Resources:${NC}"
docker stats --no-stream --format "table {{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}" \
    $(docker-compose -f $COMPOSE_FILE ps -q 2>/dev/null) 2>/dev/null | head -15
echo ""

echo -e "${BLUE}[8/9]${NC} Network Status"
if docker network inspect bside-network > /dev/null 2>&1; then
    CONTAINER_COUNT=$(docker network inspect bside-network -f '{{range .Containers}}{{.Name}} {{end}}' | wc -w | tr -d ' ')
    echo -e "${GREEN}✓${NC} Network 'bside-network' exists"
    echo -e "  └─ Connected containers: $CONTAINER_COUNT"
else
    echo -e "${YELLOW}⚠${NC} Network 'bside-network' not found"
fi
echo ""

echo -e "${BLUE}[9/9]${NC} Quick Access URLs"
echo ""
echo -e "📊 ${CYAN}Monitoring & Metrics:${NC}"
echo -e "   • Prometheus:       ${BLUE}http://localhost:9090${NC}"
echo -e "   • Grafana:          ${BLUE}http://localhost:3000${NC} (admin/admin)"
echo -e "   • Node Exporter:    ${BLUE}http://localhost:9100${NC}"
echo -e "   • cAdvisor:         ${BLUE}http://localhost:8084${NC}"
echo ""
echo -e "🔍 ${CYAN}Tracing & Logs:${NC}"
echo -e "   • Jaeger UI:        ${BLUE}http://localhost:16686${NC}"
if docker ps | grep -q loki; then
    echo -e "   • Loki (API):       ${BLUE}http://localhost:3100${NC}"
fi
if docker ps | grep -q tempo; then
    echo -e "   • Tempo (API):      ${BLUE}http://localhost:3200${NC}"
fi
echo ""
echo -e "🔔 ${CYAN}Alerting:${NC}"
if docker ps | grep -q alertmanager; then
    echo -e "   • AlertManager:     ${BLUE}http://localhost:9093${NC}"
fi
echo ""
echo -e "🏗️  ${CYAN}Infrastructure:${NC}"
echo -e "   • Redis UI:         ${BLUE}http://localhost:8083${NC}"
echo -e "   • Elasticsearch:    ${BLUE}http://localhost:9200${NC}"
echo -e "   • Kibana:           ${BLUE}http://localhost:5601${NC}"
echo ""
echo -e "🚀 ${CYAN}Application Services:${NC}"
echo -e "   • Backend API:      ${BLUE}http://localhost:8081${NC}"
echo -e "   • PocketBase:       ${BLUE}http://localhost:8092${NC}"
echo ""

# Management commands
echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
echo -e "${YELLOW}Management Commands:${NC}"
echo ""
echo -e "  ${GREEN}Start observability:${NC}    ./start-observability.sh development"
echo -e "  ${GREEN}Stop observability:${NC}     docker-compose -f $COMPOSE_FILE down"
echo -e "  ${GREEN}View logs:${NC}              docker-compose -f $COMPOSE_FILE logs -f [service]"
echo -e "  ${GREEN}Restart service:${NC}        docker-compose -f $COMPOSE_FILE restart [service]"
echo -e "  ${GREEN}Check status:${NC}           ./observability-status.sh"
echo ""
echo -e "${CYAN}═══════════════════════════════════════════════════${NC}"
