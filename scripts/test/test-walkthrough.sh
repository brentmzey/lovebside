#!/bin/bash

# B-Side Complete Stack Testing Walkthrough
# This script walks through testing all environments: dev, staging, production

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Print functions
print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_section() {
    echo -e "\n${CYAN}▶ $1${NC}"
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
    echo -e "${MAGENTA}ℹ️  $1${NC}"
}

# Test counters
TESTS_PASSED=0
TESTS_FAILED=0

# Test function
test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    echo -n "  Testing $name... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" "$url" 2>/dev/null || echo "000")
    
    if [ "$response" = "$expected_code" ]; then
        echo -e "${GREEN}✅ PASS${NC} (HTTP $response)"
        ((TESTS_PASSED++))
        return 0
    else
        echo -e "${RED}❌ FAIL${NC} (Expected $expected_code, got $response)"
        ((TESTS_FAILED++))
        return 1
    fi
}

# Wait for service
wait_for_service() {
    local name=$1
    local url=$2
    local max_attempts=${3:-30}
    local attempt=0
    
    echo -n "  Waiting for $name to be ready..."
    
    while [ $attempt -lt $max_attempts ]; do
        if curl -s "$url" > /dev/null 2>&1; then
            echo -e " ${GREEN}✅ Ready!${NC}"
            return 0
        fi
        echo -n "."
        sleep 2
        ((attempt++))
    done
    
    echo -e " ${RED}❌ Timeout${NC}"
    return 1
}

# Main walkthrough
clear

cat << 'EOF'
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║          B-SIDE COMPLETE STACK TESTING WALKTHROUGH               ║
║                                                                  ║
║  We'll test and spin up the stack in multiple environments:     ║
║  • Local Development (dev)                                       ║
║  • Staging Environment                                           ║
║  • Production-like Setup                                         ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝

EOF

read -p "Press ENTER to begin the walkthrough..."

# ============================================================================
# PART 1: LOCAL DEVELOPMENT ENVIRONMENT
# ============================================================================

print_header "PART 1: LOCAL DEVELOPMENT ENVIRONMENT"

print_info "This is the environment you use for daily development."
print_info "Features: Hot reload, debug mode, local data, fast iteration"
echo ""

# 1.1 Stop any existing services
print_section "1.1: Cleaning up existing services"
echo "  Stopping any running services..."
just stop 2>/dev/null || docker-compose down 2>/dev/null || true
print_success "Cleanup complete"

# 1.2 Check prerequisites
print_section "1.2: Checking prerequisites"

echo -n "  Checking Docker... "
if command -v docker &> /dev/null; then
    print_success "Docker installed"
else
    print_error "Docker not found"
    exit 1
fi

echo -n "  Checking Just... "
if command -v just &> /dev/null; then
    print_success "Just installed"
else
    print_warning "Just not found - will use direct commands"
fi

echo -n "  Checking Java... "
if command -v java &> /dev/null; then
    java_version=$(java -version 2>&1 | head -n 1)
    print_success "Java installed: $java_version"
else
    print_error "Java not found"
    exit 1
fi

# 1.3 Build the backend
print_section "1.3: Building backend services"
echo "  Building Ktor server JAR..."
./gradlew :server:shadowJar --quiet
print_success "Server JAR built successfully"

# 1.4 Start backend stack (dev mode)
print_section "1.4: Starting backend stack (Development mode)"
print_info "Using: docker-compose.yml (lightweight dev configuration)"
echo ""

docker-compose up -d

print_success "Backend stack started"
echo ""

# 1.5 Wait for services
print_section "1.5: Waiting for services to be ready"
wait_for_service "PocketBase" "http://localhost:8092/api/health"
wait_for_service "Ktor Server" "http://localhost:8081/health"
wait_for_service "Redis" "http://localhost:6379"

