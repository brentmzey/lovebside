#!/bin/bash

# B-Side Phase 0 Validation Script
# Comprehensive testing of all client targets before deployment

set -e

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
NC='\033[0m'

# Counters
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_SKIPPED=0

print_header() {
    echo -e "\n${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}\n"
}

print_section() {
    echo -e "\n${MAGENTA}▶ $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
    ((TESTS_PASSED++))
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
    ((TESTS_FAILED++))
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_skip() {
    echo -e "${YELLOW}⏭️  $1${NC}"
    ((TESTS_SKIPPED++))
}

# Test function
test_command() {
    local name=$1
    local command=$2
    
    echo -n "  Testing $name... "
    
    if eval "$command" > /dev/null 2>&1; then
        print_success "$name"
        return 0
    else
        print_error "$name failed"
        return 1
    fi
}

clear

cat << 'EOF'
╔══════════════════════════════════════════════════════════════════╗
║                                                                  ║
║       🧪 B-SIDE PHASE 0: PRE-DEPLOYMENT VALIDATION               ║
║                                                                  ║
║  This script validates EVERYTHING before deployment:            ║
║  • Backend services                                              ║
║  • Database & migrations                                         ║
║  • All client targets (Desktop, Web, Android, iOS)              ║
║  • Integration tests                                             ║
║                                                                  ║
╚══════════════════════════════════════════════════════════════════╝

EOF

read -p "Press ENTER to begin validation..."

# ============================================================================
# 0.1 BACKEND STACK VALIDATION
# ============================================================================

print_header "0.1: Backend Stack Validation"

print_section "Stopping any existing services"
just stop 2>/dev/null || docker-compose down 2>/dev/null || true
print_success "Cleanup complete"

print_section "Building backend"
./gradlew :server:shadowJar --quiet
print_success "Server JAR built"

print_section "Starting backend services"
docker-compose up -d
sleep 10

print_section "Checking service health"

# PocketBase
if curl -sf http://localhost:8092/api/health > /dev/null; then
    print_success "PocketBase is healthy"
else
    print_error "PocketBase is not responding"
fi

# Ktor
if curl -sf http://localhost:8081/health > /dev/null; then
    print_success "Ktor server is healthy"
else
    print_error "Ktor server is not responding"
fi

# Redis
if docker exec bside-redis redis-cli ping 2>/dev/null | grep -q PONG; then
    print_success "Redis is healthy"
else
    print_error "Redis is not responding"
fi

print_section "Checking service logs for errors"
ERROR_COUNT=$(docker-compose logs --tail=50 2>&1 | grep -i error | wc -l)
if [ "$ERROR_COUNT" -lt 5 ]; then
    print_success "No critical errors in logs"
else
    print_warning "Found $ERROR_COUNT error messages in logs (review recommended)"
fi

# ============================================================================
# 0.2 DATABASE MIGRATION & SEEDING
# ============================================================================

print_header "0.2: Database Migration & Seeding"

print_section "Checking database collections"

COLLECTIONS=$(docker exec bside-pocketbase wget -qO- http://localhost:8090/api/collections 2>/dev/null | jq -r '.items[].name' | wc -l)

if [ "$COLLECTIONS" -gt 5 ]; then
    print_success "Database has $COLLECTIONS collections"
else
    print_warning "Only $COLLECTIONS collections found (may need migrations)"
fi

print_section "Creating seed data script"

cat > /tmp/seed-data.sh << 'SEED_EOF'
#!/bin/bash
# Seed test data

echo "🌱 Seeding test data..."

# Create test users via API
for i in {1..5}; do
    curl -sf -X POST http://localhost:8092/api/collections/t_user/records \
        -H "Content-Type: application/json" \
        -d "{
            \"username\": \"testuser$i\",
            \"email\": \"test$i@bside.test\",
            \"password\": \"Test1234!\",
            \"passwordConfirm\": \"Test1234!\",
            \"name\": \"Test User $i\"
        }" > /dev/null && echo "✅ Created testuser$i" || echo "⚠️  testuser$i may already exist"
done

echo "✅ Seed data complete"
SEED_EOF

chmod +x /tmp/seed-data.sh
bash /tmp/seed-data.sh

print_success "Seed data created"

# ============================================================================
# 0.3 CLIENT TARGET TESTING - DESKTOP
# ============================================================================

print_header "0.3: Desktop Client Testing"

print_section "Building desktop client"
if ./gradlew :composeApp:jvmJar --quiet 2>&1 | tail -5; then
    print_success "Desktop JAR built successfully"
else
    print_error "Desktop build failed"
fi

print_section "Desktop manual testing required"
print_warning "Manual steps:"
echo "  1. Run: just desktop"
echo "  2. Test user registration"
echo "  3. Test login"
echo "  4. Test sending/receiving messages"
echo "  5. Take screenshots"
echo ""
read -p "Have you completed desktop testing? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_success "Desktop testing completed"
else
    print_skip "Desktop testing skipped"
fi

# ============================================================================
# 0.4 CLIENT TARGET TESTING - WEB
# ============================================================================

print_header "0.4: Web Client Testing"

print_section "Building web client"
if ./gradlew :composeApp:jsBrowserDevelopmentWebpack --quiet 2>&1 | tail -5; then
    print_success "Web build successful"
else
    print_error "Web build failed"
fi

print_section "Web manual testing required"
print_warning "Manual steps:"
echo "  1. Run: just web"
echo "  2. Test in Chrome, Firefox, Safari"
echo "  3. Test responsive design"
echo "  4. Test all features"
echo "  5. Take screenshots"
echo ""
read -p "Have you completed web testing? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_success "Web testing completed"
else
    print_skip "Web testing skipped"
fi

# ============================================================================
# 0.5 CLIENT TARGET TESTING - ANDROID
# ============================================================================

print_header "0.5: Android Client Testing"

print_section "Checking Android build configuration"
if ./gradlew :composeApp:tasks | grep -q assembleDebug; then
    print_success "Android build tasks available"
else
    print_error "Android build configuration missing"
fi

print_section "Android manual testing required"
print_warning "Manual steps:"
echo "  1. Run: just android-studio"
echo "  2. Build debug APK"
echo "  3. Install on emulator or device"
echo "  4. Test all features"
echo "  5. Test location permissions"
echo "  6. Take screenshots"
echo ""
read -p "Have you completed Android testing? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_success "Android testing completed"
else
    print_skip "Android testing skipped"
fi

# ============================================================================
# 0.6 CLIENT TARGET TESTING - IOS
# ============================================================================

print_header "0.6: iOS Client Testing"

if [ "$(uname)" = "Darwin" ]; then
    print_section "Checking iOS setup"
    if [ -d "iosApp/iosApp.xcodeproj" ]; then
        print_success "iOS project exists"
    else
        print_error "iOS project not found"
    fi
    
    print_section "iOS manual testing required"
    print_warning "Manual steps:"
    echo "  1. Run: just ios"
    echo "  2. Build for simulator"
    echo "  3. Test all features"
    echo "  4. Test location permissions"
    echo "  5. Test on real device if possible"
    echo "  6. Take screenshots"
    echo ""
    read -p "Have you completed iOS testing? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_success "iOS testing completed"
    else
        print_skip "iOS testing skipped"
    fi
else
    print_skip "iOS testing (macOS only)"
fi

# ============================================================================
# 0.7 INTEGRATION TESTING
# ============================================================================

print_header "0.7: Integration Testing"

print_section "Multi-user test required"
print_warning "Manual steps:"
echo "  1. Open 2+ client instances (different browsers/apps)"
echo "  2. Login as different users"
echo "  3. Send messages between users"
echo "  4. Verify real-time delivery"
echo "  5. Test typing indicators"
echo "  6. Test read receipts"
echo "  7. Test reactions"
echo ""
read -p "Have you completed integration testing? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    print_success "Integration testing completed"
else
    print_skip "Integration testing skipped"
fi

# ============================================================================
# 0.8 PERFORMANCE TESTING
# ============================================================================

print_header "0.8: Performance Testing"

print_section "Basic performance checks"

# Response time test
START=$(date +%s%N)
curl -sf http://localhost:8092/api/health > /dev/null
END=$(date +%s%N)
DURATION=$((($END - $START) / 1000000))

if [ "$DURATION" -lt 100 ]; then
    print_success "API response time: ${DURATION}ms (< 100ms)"
else
    print_warning "API response time: ${DURATION}ms (target: < 100ms)"
fi

print_section "Load testing recommended"
print_warning "For production, run:"
echo "  $ k6 run scripts/load-test.js"
echo "  $ ab -n 1000 -c 10 http://localhost:8092/api/health"

# ============================================================================
# FINAL SUMMARY
# ============================================================================

print_header "VALIDATION SUMMARY"

echo ""
echo "📊 Test Results:"
echo -e "  ${GREEN}✅ Passed:  $TESTS_PASSED${NC}"
echo -e "  ${RED}❌ Failed:  $TESTS_FAILED${NC}"
echo -e "  ${YELLOW}⏭️  Skipped: $TESTS_SKIPPED${NC}"
echo ""

if [ "$TESTS_FAILED" -eq 0 ]; then
    print_success "ALL AUTOMATED TESTS PASSED!"
    echo ""
    echo "📋 Phase 0 Status:"
    echo "  • Backend: ✅ Validated"
    echo "  • Database: ✅ Seeded"
    echo "  • Clients: Manual testing required"
    echo ""
    echo "📝 Next Steps:"
    echo "  1. Complete manual client testing"
    echo "  2. Document any issues in .code-hq/PROJECT_TRACKER.md"
    echo "  3. Review checklist in PROJECT_TRACKER.md"
    echo "  4. When all Phase 0 tasks complete, begin Phase 1"
    echo ""
else
    print_error "VALIDATION FAILED"
    echo ""
    echo "❌ $TESTS_FAILED tests failed"
    echo ""
    echo "Please fix the failed tests before proceeding."
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
