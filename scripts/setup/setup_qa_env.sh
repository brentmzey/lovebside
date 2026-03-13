#!/bin/bash

# Base URL
PB_URL="http://127.0.0.1:8090"

echo "=== B-Side QA Environment Setup ==="
echo "Setting up Alice and Bob as a confirmed match for cross-device testing."

# 1. Authenticate Admin
TOKEN=$(curl -s -X POST $PB_URL/api/collections/_superusers/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"admin@bside.love","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get admin token. ensure PocketBase is running."
  exit 1
fi

# Helper to ensure user
ensure_user() {
    EMAIL=$1
    NAME=$2
    
    # Check if exists
    USER_ID=$(curl -s -G $PB_URL/api/collections/users/records \
       -H "Authorization: $TOKEN" \
       --data-urlencode "filter=(email='$EMAIL')" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

    if [ -z "$USER_ID" ]; then
        CREATE_RES=$(curl -s -X POST $PB_URL/api/collections/users/records \
            -H "Authorization: $TOKEN" \
            -H "Content-Type: application/json" \
            -d "{
              \"username\": \"${NAME// /_}\",
              \"email\": \"$EMAIL\",
              \"password\": \"password123\",
              \"passwordConfirm\": \"password123\",
              \"name\": \"$NAME\",
              \"verified\": true
            }")
        USER_ID=$(echo $CREATE_RES | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
        echo "Created User: $NAME ($EMAIL)"
    else
        echo "User exists: $NAME ($USER_ID)"
    fi
    # Create Profile if missing
     PROFILE_ID=$(curl -s -G $PB_URL/api/collections/s_profiles/records \
       -H "Authorization: $TOKEN" \
       --data-urlencode "filter=(userId='$USER_ID')" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
     
     if [ -z "$PROFILE_ID" ]; then
        curl -s -X POST $PB_URL/api/collections/s_profiles/records \
          -H "Authorization: $TOKEN" \
          -H "Content-Type: application/json" \
          -d "{\"userId\": \"$USER_ID\", \"firstName\": \"$NAME\", \"birthDate\": \"1995-01-01 12:00:00.000Z\"}" > /dev/null
     fi

    eval "$3='$USER_ID'"
}

# 2. Get/Create Users
ensure_user "alice@bside.love" "Alice QA" ID_ALICE
ensure_user "bob@bside.love" "Bob QA" ID_BOB

# 3. Create Matches (Mutual Accepted)
echo "creating Mutual Match..."

# Check if match exists
MATCH_CHECK=$(curl -s -G $PB_URL/api/collections/m_matches/records \
    -H "Authorization: $TOKEN" \
    --data-urlencode "filter=(user_id='$ID_ALICE' && matched_user_id='$ID_BOB')")
    
COUNT=$(echo $MATCH_CHECK | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)

if [ "$COUNT" -eq "0" ]; then
    # Create Alice -> Bob (Accepted)
    curl -s -X POST $PB_URL/api/collections/m_matches/records \
        -H "Authorization: $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"user_id\": \"$ID_ALICE\",
            \"matched_user_id\": \"$ID_BOB\",
            \"status\": \"accepted\",
            \"match_score\": 95
        }" > /dev/null
        
    # Create Bob -> Alice (Accepted)
    curl -s -X POST $PB_URL/api/collections/m_matches/records \
        -H "Authorization: $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{
            \"user_id\": \"$ID_BOB\",
            \"matched_user_id\": \"$ID_ALICE\",
            \"status\": \"accepted\",
            \"match_score\": 95
        }" > /dev/null
    echo "✅ Match Created: Alice <-> Bob"
else
    echo "✅ Match already exists."
fi

# 4. Instructions
echo ""
echo "=== SETUP COMPLETE ==="
echo "You can now perform Cross-Device Testing:"
echo ""
echo "🤖 Android Emulator:"
echo "   - Login: bob@bside.love"
echo "   - Pass:  password123"
echo ""
echo "🍎 iOS Simulator:"
echo "   - Login: alice@bside.love"
echo "   - Pass:  password123"
echo ""
echo "🚀 Action: They are already matched. Go to 'Matches' tab and start chatting!"
