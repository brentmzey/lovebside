#!/bin/bash

# Observability Stack Verification Script
# Checks health and connectivity of all observability services

set -e

COLOR_GREEN='\033[0;32m'
COLOR_RED='\033[0;31m'
COLOR_YELLOW='\033[1;33m'
COLOR_BLUE='\033[0;34m'
COLOR_RESET='\033[0m'

echo -e "${COLOR_BLUE}════════════════════════════════════════════════════${COLOR_RESET}"
echo -e "${COLOR_BLUE}   Observability Stack Verification${COLOR_RESET}"
echo -e "${COLOR_BLUE}════════════════════════════════════════════════════${COLOR_RESET}"
echo ""

# Function to check if service is responding
check_service() {
    local name=$1
    local url=$2
    local expected=$3
    
    echo -n "Checking $name... "
    
    if curl -s -f "$url" > /dev/null 2>&1; then
        echo -e "${COLOR_GREEN}✓ OK${COLOR_RESET}"
        return 0
    else
        echo -e "${COLOR_RED}✗ FAILED${COLOR_RESET}"
        return 1
    fi
}

# Function to check Docker container
check_container() {
    local name=$1
    
    echo -n "Checking container $name... "
    
    if docker ps --format '{{.Names}}' | grep -q "^${name}$"; then
        local status=$(docker inspect --format='{{.State.Health.Status}}' "$name" 2>/dev/null || echo "running")
        if [[ "$status" == "healthy" || "$status" == "running" ]]; then
            echo -e "${COLOR_GREEN}✓ Running${COLOR_RESET}"
            return 0
        else
            echo -e "${COLOR_YELLOW}⚠ Unhealthy (${status})${COLOR_RESET}"
            return 1
        fi
    else
        echo -e "${COLOR_RED}✗ Not Running${COLOR_RESET}"
        return 1
    fi
}

total_checks=0
passed_checks=0

echo -e "${COLOR_YELLOW}1. Container Health Checks${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

containers=(
    "grafana"
    "prometheus"
    "jaeger"
    "loki"
    "promtail"
    "otel-collector"
    "tempo"
    "alertmanager"
)

for container in "${containers[@]}"; do
    ((total_checks++))
    if check_container "$container"; then
        ((passed_checks++))
    fi
done

echo ""
echo -e "${COLOR_YELLOW}2. Service Endpoint Checks${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Grafana
((total_checks++))
if check_service "Grafana UI" "http://localhost:3000/api/health" "200"; then
    ((passed_checks++))
fi

# Prometheus
((total_checks++))
if check_service "Prometheus API" "http://localhost:9090/-/healthy" "200"; then
    ((passed_checks++))
fi

# Jaeger
((total_checks++))
if check_service "Jaeger UI" "http://localhost:16686/" "200"; then
    ((passed_checks++))
fi

# Loki
((total_checks++))
if check_service "Loki API" "http://localhost:3100/ready" "200"; then
    ((passed_checks++))
fi

# OpenTelemetry Collector
((total_checks++))
if check_service "OTEL Collector" "http://localhost:13133/" "200"; then
    ((passed_checks++))
fi

# Tempo
((total_checks++))
if check_service "Tempo API" "http://localhost:3200/ready" "200"; then
    ((passed_checks++))
fi

# AlertManager
((total_checks++))
if check_service "AlertManager API" "http://localhost:9093/-/healthy" "200"; then
    ((passed_checks++))
fi

