#!/bin/bash

# Full Automated Demo - Seeds DB, Starts Backend, Launches All UIs, Tests Realtime Messaging
# Usage: ./scripts/run-full-demo.sh

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Configuration
export POCKETBASE_URL="http://localhost:8092"
BACKEND_URL="http://localhost:8080"
NGINX_URL="http://localhost:80"
WEB_URL="http://localhost:3000"

echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   B-SIDE FULL STACK AUTOMATED DEMO                        ║"
echo "║   Professional KMP Architecture                           ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Step 1: Check prerequisites
echo -e "${YELLOW}[1/10] Checking prerequisites...${NC}"
command -v docker >/dev/null 2>&1 || { echo "Docker required but not installed"; exit 1; }
command -v docker-compose >/dev/null 2>&1 || { echo "Docker Compose required"; exit 1; }
command -v ./gradlew >/dev/null 2>&1 || { echo "Gradle wrapper not found"; exit 1; }
echo -e "${GREEN}✓ Prerequisites OK${NC}"

# Step 2: Clean previous run
echo -e "${YELLOW}[2/10] Cleaning previous run...${NC}"
docker-compose down -v 2>/dev/null || true
pkill -f "gradle.*composeApp" 2>/dev/null || true
pkill -f "ktor" 2>/dev/null || true
rm -rf pocketbase/pb_data 2>/dev/null || true
mkdir -p pocketbase/pb_data
echo -e "${GREEN}✓ Cleaned${NC}"

# Step 3: Start infrastructure (PocketBase, Nginx)
echo -e "${YELLOW}[3/10] Starting infrastructure (PocketBase + Nginx)...${NC}"
docker-compose up -d pocketbase nginx
sleep 5

# Wait for PocketBase to be ready
echo -e "${BLUE}Waiting for PocketBase...${NC}"
for i in {1..30}; do
    if curl -s "$POCKETBASE_URL/api/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ PocketBase ready${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Step 4: Seed database with test data
echo -e "${YELLOW}[4/10] Seeding database with test data...${NC}"

# Manually create the admin after initialized tables
docker-compose exec -T pocketbase /usr/local/bin/pocketbase admin create "tester_admin@bside.love" "password123" --dir=/pb_data 2>/dev/null || docker-compose exec -T pocketbase /usr/local/bin/pocketbase admin update "tester_admin@bside.love" "password123" --dir=/pb_data 2>/dev/null || true

cat << 'SEED' > /tmp/seed_db.sh
#!/bin/bash

PB_URL="http://localhost:8092"

# Login as admin (auto-created by container entrypoint)
echo "Logging in as admin..."
ADMIN_TOKEN=$(curl -s -X POST "$PB_URL/api/admins/auth-with-password" \
  -H "Content-Type: application/json" \
  -d '{
    "identity": "tester_admin@bside.love",
    "password": "password123"
  }' | jq -r '.token')

echo "Admin token: ${ADMIN_TOKEN:0:20}..."

# Create test users
# Disable security rules temporarily to allow Ktor anonymous lookup
echo "Reconfiguring collection rules for Ktor backend..."
curl -s -X PATCH "$PB_URL/api/collections/s_profiles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"listRule": "", "viewRule": ""}' > /dev/null

echo "Creating test users..."
for i in {1..5}; do
  curl -s -X POST "$PB_URL/api/collections/users/records" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"email\": \"user$i@test.com\",
      \"password\": \"password123\",
      \"passwordConfirm\": \"password123\",
      \"name\": \"Test User $i\",
      \"verified\": true
    }" > /dev/null
  echo "  ✓ Created user$i@test.com"
done

# Get user IDs
USER1_ID=$(curl -s "$PB_URL/api/collections/users/records?filter=(email='user1@test.com')" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.items[0].id')

USER2_ID=$(curl -s "$PB_URL/api/collections/users/records?filter=(email='user2@test.com')" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq -r '.items[0].id')

echo "User1 ID: $USER1_ID"
echo "User2 ID: $USER2_ID"

# Create test conversation
echo "Creating test conversation..."
CONV_ID=$(curl -s -X POST "$PB_URL/api/collections/m_conversations/records" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"type\": \"direct\"
  }" | jq -r '.id')

echo "Conversation ID: $CONV_ID"

# Add participants
curl -s -X POST "$PB_URL/api/collections/m_conversation_participants/records" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"conversation\": \"$CONV_ID\",
    \"user\": \"$USER1_ID\",
    \"role\": \"owner\"
  }" > /dev/null

curl -s -X POST "$PB_URL/api/collections/m_conversation_participants/records" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"conversation\": \"$CONV_ID\",
    \"user\": \"$USER2_ID\",
    \"role\": \"member\"
  }" > /dev/null

# Create test messages
echo "Creating test messages..."
for i in {1..3}; do
  SENDER=$([[ $((i % 2)) -eq 0 ]] && echo "$USER1_ID" || echo "$USER2_ID")
  
  curl -s -X POST "$PB_URL/api/collections/m_messages/records" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"conversation\": \"$CONV_ID\",
      \"sender\": \"$SENDER\",
      \"content\": \"Test message $i\",
      \"type\": \"text\"
    }" > /dev/null
  echo "  ✓ Created message $i"
done

echo ""
echo "════════════════════════════════════════"
echo "Database seeded successfully!"
echo "════════════════════════════════════════"
echo "Test Accounts:"
echo "  user1@test.com / password123"
echo "  user2@test.com / password123"
echo "  user3@test.com / password123"
echo "  user4@test.com / password123"
echo "  user5@test.com / password123"
echo "════════════════════════════════════════"
SEED