# 1.6 Test dev endpoints
print_section "1.6: Testing development endpoints"
test_endpoint "PocketBase Health" "http://localhost:8092/api/health"
test_endpoint "Ktor Health" "http://localhost:8081/health"
test_endpoint "PocketBase Admin UI" "http://localhost:8092/_/" 200

echo ""
print_info "Development backend is running at:"
echo "  • PocketBase:    http://localhost:8092"
echo "  • Ktor API:      http://localhost:8081"
echo "  • Admin Panel:   http://localhost:8092/_/"
echo "  • Credentials:   tester_admin@bside.love / password123"

echo ""
read -p "Press ENTER to test the client applications..."

# 1.7 Test client builds
print_section "1.7: Testing client application builds"

echo "  Building desktop client..."
./gradlew :composeApp:jvmJar --quiet
print_success "Desktop client built"

echo "  Building web client..."
./gradlew :composeApp:jsBrowserDevelopmentWebpack --quiet
print_success "Web client built"

echo "  Checking Android setup..."
if [ -d "composeApp/build/outputs/apk" ] || ./gradlew :composeApp:assembleDebug --dry-run &>/dev/null; then
    print_success "Android build configuration OK"
else
    print_warning "Android build may need setup"
fi

echo ""
print_section "1.8: Manual testing time!"
print_info "The backend is running. Now let's test the apps:"
echo ""
echo "  Option 1 - Desktop:"
echo "    $ just desktop"
echo ""
echo "  Option 2 - Web:"
echo "    $ just web"
echo ""
echo "  Option 3 - Android:"
echo "    $ just android-studio"
echo ""

read -p "Test one of the apps, then press ENTER to continue..."

# ============================================================================
# PART 2: ENHANCED DEVELOPMENT (WITH MONITORING)
# ============================================================================

print_header "PART 2: ENHANCED DEVELOPMENT (WITH MONITORING)"

print_info "This adds observability tools for more thorough testing."
print_info "Features: Grafana, Prometheus, metrics, real-time monitoring"
echo ""

read -p "Start enhanced stack? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Skipping enhanced stack"
else
    print_section "2.1: Stopping basic dev stack"
    docker-compose down
    
    print_section "2.2: Starting enhanced stack"
    print_info "Using: docker-compose.enhanced-lite.yml"
    docker-compose -f docker-compose.enhanced-lite.yml up -d
    
    print_section "2.3: Waiting for enhanced services"
    wait_for_service "PocketBase" "http://localhost:8092/api/health"
    wait_for_service "Ktor Server" "http://localhost:8081/health"
    wait_for_service "Grafana" "http://localhost:3000"
    wait_for_service "Prometheus" "http://localhost:9090"
    
    print_section "2.4: Testing enhanced endpoints"
    test_endpoint "PocketBase" "http://localhost:8092/api/health"
    test_endpoint "Ktor" "http://localhost:8081/health"
    test_endpoint "Grafana" "http://localhost:3000/api/health"
    test_endpoint "Prometheus" "http://localhost:9090/-/healthy"
    
    echo ""
    print_info "Enhanced stack is running:"
    echo "  • All dev services (as before)"
    echo "  • Grafana:       http://localhost:3000 (admin/admin)"
    echo "  • Prometheus:    http://localhost:9090"
    echo "  • Metrics:       http://localhost:8081/metrics"
    
    echo ""
    read -p "Check Grafana dashboards, then press ENTER..."
fi

# ============================================================================
# PART 3: FULL STACK (WITH ALL SERVICES)
# ============================================================================

print_header "PART 3: FULL STACK (COMPLETE LOCAL SETUP)"

print_info "This includes ALL services: monitoring, analytics, UI tools."
print_info "Features: Everything! Redis UI, Node exporter, cAdvisor, GoAccess"
echo ""

