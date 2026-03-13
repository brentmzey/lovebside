#!/bin/bash

# Real-Time Messaging Demo
# Shows live data flow with visual feedback

set -e

PORT=8091
PB_DIR="pocketbase_demo_data"
ADMIN_EMAIL="demo_admin@bside.love"
ADMIN_PASS="password123"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
MAGENTA='\033[0;35m'
NC='\033[0m'

cleanup() {
    if [ -n "$PID" ]; then
        kill $PID 2>/dev/null
    fi
    rm -rf $PB_DIR
    rm -f pocketbase_demo.log
}

trap cleanup EXIT

clear
echo -e "${CYAN}╔════════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}  ${BLUE}BSide Messaging - Real-Time Demo${NC}                        ${CYAN}║${NC}"
echo -e "${CYAN}╚════════════════════════════════════════════════════════════╝${NC}"
echo ""

# Setup
echo -e "${YELLOW}⚙️  Setting up demo environment...${NC}"
rm -rf $PB_DIR
mkdir -p $PB_DIR
./pocketbase/pocketbase superuser create $ADMIN_EMAIL $ADMIN_PASS --dir=./$PB_DIR/pb_data > /dev/null 2>&1
./pocketbase/pocketbase serve --http=127.0.0.1:$PORT --dir=./$PB_DIR/pb_data --migrationsDir=./pocketbase/pb_migrations > pocketbase_demo.log 2>&1 &
PID=$!

# Wait for server
for i in {1..30}; do
  if nc -z 127.0.0.1 $PORT 2>/dev/null; then
    break
  fi
  sleep 1
done

echo -e "${GREEN}✓${NC} PocketBase started on port $PORT"
echo ""

# Get auth token
AUTH_TOKEN=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/_superusers/auth-with-password" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Create demo users
echo -e "${CYAN}👥 Creating demo users...${NC}"
USER1_ID=""
USER2_ID=""

# User 1
USER1_RESP=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/users/records" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@demo.com","password":"password123","passwordConfirm":"password123","name":"Alice"}')
USER1_ID=$(echo $USER1_RESP | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

# User 2
USER2_RESP=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/users/records" \
  -H "Content-Type: application/json" \
  -d '{"email":"bob@demo.com","password":"password123","passwordConfirm":"password123","name":"Bob"}')
USER2_ID=$(echo $USER2_RESP | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

echo -e "  ${GREEN}✓${NC} Alice (${USER1_ID:0:8}...)"
echo -e "  ${GREEN}✓${NC} Bob (${USER2_ID:0:8}...)"
echo ""

# Auth as User 1
USER1_TOKEN=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/users/auth-with-password" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"alice@demo.com\",\"password\":\"password123\"}" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

echo -e "${CYAN}💬 Creating conversation...${NC}"
CONV_RESP=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_conversations/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"participants\":[\"$USER1_ID\",\"$USER2_ID\"],\"type\":\"direct\"}")
CONV_ID=$(echo $CONV_RESP | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)

echo -e "  ${GREEN}✓${NC} Conversation created (${CONV_ID:0:8}...)"
echo ""

# Send messages
echo -e "${CYAN}📨 Sending messages...${NC}"
sleep 1

MSG1=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_messages/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"conversation_id\":\"$CONV_ID\",\"sender_id\":\"$USER1_ID\",\"content\":\"Hey Bob! 👋\",\"type\":\"text\"}")
MSG1_ID=$(echo $MSG1 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "  ${BLUE}Alice:${NC} Hey Bob! 👋"
sleep 1

MSG2=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_messages/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"conversation_id\":\"$CONV_ID\",\"sender_id\":\"$USER1_ID\",\"content\":\"How's the BSide app coming along?\",\"type\":\"text\"}")
MSG2_ID=$(echo $MSG2 | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo -e "  ${BLUE}Alice:${NC} How's the BSide app coming along?"
sleep 1

# Add reaction
echo ""
echo -e "${CYAN}👍 Adding reaction...${NC}"
curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_reactions/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"message_id\":\"$MSG1_ID\",\"user_id\":\"$USER1_ID\",\"reaction\":\"👍\"}" > /dev/null
echo -e "  ${GREEN}✓${NC} Reaction added to first message"
sleep 1

# Set presence
echo ""
echo -e "${CYAN}👀 Setting presence...${NC}"
curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_presence/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"user_id\":\"$USER1_ID\",\"status\":\"online\",\"activity_message\":\"Building something awesome\"}" > /dev/null
echo -e "  ${GREEN}✓${NC} Alice is now ${GREEN}online${NC}: Building something awesome"
sleep 1

# Create threaded reply
echo ""
echo -e "${CYAN}🧵 Creating threaded reply...${NC}"
MSG3=$(curl -s -X POST "http://127.0.0.1:$PORT/api/collections/m_messages/records" \
  -H "Authorization: Bearer $USER1_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"conversation_id\":\"$CONV_ID\",\"sender_id\":\"$USER1_ID\",\"content\":\"Let me elaborate on that...\",\"type\":\"text\",\"reply_to_message_id\":\"$MSG2_ID\",\"thread_root_id\":\"$MSG2_ID\"}")
echo -e "  ${BLUE}Alice:${NC} ↳ Let me elaborate on that... ${MAGENTA}[reply to previous]${NC}"
sleep 1

echo ""
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${GREEN}✅ Demo data created successfully!${NC}"
echo -e "${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Show data counts
CONV_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_conversations/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
MSG_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_messages/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
REACT_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_reactions/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
PRESENCE_COUNT=$(curl -s -H "Authorization: Bearer $AUTH_TOKEN" "http://127.0.0.1:$PORT/api/collections/m_presence/records" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)

echo -e "${YELLOW}📊 Database Contents:${NC}"
echo -e "  ${BLUE}💬 Conversations:${NC} ${GREEN}${CONV_COUNT}${NC}"
echo -e "  ${BLUE}📨 Messages:${NC} ${GREEN}${MSG_COUNT}${NC} (including 1 threaded reply)"
echo -e "  ${BLUE}👍 Reactions:${NC} ${GREEN}${REACT_COUNT}${NC}"
echo -e "  ${BLUE}👀 Presence:${NC} ${GREEN}${PRESENCE_COUNT}${NC}"
echo ""
echo -e "${YELLOW}🔍 View in PocketBase Admin:${NC}"
echo -e "  ${BLUE}URL:${NC} http://localhost:$PORT/_/"
echo -e "  ${BLUE}Login:${NC} $ADMIN_EMAIL / $ADMIN_PASS"
echo ""
echo -e "${YELLOW}📡 API Endpoints Ready:${NC}"
echo -e "  ${BLUE}Base URL:${NC} http://localhost:$PORT/"
echo -e "  ${BLUE}Collections:${NC} m_conversations, m_messages, m_reactions, m_presence"
echo ""
echo -e "${GREEN}Press [Enter] to cleanup and exit...${NC}"
read