echo ""
echo -e "${COLOR_YELLOW}3. Data Flow Verification${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# Check Prometheus targets
echo -n "Checking Prometheus targets... "
((total_checks++))
targets=$(curl -s http://localhost:9090/api/v1/targets 2>/dev/null | grep -o '"health":"up"' | wc -l)
if [ "$targets" -gt 0 ]; then
    echo -e "${COLOR_GREEN}✓ Found ${targets} active targets${COLOR_RESET}"
    ((passed_checks++))
else
    echo -e "${COLOR_RED}✗ No active targets${COLOR_RESET}"
fi

# Check Grafana datasources
echo -n "Checking Grafana datasources... "
((total_checks++))
datasources=$(curl -s -u admin:admin123 http://localhost:3000/api/datasources 2>/dev/null | grep -o '"name"' | wc -l)
if [ "$datasources" -gt 0 ]; then
    echo -e "${COLOR_GREEN}✓ Found ${datasources} datasources${COLOR_RESET}"
    ((passed_checks++))
else
    echo -e "${COLOR_RED}✗ No datasources configured${COLOR_RESET}"
fi

# Check OpenTelemetry Collector metrics
echo -n "Checking OTEL Collector metrics... "
((total_checks++))
otel_metrics=$(curl -s http://localhost:8888/metrics 2>/dev/null | grep -c "otelcol_" || echo "0")
if [ "$otel_metrics" -gt 0 ]; then
    echo -e "${COLOR_GREEN}✓ Collector is processing data${COLOR_RESET}"
    ((passed_checks++))
else
    echo -e "${COLOR_YELLOW}⚠ No metrics found (may be normal on fresh start)${COLOR_RESET}"
fi

echo ""
echo -e "${COLOR_YELLOW}4. Configuration Files${COLOR_RESET}"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

configs=(
    "observability/prometheus/prometheus.yml"
    "observability/prometheus/alerts.yml"
    "observability/loki/loki-config.yaml"
    "observability/promtail/promtail-config.yaml"
    "observability/otel/otel-collector-config.yaml"
    "observability/tempo/tempo.yaml"
    "observability/alertmanager/alertmanager.yml"
    "docker-compose.observability.yml"
)

for config in "${configs[@]}"; do
    echo -n "Checking $config... "
    ((total_checks++))
    if [ -f "$config" ]; then
        echo -e "${COLOR_GREEN}✓ Exists${COLOR_RESET}"
        ((passed_checks++))
    else
        echo -e "${COLOR_RED}✗ Missing${COLOR_RESET}"
    fi
done

echo ""
echo -e "${COLOR_BLUE}════════════════════════════════════════════════════${COLOR_RESET}"
echo -e "${COLOR_BLUE}   Summary${COLOR_RESET}"
echo -e "${COLOR_BLUE}════════════════════════════════════════════════════${COLOR_RESET}"
echo ""

percentage=$((passed_checks * 100 / total_checks))

echo "Total Checks: $total_checks"
echo "Passed: $passed_checks"
echo "Failed: $((total_checks - passed_checks))"
echo -e "Success Rate: ${percentage}%"
echo ""

if [ $percentage -eq 100 ]; then
    echo -e "${COLOR_GREEN}✓ All checks passed! Observability stack is fully operational.${COLOR_RESET}"
    echo ""
    echo "Access your services:"
    echo "  • Grafana:      http://localhost:3000 (admin/admin123)"
    echo "  • Prometheus:   http://localhost:9090"
    echo "  • Jaeger:       http://localhost:16686"
    echo "  • AlertManager: http://localhost:9093"
    exit 0
elif [ $percentage -ge 80 ]; then
    echo -e "${COLOR_YELLOW}⚠ Most checks passed, but some issues detected.${COLOR_RESET}"
    echo "  Review the failures above and check logs:"
    echo "  docker-compose -f docker-compose.observability.yml logs"
    exit 1
else
    echo -e "${COLOR_RED}✗ Multiple checks failed. Stack may not be running properly.${COLOR_RESET}"
    echo ""
    echo "Suggested actions:"
    echo "  1. Ensure Docker is running"
    echo "  2. Start the stack: ./start-observability.sh development"
    echo "  3. Check logs: docker-compose -f docker-compose.observability.yml logs"
    echo "  4. Verify ports are not in use: netstat -tuln | grep -E '(3000|9090|16686)'"
    exit 2
fi