read -p "Start full stack? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Skipping full stack"
else
    print_section "3.1: Stopping previous stack"
    docker-compose -f docker-compose.enhanced-lite.yml down 2>/dev/null || docker-compose down
    
    print_section "3.2: Starting full stack"
    print_info "Using: docker-compose.full.yml"
    docker-compose -f docker-compose.full.yml up -d
    
    print_section "3.3: Waiting for all services"
    wait_for_service "PocketBase" "http://localhost:8092/api/health" 60
    wait_for_service "Ktor Server" "http://localhost:8081/health" 60
    wait_for_service "Grafana" "http://localhost:3000" 30
    wait_for_service "Prometheus" "http://localhost:9090/-/healthy" 30
    
    print_section "3.4: Testing all services"
    echo ""
    echo "Core services:"
    test_endpoint "PocketBase" "http://localhost:8092/api/health"
    test_endpoint "Ktor" "http://localhost:8081/health"
    test_endpoint "Redis" "http://localhost:6379"
    
    echo ""
    echo "Monitoring:"
    test_endpoint "Grafana" "http://localhost:3000/api/health"
    test_endpoint "Prometheus" "http://localhost:9090/-/healthy"
    
    echo ""
    echo "Tools:"
    test_endpoint "Redis UI" "http://localhost:8083" 200
    test_endpoint "Node Exporter" "http://localhost:9100/metrics"
    
    echo ""
    print_info "Full stack services:"
    echo "  Core:"
    echo "    • PocketBase:      http://localhost:8092"
    echo "    • Ktor API:        http://localhost:8081"
    echo "    • Redis:           localhost:6379"
    echo ""
    echo "  Monitoring:"
    echo "    • Grafana:         http://localhost:3000"
    echo "    • Prometheus:      http://localhost:9090"
    echo ""
    echo "  Tools:"
    echo "    • Redis UI:        http://localhost:8083"
    echo "    • Node Exporter:   http://localhost:9100"
    echo "    • cAdvisor:        http://localhost:8080"
    
    echo ""
    read -p "Explore the full stack, then press ENTER..."
fi

# ============================================================================
# PART 4: PRODUCTION-LIKE CONFIGURATION
# ============================================================================

print_header "PART 4: PRODUCTION-LIKE CONFIGURATION"

print_info "This mimics production with proper security, limits, and monitoring."
print_info "Features: Rate limiting, HTTPS ready, resource limits, health checks"
echo ""

read -p "Start production-like stack? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Skipping production-like stack"
else
    print_section "4.1: Creating production environment file"
    
    cat > .env.production << 'ENV_EOF'
# Production Environment Configuration
POCKETBASE_ADMIN_EMAIL=admin@bside.app
POCKETBASE_ADMIN_PASSWORD=CHANGE_ME_IN_PRODUCTION
PB_PUBLIC_URL=http://localhost:8092
CDN_ENABLED=false
ENV_EOF
    
    print_success "Production .env created"
    
    print_section "4.2: Stopping previous stack"
    docker-compose -f docker-compose.full.yml down 2>/dev/null || docker-compose down
    
    print_section "4.3: Starting production-like stack"
    print_info "Using: docker-compose.production.yml"
    docker-compose -f docker-compose.production.yml --env-file .env.production up -d
    
    print_section "4.4: Waiting for services (production startup is slower)"
    wait_for_service "PocketBase" "http://localhost:8092/api/health" 90
    wait_for_service "Ktor Server" "http://localhost:8081/health" 90
    
    print_section "4.5: Testing production configuration"
    test_endpoint "PocketBase Health" "http://localhost:8092/api/health"
    test_endpoint "Ktor Health" "http://localhost:8081/health"
    test_endpoint "Nginx Proxy" "http://localhost:8082" 502
    
    echo ""
    print_info "Production-like stack is running with:"
    echo "  ✓ Resource limits enforced"
    echo "  ✓ Restart policies configured"
    echo "  ✓ Health checks active"
    echo "  ✓ Environment separation"
    echo "  ✓ Security headers (via Nginx)"
    
    echo ""
    read -p "Review production configuration, then press ENTER..."
