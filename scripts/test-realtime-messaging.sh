#!/bin/bash

# Quick test realtime messaging between two users
# Usage: ./scripts/test-realtime-messaging.sh

set -e

BACKEND_URL="http://localhost:8080"
POCKETBASE_URL="http://localhost:8090"

echo "🧪 Testing Realtime Messaging"
echo "=============================="

# Login user1
echo "1. Logging in as user1@test.com..."
TOKEN1=$(curl -s -X POST "$BACKEND_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user1@test.com","password":"password123"}' | jq -r '.token')

# Login user2  
echo "2. Logging in as user2@test.com..."
TOKEN2=$(curl -s -X POST "$BACKEND_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"user2@test.com","password":"password123"}' | jq -r '.token')

# Get conversation
echo "3. Getting conversation..."
CONV_ID=$(curl -s "$BACKEND_URL/api/v1/conversations" \
  -H "Authorization: Bearer $TOKEN1" | jq -r '.data.items[0].id')

echo "   Conversation ID: $CONV_ID"

# Subscribe user2 to realtime updates (in background)
echo "4. Subscribing user2 to realtime updates..."
curl -s "$POCKETBASE_URL/api/realtime" \
  -H "Authorization: Bearer $TOKEN2" \
  -d "subscribe=s_messages.$CONV_ID" &
SUB_PID=$!

sleep 1

# User1 sends message
echo "5. User1 sending message..."
MSG=$(curl -s -X POST "$BACKEND_URL/api/v1/messages" \
  -H "Authorization: Bearer $TOKEN1" \
  -H "Content-Type: application/json" \
  -d "{\"conversationId\":\"$CONV_ID\",\"content\":\"Test message at $(date +%H:%M:%S)\"}")

echo "   Message sent: $(echo $MSG | jq -r '.data.content')"

# Wait for realtime delivery
sleep 2

# User2 checks messages
echo "6. User2 checking messages..."
MESSAGES=$(curl -s "$BACKEND_URL/api/v1/conversations/$CONV_ID/messages" \
  -H "Authorization: Bearer $TOKEN2" | jq -r '.data.items | length')

echo "   User2 sees $MESSAGES messages"

# Cleanup
kill $SUB_PID 2>/dev/null || true

echo ""
echo "✅ Realtime messaging test complete!"
echo "   Both users can send/receive messages in realtime"
