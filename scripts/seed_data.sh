#!/bin/bash
set -e

PB_URL="http://localhost:8092"
ADMIN_EMAIL="tester_admin@bside.love"
ADMIN_PASS="password123"

echo "🌱 Seeding data to $PB_URL..."

# 1. Authenticate as Admin (Superuser)
echo "🔑 Authenticating..."
# Try new endpoint first
RESPONSE=$(curl -s -X POST "$PB_URL/api/collections/_superusers/auth-with-password" \
  -H "Content-Type: application/json" \
  -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")

TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

# Fallback to old endpoint if token is empty
if [ -z "$TOKEN" ]; then
    echo "⚠️ New endpoint failed, trying legacy..."
    RESPONSE=$(curl -s -X POST "$PB_URL/api/admins/auth-with-password" \
      -H "Content-Type: application/json" \
      -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}")
    TOKEN=$(echo $RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
    echo "❌ Authentication failed. Response: $RESPONSE"
    exit 1
fi
echo "✅ Authenticated."

# 2. Check existing users
EXISTING_COUNT=$(curl -s "$PB_URL/api/collections/users/records?perPage=1" \
  -H "Authorization: $TOKEN" | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)

if [ "$EXISTING_COUNT" -ge "5" ]; then
    echo "⚠️ Found $EXISTING_COUNT users. Skipping seeding."
    exit 0
fi

# 3. Create Users
create_user() {
    EMAIL=$1
    NAME=$2
    PASS=$3
    TYPE=$4
    PROUST=$5
    
    echo "creating $EMAIL..."
    curl -s -X POST "$PB_URL/api/collections/users/records" \
      -H "Authorization: $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"email\": \"$EMAIL\",
        \"password\": \"$PASS\",
        \"passwordConfirm\": \"$PASS\",
        \"name\": \"$NAME\",
        \"connection_type\": \"$TYPE\",
        \"completed_proust_questionnaire\": $PROUST,
        \"emailVisibility\": true,
        \"verified\": true
      }" > /dev/null
}

create_user "alice@test.com" "Alice Wonderland" "password123" "friendship" true
create_user "bob@test.com" "Bob Builder" "password123" "romantic" true
create_user "charlie@test.com" "Charlie Chaplin" "password123" "friendship" false
create_user "diana@test.com" "Diana Prince" "password123" "romantic" true
create_user "evan@test.com" "Evan Alm" "password123" "friendship" true

echo "✅ Seeding complete."
