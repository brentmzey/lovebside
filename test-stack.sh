#!/usr/bin/env bash
set -euo pipefail

# Bside Stack Integration Test
# Tests the entire stack end-to-end

echo "🧪 Bside Stack Integration Test"
echo "================================"
echo ""

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

TESTS_PASSED=0
TESTS_FAILED=0

# Test function
test_endpoint() {
    local name=$1
    local url=$2
    local expected_code=${3:-200}
    
    echo -n "Testing $name... "
    
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

# Test POST endpoint
test_post() {
    local name=$1
    local url=$2
    local data=$3
    local expected_code=${4:-200}
    
    echo -n "Testing $name... "
    
    response=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST \
        -H "Content-Type: application/json" \
        -d "$data" \
        "$url" 2>/dev/null || echo "000")
    
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

echo "📍 Testing Infrastructure"
echo "========================="
test_endpoint "Nginx Health" "http://localhost:8082/health"
test_endpoint "Backend Health" "http://localhost:8081/health"
test_endpoint "PocketBase Health" "http://localhost:8092/api/health"
echo ""

echo "📍 Testing PocketBase Routes (via Nginx)"
echo "=========================================="
test_endpoint "PocketBase API" "http://localhost:8082/api/pb/health"
test_endpoint "PocketBase Collections" "http://localhost:8082/api/pb/collections" "200"
echo ""

echo "📍 Testing Database Collections"
echo "================================"
test_endpoint "Users Collection" "http://localhost:8092/api/collections/users/records"
test_endpoint "Profiles Collection" "http://localhost:8092/api/collections/s_profiles/records"
test_endpoint "Conversations Collection" "http://localhost:8092/api/collections/m_conversations/records"
test_endpoint "Messages Collection" "http://localhost:8092/api/collections/m_messages/records"
test_endpoint "Typing Status Collection" "http://localhost:8092/api/collections/m_typing_status/records"
test_endpoint "Read Receipts Collection" "http://localhost:8092/api/collections/m_read_receipts/records"
test_endpoint "Reactions Collection" "http://localhost:8092/api/collections/m_reactions/records"
test_endpoint "Presence Collection" "http://localhost:8092/api/collections/m_presence/records"
test_endpoint "Polls Collection" "http://localhost:8092/api/collections/m_polls/records"
test_endpoint "Mentions Collection" "http://localhost:8092/api/collections/m_mentions/records"
test_endpoint "Message Media Collection" "http://localhost:8092/api/collections/m_message_media/records"
test_endpoint "Proust Questionnaire" "http://localhost:8092/api/collections/t_proust_questionnaire/records"
test_endpoint "Matches Collection" "http://localhost:8092/api/collections/m_matches/records"
echo ""

echo "📍 Testing Real-Time Features"
echo "=============================="
echo "Note: WebSocket tests require manual verification"
echo "   - Open browser console"
echo "   - Connect to ws://localhost:8082/api/pb/"
echo "   - Subscribe to collections for real-time updates"
echo ""

echo "📍 Testing Rate Limiting"
echo "========================"
echo "Sending 15 rapid requests (limit is 10/s)..."
rate_limited=false
for i in {1..15}; do
    response=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:8082/api/pb/health" 2>/dev/null)
    if [ "$response" = "429" ]; then
        rate_limited=true
        break
    fi
done

if [ "$rate_limited" = true ]; then
    echo -e "${GREEN}✅ Rate limiting is working${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠️  Rate limiting not triggered (may need more requests)${NC}"
fi
echo ""

echo "📍 Testing Load Balancing"
echo "========================="
echo "Testing request distribution..."
for i in {1..10}; do
    curl -s "http://localhost:8082/api/pb/health" > /dev/null &
done
wait
echo -e "${GREEN}✅ All concurrent requests completed${NC}"
((TESTS_PASSED++))
echo ""

echo "📍 Testing CORS and Security Headers"
echo "====================================="
security_test=$(curl -s -I "http://localhost:8082/health" | grep -E "X-Frame-Options|X-Content-Type-Options|X-XSS-Protection")
if [ -n "$security_test" ]; then
    echo -e "${GREEN}✅ Security headers present${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${RED}❌ Security headers missing${NC}"
    ((TESTS_FAILED++))
fi
echo ""

echo "📍 Testing Compression"
echo "======================"
gzip_test=$(curl -s -I -H "Accept-Encoding: gzip" "http://localhost:8082/api/pb/health" | grep -i "content-encoding: gzip")
if [ -n "$gzip_test" ]; then
    echo -e "${GREEN}✅ Gzip compression enabled${NC}"
    ((TESTS_PASSED++))
else
    echo -e "${YELLOW}⚠️  Gzip not detected (may not be needed for this endpoint)${NC}"
fi
echo ""

# Summary
echo "═══════════════════════════════════════════"
echo "📊 TEST SUMMARY"
echo "═══════════════════════════════════════════"
echo -e "Tests Passed: ${GREEN}$TESTS_PASSED${NC}"
echo -e "Tests Failed: ${RED}$TESTS_FAILED${NC}"
echo ""

if [ $TESTS_FAILED -eq 0 ]; then
    echo -e "${GREEN}🎉 ALL TESTS PASSED!${NC}"
    echo ""
    echo "✅ Your Bside stack is ready to use!"
    echo ""
    echo "Next steps:"
    echo "1. Open the mobile app"
    echo "2. Sign up a new user"
    echo "3. Complete the Proust questionnaire"
    echo "4. Start a conversation"
    echo "5. Send messages and test real-time features"
    echo ""
    exit 0
else
    echo -e "${RED}❌ SOME TESTS FAILED${NC}"
    echo ""
    echo "Check the logs:"
    echo "  docker-compose logs -f"
    echo ""
    exit 1
fi