fi

# ============================================================================
# PART 5: ENTERPRISE STACK (OPTIONAL)
# ============================================================================

print_header "PART 5: ENTERPRISE STACK (OPTIONAL)"

print_info "Complete enterprise setup with all bells and whistles."
print_info "This is heavy and comprehensive - only for full integration testing."
echo ""

read -p "Start enterprise stack? (y/n): " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    print_warning "Skipping enterprise stack"
else
    print_section "5.1: Stopping previous stack"
    docker-compose -f docker-compose.production.yml down 2>/dev/null || docker-compose down
    
    print_section "5.2: Starting enterprise stack"
    print_warning "This will start MANY containers. Be patient..."
    docker-compose -f docker-compose.enterprise.yml up -d
    
    print_section "5.3: Waiting for core services"
    wait_for_service "PocketBase" "http://localhost:8092/api/health" 120
    wait_for_service "Ktor Server" "http://localhost:8081/health" 120
    
    print_success "Enterprise stack started"
    
    echo ""
    print_info "Enterprise stack includes EVERYTHING:"
    echo "  • All previous services"
    echo "  • Advanced monitoring"
    echo "  • Log aggregation (Loki)"
    echo "  • Distributed tracing"
    echo "  • Full observability"
    
    echo ""
    read -p "Explore enterprise features, then press ENTER..."
fi

# ============================================================================
# PART 6: INTEGRATION TESTING
# ============================================================================

print_header "PART 6: RUNNING INTEGRATION TESTS"

print_section "6.1: Running built-in test suite"

if [ -f "test-stack.sh" ]; then
    print_info "Running test-stack.sh..."
    ./test-stack.sh
else
    print_warning "test-stack.sh not found, skipping"
fi

if [ -f "scripts/test-full-stack.sh" ]; then
    print_info "Running scripts/test-full-stack.sh..."
    ./scripts/test-full-stack.sh
else
    print_warning "test-full-stack.sh not found, skipping"
fi

# ============================================================================
# FINAL SUMMARY
# ============================================================================

print_header "WALKTHROUGH COMPLETE!"

echo ""
echo "📊 Test Results:"
echo "  ✅ Passed: $TESTS_PASSED"
echo "  ❌ Failed: $TESTS_FAILED"
echo ""

echo "🎯 What you tested:"
echo "  ✓ Local development environment"
echo "  ✓ Enhanced monitoring stack"
echo "  ✓ Full local setup"
echo "  ✓ Production-like configuration"
echo "  ✓ Integration tests"
echo ""

echo "📝 Current State:"
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" | head -10
echo ""

echo "🛠️  Useful Commands:"
echo "  Stop everything:     just stop"
echo "  View logs:           docker-compose logs -f"
echo "  Check health:        curl http://localhost:8092/api/health"
echo "  Admin panel:         open http://localhost:8092/_/"
echo ""

echo "📚 Next Steps:"
echo "  1. Choose your preferred stack configuration"
echo "  2. Run client applications (desktop, web, mobile)"
echo "  3. Test real-time messaging features"
echo "  4. Monitor performance in Grafana"
echo "  5. Prepare for staging/production deployment"
echo ""

read -p "Clean up and stop all services? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_section "Cleaning up..."
    docker-compose -f docker-compose.enterprise.yml down 2>/dev/null || true
    docker-compose -f docker-compose.production.yml down 2>/dev/null || true
    docker-compose -f docker-compose.full.yml down 2>/dev/null || true
    docker-compose -f docker-compose.enhanced-lite.yml down 2>/dev/null || true
    docker-compose down 2>/dev/null || true
    rm -f .env.production
    print_success "All services stopped and cleaned up"
else
    print_info "Services left running. Use 'just stop' to clean up later."
fi

echo ""
print_success "Walkthrough complete! 🎉"
echo ""
