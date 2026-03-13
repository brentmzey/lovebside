#!/bin/bash

echo "╔═══════════════════════════════════════════════════════════════════════╗"
echo "║                                                                       ║"
echo "║              🧪 FULL STACK TEST - Everything Working?                ║"
echo "║                                                                       ║"
echo "╚═══════════════════════════════════════════════════════════════════════╝"
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

FAILED=0
PASSED=0

test_endpoint() {
  NAME=$1
  URL=$2
  EXPECTED=$3
  
  echo -n "Testing $NAME... "
  RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" "$URL" 2>/dev/null)
  
  if [ "$RESPONSE" = "$EXPECTED" ]; then
    echo -e "${GREEN}✅ PASS${NC} (HTTP $RESPONSE)"
    ((PASSED++))
  else
    echo -e "${RED}❌ FAIL${NC} (Expected $EXPECTED, got $RESPONSE)"
    ((FAILED++))
  fi
}

echo "=== Core Services ==="
test_endpoint "Nginx" "http://localhost:8082/" "200"
test_endpoint "Backend API" "http://localhost:8081/health" "200"
test_endpoint "PocketBase" "http://localhost:8092/api/health" "200"

# Redis (different test)
echo -n "Testing Redis... "
if docker exec bside-redis redis-cli PING | grep -q "PONG"; then
  echo -e "${GREEN}✅ PASS${NC}"
  ((PASSED++))
else
  echo -e "${RED}❌ FAIL${NC}"
  ((FAILED++))
fi

echo ""
echo "=== Monitoring Services ==="
test_endpoint "Prometheus" "http://localhost:9090/-/healthy" "200"
test_endpoint "Grafana" "http://localhost:3000/api/health" "200"
test_endpoint "Loki" "http://localhost:3100/ready" "200"
test_endpoint "Node Exporter" "http://localhost:9100/metrics" "200"
test_endpoint "cAdvisor" "http://localhost:8080/metrics" "200"
test_endpoint "Redis Exporter" "http://localhost:9121/metrics" "200"
test_endpoint "GoAccess" "http://localhost:7817/" "200"
test_endpoint "Redis Commander" "http://localhost:8083/" "200"

echo ""
echo "=== Grafana Configuration ==="
echo -n "Checking Grafana datasources... "
DS_COUNT=$(curl -s -u admin:admin http://localhost:3000/api/datasources | jq '. | length')
if [ "$DS_COUNT" -ge "2" ]; then
  echo -e "${GREEN}✅ $DS_COUNT datasources configured${NC}"
  ((PASSED++))
else
  echo -e "${YELLOW}⚠️  Only $DS_COUNT datasources (expected 2+)${NC}"
fi

echo ""
echo "=== Metrics Collection ==="
echo -n "Checking Prometheus targets... "
TARGETS=$(curl -s http://localhost:9090/api/v1/targets | jq '.data.activeTargets | length')
echo -e "${GREEN}$TARGETS targets${NC}"

echo -n "Checking if metrics are flowing... "
METRIC_COUNT=$(curl -s http://localhost:9090/api/v1/label/__name__/values | jq '.data | length')
if [ "$METRIC_COUNT" -gt "100" ]; then
  echo -e "${GREEN}✅ $METRIC_COUNT metrics available${NC}"
  ((PASSED++))
else
  echo -e "${RED}❌ Only $METRIC_COUNT metrics${NC}"
  ((FAILED++))
fi

echo ""
echo "=== Container Health ==="
RUNNING=$(docker-compose -f docker-compose.enhanced-lite.yml ps 2>/dev/null | grep -c "Up")
echo -e "Running containers: ${GREEN}$RUNNING/12${NC}"

if [ "$RUNNING" -eq "12" ]; then
  ((PASSED++))
else
  ((FAILED++))
fi

echo ""
echo "╔═══════════════════════════════════════════════════════════════════════╗"
echo "║                           TEST SUMMARY                                ║"
echo "╠═══════════════════════════════════════════════════════════════════════╣"
printf "║  %-20s %48s  ║\n" "Passed:" "${GREEN}$PASSED${NC}"
printf "║  %-20s %48s  ║\n" "Failed:" "${RED}$FAILED${NC}"
echo "╠═══════════════════════════════════════════════════════════════════════╣"

if [ $FAILED -eq 0 ]; then
  echo "║                                                                       ║"
  echo "║  ${GREEN}🎉 ALL TESTS PASSED - STACK IS FULLY OPERATIONAL! 🎉${NC}             ║"
  echo "║                                                                       ║"
  echo "╚═══════════════════════════════════════════════════════════════════════╝"
  echo ""
  echo "✨ Your observability stack is ready!"
  echo ""
  echo "Next steps:"
  echo "  1. Open Grafana: http://localhost:3000 (admin/admin)"
  echo "  2. Import dashboards (IDs: 1860, 763, 14282)"
  echo "  3. Read the walkthrough: cat .code-hq/OPTION_B_WALKTHROUGH.md"
  echo "  4. Plan next phase: cat .code-hq/OPTION_C_NEXT_PHASE.md"
  echo ""
  exit 0
else
  echo "║                                                                       ║"
  echo "║  ${RED}⚠️  SOME TESTS FAILED - CHECK LOGS ABOVE${NC}                          ║"
  echo "║                                                                       ║"
  echo "╚═══════════════════════════════════════════════════════════════════════╝"
  echo ""
  echo "Troubleshooting:"
  echo "  1. Check logs: docker-compose -f docker-compose.enhanced-lite.yml logs"
  echo "  2. Restart services: docker-compose -f docker-compose.enhanced-lite.yml restart"
  echo "  3. Full restart: docker-compose -f docker-compose.enhanced-lite.yml down && docker-compose -f docker-compose.enhanced-lite.yml up -d"
  echo ""
  exit 1
fi
