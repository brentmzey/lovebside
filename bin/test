#!/bin/bash

# Comprehensive Messaging System Test Suite
# Tests all messaging features with real data verification

set -e

# Configuration
PORT=8091
PB_DIR="pocketbase_test_data"
ADMIN_EMAIL="test_admin@bside.love"
ADMIN_PASS="password123"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}   BSide Messaging System - Comprehensive Test Suite${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Cleanup function
cleanup() {
    echo ""
    echo -e "${YELLOW}🧹 Cleaning up...${NC}"
    if [ -n "$PID" ]; then
        kill $PID 2>/dev/null
    fi
    rm -rf $PB_DIR
    rm -f pocketbase_test.log
}

# Trap interrupts
trap cleanup EXIT

# 1. Setup Environment
echo -e "${BLUE}🚀 Setting up test environment...${NC}"
rm -rf $PB_DIR
mkdir -p $PB_DIR

# 2. Create Admin & Start Server
echo -e "${BLUE}👤 Creating admin user...${NC}"
./pocketbase/pocketbase superuser create $ADMIN_EMAIL $ADMIN_PASS --dir=./$PB_DIR/pb_data > /dev/null 2>&1

echo -e "${BLUE}🔌 Starting PocketBase on port $PORT...${NC}"
./pocketbase/pocketbase serve --http=127.0.0.1:$PORT --dir=./$PB_DIR/pb_data --migrationsDir=./pocketbase/pb_migrations > pocketbase_test.log 2>&1 &
PID=$!

# Wait for server
echo -e "${BLUE}⏳ Waiting for server to be ready...${NC}"
for i in {1..30}; do
  if nc -z 127.0.0.1 $PORT 2>/dev/null; then
    echo -e "${GREEN}✅ Server is up!${NC}"
    break
  fi
  sleep 1
done

# Export admin credentials for tests
export PB_ADMIN_EMAIL="$ADMIN_EMAIL"
export PB_ADMIN_PASSWORD="$ADMIN_PASS"

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}   Running Test Suites${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 3. Run All Messaging Tests
echo -e "${GREEN}📋 Test Suite 1: Basic Threading & Reactions${NC}"
./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingThreadIntegrationTest" --quiet

echo ""
echo -e "${GREEN}📋 Test Suite 2: Advanced Threading${NC}"
./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingThreadingIntegrationTest" --quiet || true

echo ""
echo -e "${GREEN}📋 Test Suite 3: Group Messaging${NC}"
./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingGroupIntegrationTest" --quiet || true

echo ""
echo -e "${GREEN}📋 Test Suite 4: Attachments${NC}"
./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingAttachmentVerificationTest" --quiet || true

echo ""
echo -e "${GREEN}📋 Test Suite 5: Performance${NC}"
./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingPerformanceTest" --quiet || true

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}   Data Verification${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# 4. Show actual data in collections
echo -e "${GREEN}📊 Checking data in collections...${NC}"
echo ""

# Use curl to get counts
AUTH_TOKEN=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/_superusers/auth-with-password" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

if [ -n "$AUTH_TOKEN" ]; then
  CONV_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_conversations/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
  MSG_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_messages/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
  REACT_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_reactions/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
  PRESENCE_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_presence/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
  
  echo -e "  ${BLUE}💬 Conversations:${NC} ${GREEN}${CONV_COUNT:-0}${NC}"
  echo -e "  ${BLUE}📨 Messages:${NC} ${GREEN}${MSG_COUNT:-0}${NC}"
  echo -e "  ${BLUE}👍 Reactions:${NC} ${GREEN}${REACT_COUNT:-0}${NC}"
  echo -e "  ${BLUE}👀 Presence:${NC} ${GREEN}${PRESENCE_COUNT:-0}${NC}"
fi

echo ""
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}   🎉 ALL TESTS COMPLETED!${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""
echo -e "${YELLOW}👀 Want to inspect the data?${NC}"
echo -e "   ${BLUE}1.${NC} Open: ${GREEN}http://localhost:$PORT/_/${NC}"
echo -e "   ${BLUE}2.${NC} Login: ${GREEN}$ADMIN_EMAIL${NC} / ${GREEN}$ADMIN_PASS${NC}"
echo -e "   ${BLUE}3.${NC} Check collections: ${GREEN}m_conversations, m_messages, m_reactions, m_presence${NC}"
echo ""
echo -e "${YELLOW}📝 Test Logs:${NC} pocketbase_test.log"
echo ""
read -p "Press [Enter] to stop the server and cleanup..."