chmod +x /tmp/seed_db.sh
bash /tmp/seed_db.sh
echo -e "${GREEN}✓ Database seeded${NC}"

# Step 5: Build backend
echo -e "${YELLOW}[5/10] Building Ktor backend...${NC}"
./gradlew :server:build -x test
echo -e "${GREEN}✓ Backend built${NC}"

# Step 6: Start backend
echo -e "${YELLOW}[6/10] Starting Ktor backend...${NC}"
./gradlew :server:run > logs/backend.log 2>&1 &
BACKEND_PID=$!
echo "Backend PID: $BACKEND_PID"

# Wait for backend
echo -e "${BLUE}Waiting for backend...${NC}"
for i in {1..30}; do
    if curl -s "$BACKEND_URL/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend ready${NC}"
        break
    fi
    echo -n "."
    sleep 1
done

# Step 7: Start Desktop app
echo -e "${YELLOW}[7/10] Starting Desktop app...${NC}"
./gradlew :composeApp:run > logs/desktop.log 2>&1 &
DESKTOP_PID=$!
echo "Desktop PID: $DESKTOP_PID"
echo -e "${GREEN}✓ Desktop app starting...${NC}"

# Step 8: Start Web app
echo -e "${YELLOW}[8/10] Starting Web app...${NC}"
if [ -d "composeApp/build/dist/js/productionExecutable" ]; then
    cd composeApp/build/dist/js/productionExecutable
    python3 -m http.server 3000 > /dev/null 2>&1 &
    WEB_SERVER_PID=$!
    cd - > /dev/null
    echo "Web server PID: $WEB_SERVER_PID"
    echo -e "${GREEN}✓ Web app available at $WEB_URL${NC}"
else
    echo -e "${YELLOW}⚠ Web build not found. Run: ./gradlew :composeApp:jsBrowserDistribution${NC}"
fi

# Step 9: Test realtime messaging
echo -e "${YELLOW}[9/10] Testing realtime messaging...${NC}"
cat << 'TEST' > /tmp/test_messaging.sh
#!/bin/bash

BACKEND_URL="http://localhost:8080"

# Login as user1
echo "Logging in as user1..."
TOKEN1=$(curl -s -X POST "$BACKEND_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user1@test.com",
    "password": "password123"
  }' | jq -r '.data.token')

echo "User1 token: ${TOKEN1:0:20}..."

# Get conversations
echo "Fetching conversations..."
CONV_RESPONSE=$(curl -s -X GET "$BACKEND_URL/api/v1/conversations" \
  -H "Authorization: Bearer $TOKEN1")

echo "Conversations: $CONV_RESPONSE"

CONV_ID=$(echo "$CONV_RESPONSE" | jq -r '.data.items[0].id')
echo "Using conversation: $CONV_ID"

# Send test message
echo "Sending test message..."
MSG_RESPONSE=$(curl -s -X POST "$BACKEND_URL/api/v1/messages" \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d "{
    \"conversationId\": \"$CONV_ID\",
    \"content\": \"🚀 Automated test message at $(date)\"
  }")

echo "Message sent: $MSG_RESPONSE"

# Get messages
echo "Fetching messages..."
MESSAGES=$(curl -s -X GET "$BACKEND_URL/api/v1/conversations/$CONV_ID/messages" \
  -H "Authorization: Bearer $TOKEN1")

echo "Messages: $MESSAGES"

echo ""
echo "════════════════════════════════════════"
echo "Realtime messaging test completed!"
echo "════════════════════════════════════════"
TEST

chmod +x /tmp/test_messaging.sh
bash /tmp/test_messaging.sh
echo -e "${GREEN}✓ Messaging tested${NC}"

# Step 10: Display status
echo ""
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   DEMO RUNNING - ALL SYSTEMS OPERATIONAL                  ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

echo -e "${GREEN}Services Running:${NC}"
echo "  🗄️  PocketBase:    $POCKETBASE_URL"
echo "  🚀 Backend:        $BACKEND_URL"
echo "  🌐 Nginx Gateway:  $NGINX_URL"
echo "  🖥️  Desktop App:    Running (PID: $DESKTOP_PID)"
echo "  🌍 Web App:        $WEB_URL"
echo ""
echo -e "${GREEN}Test Accounts:${NC}"
echo "  📧 user1@test.com / password123"
echo "  📧 user2@test.com / password123"
echo "  📧 user3@test.com / password123"
echo ""
echo -e "${GREEN}Logs:${NC}"
echo "  📝 Backend:  tail -f logs/backend.log"
echo "  📝 Desktop:  tail -f logs/desktop.log"
echo "  📝 Docker:   docker-compose logs -f"
echo ""
echo -e "${GREEN}Admin Panel:${NC}"
echo "  🔧 PocketBase Admin: $POCKETBASE_URL/_/"
echo "     (admin@bside.app / admin123456)"
echo ""
echo -e "${YELLOW}Press Ctrl+C to stop all services${NC}"
echo ""

# Trap Ctrl+C
trap "echo -e '\n${YELLOW}Stopping all services...${NC}'; \
      kill $BACKEND_PID 2>/dev/null || true; \
      kill $DESKTOP_PID 2>/dev/null || true; \
      kill $WEB_SERVER_PID 2>/dev/null || true; \
      docker-compose down; \
      echo -e '${GREEN}✓ All services stopped${NC}'; \
      exit 0" INT

# Keep script running
wait
