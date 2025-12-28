#!/bin/bash

# Base URL
PB_URL="http://127.0.0.1:8090"

echo "=== B-Side Affinity Algorithm Test ==="

# 1. Authenticate Admin
echo "Logging in as Admin..."
TOKEN=$(curl -s -X POST $PB_URL/api/collections/_superusers/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"admin@bside.love","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ Failed to get admin token. ensure PocketBase is running."
  exit 1
fi
echo "✅ Admin authenticated."

# Helper to create/update user + profile
setup_test_user() {
    EMAIL=$1
    NAME=$2
    INTERESTS=$3 # JSON Array string e.g. '["Music", "Tech"]'
    SEEKING=$4
    LAT=$5
    LNG=$6

    echo "--- Setting up $NAME ($EMAIL) ---"
    
    # 1. Ensure User
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
        echo "Created User ID: $USER_ID"
    else
        echo "User exists: $USER_ID"
    fi

    # 2. Upsert Profile
    # Check if profile exists
    PROFILE_ID=$(curl -s -G $PB_URL/api/collections/s_profiles/records \
       -H "Authorization: $TOKEN" \
       --data-urlencode "filter=(userId='$USER_ID')" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

    PROFILE_DATA="{
        \"userId\": \"$USER_ID\",
        \"firstName\": \"$NAME\",
        \"birthDate\": \"1995-01-01 12:00:00.000Z\",
        \"seeking\": \"$SEEKING\",
        \"interests\": $INTERESTS,
        \"lat\": $LAT,
        \"lng\": $LNG,
        \"location\": \"Test City\"
    }"

    if [ -z "$PROFILE_ID" ]; then
        curl -s -X POST $PB_URL/api/collections/s_profiles/records \
          -H "Authorization: $TOKEN" \
          -H "Content-Type: application/json" \
          -d "$PROFILE_DATA" > /dev/null
        echo "Created Profile."
    else
        curl -s -X PATCH $PB_URL/api/collections/s_profiles/records/$PROFILE_ID \
          -H "Authorization: $TOKEN" \
          -H "Content-Type: application/json" \
          -d "$PROFILE_DATA" > /dev/null
        echo "Updated Profile."
    fi
    
    # Echo UserID for external use
    eval "$7='$USER_ID'"
}

# 2. Setup Alice (Likes: Tech, Jazz; Seeking: Both; Loc: 40.71, -74.00)
setup_test_user "algo_alice@bside.love" "Alice Algo" '["Tech", "Jazz"]' "Both" 40.710 -74.006 U1

# 3. Setup Bob (Likes: Tech, Rock; Seeking: Both; Loc: 40.71, -74.00) -> High Compatibility (Location + Tech)
setup_test_user "algo_bob@bside.love" "Bob Algo" '["Tech", "Rock"]' "Both" 40.710 -74.006 U2

# 4. Setup Charlie (Likes: Cooking; Seeking: Friendship; Loc: 34.05, -118.25) -> Low Compatibility (Far away, diff interests)
setup_test_user "algo_charlie@bside.love" "Charlie Algo" '["Cooking"]' "Friendship" 34.052 -118.243 U3


echo "--- API Trigger: Running Matching Algorithm ---"
# 5. Trigger Cron
START=$(date +%s)
TRIGGER_RES=$(curl -s -X POST $PB_URL/api/crons/trigger-matching \
    -H "Authorization: $TOKEN" \
    -H "Content-Type: application/json" \
    -d '{}')

END=$(date +%s)
DIFF=$((END-START))
echo "Algorithm finished in ${DIFF}s."
echo "Response: $TRIGGER_RES"

# 6. Verify Matches
echo "--- Verification ---"

check_match() {
    U_A=$1
    U_B=$2
    EXPECT_MATCH=$3 # true/false
    
    MATCH_RES=$(curl -s -G $PB_URL/api/collections/m_matches/records \
        -H "Authorization: $TOKEN" \
        --data-urlencode "filter=(user_id='$U_A' && matched_user_id='$U_B') || (user_id='$U_B' && matched_user_id='$U_A')")
        
    COUNT=$(echo $MATCH_RES | grep -o '"totalItems":[0-9]*' | cut -d':' -f2)
    
    if [ "$EXPECT_MATCH" = "true" ]; then
        if [ "$COUNT" -ge 1 ]; then
            SCORE=$(echo $MATCH_RES | grep -o '"match_score":[0-9]*' | head -1 | cut -d':' -f2)
            echo "✅ SUCCESS: Match found between $4 and $5! Score: $SCORE"
        else
            echo "❌ FAILURE: Expected match between $4 and $5 but none found."
        fi
    else
        if [ "$COUNT" -eq 0 ]; then
             echo "✅ SUCCESS: Correctly NO match between $4 and $5."
        else
             echo "❌ FAILURE: Found unexpected match between $4 and $5."
        fi
    fi
}

check_match $U1 $U2 "true" "Alice" "Bob"
check_match $U1 $U3 "false" "Alice" "Charlie"

echo "=== Test Complete ==="
