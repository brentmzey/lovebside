#!/bin/bash

# Seed Demo Data for B-Side
# usage: ./scripts/seed_for_demo.sh

PB_PORT=8090
PB_HOST="127.0.0.1"
PB_URL="http://$PB_HOST:$PB_PORT"
ADMIN_EMAIL="tester_admin@bside.love"
ADMIN_PASS="password123"

echo "Using PocketBase at $PB_URL"

# 1. Authenticate as Admin
echo "Authenticating..."
AUTH_RESP=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" "$PB_URL/api/collections/_superusers/auth-with-password")
TOKEN=$(echo $AUTH_RESP | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Failed to get token. Response: $AUTH_RESP"
    exit 1
fi
echo "Got Token."

AUTH_HEADER="Authorization: $TOKEN"
CONTENT_TYPE="Content-Type: application/json"

# 2. Create Users
create_user() {
    EMAIL=$1
    PASSWORD="password123"
    NAME=$2
    AVATAR=$3 # Optional
    
    echo "Checking for user $EMAIL..." >&2
    # Check if exists
    EXISTING_ID=$(curl -s -H "$AUTH_HEADER" "$PB_URL/api/collections/users/records?filter=(email='$EMAIL')" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [ ! -z "$EXISTING_ID" ]; then
        echo " -> Found existing user: $EXISTING_ID" >&2
        echo "$EXISTING_ID"
        return
    fi

    echo "Creating user $NAME ($EMAIL)..." >&2
    RESP=$(curl -s -X POST -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d "{
        \"email\": \"$EMAIL\",
        \"password\": \"$PASSWORD\",
        \"passwordConfirm\": \"$PASSWORD\",
        \"name\": \"$NAME\",
        \"emailVisibility\": true
    }" "$PB_URL/api/collections/users/records")
    
    ID=$(echo $RESP | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    # If ID is empty, print error to stderr
    if [ -z "$ID" ]; then 
        echo "Error creating user. Response: $RESP" >&2
    fi
    echo "$ID"
}

ALICE_ID=$(create_user "alice@bside.love" "Alice Wonderland")
BOB_ID=$(create_user "bob@bside.love" "Bob Builder")

if [ -z "$ALICE_ID" ] || [ -z "$BOB_ID" ]; then
    echo "Failed to get User IDs. Check PocketBase logs."
    exit 1
fi

# 3. Create Conversation
echo "Creating Conversation..."
CONV_RESP=$(curl -s -X POST -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d "{
    \"type\": \"direct\",
    \"participants\": [\"$ALICE_ID\", \"$BOB_ID\"],
    \"name\": \"Alice & Bob\"
}" "$PB_URL/api/collections/conversations/records")

CONV_ID=$(echo $CONV_RESP | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
echo " -> Conversation ID: $CONV_ID"

if [ -z "$CONV_ID" ]; then
   # Try finding it
    CONV_ID=$(curl -s -H "$AUTH_HEADER" "$PB_URL/api/collections/conversations/records" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    echo " -> Used existing Conversation: $CONV_ID"
fi


# 4. Send Messages
send_message() {
    FROM_ID=$1
    CONTENT=$2
    
    echo "Sending from $FROM_ID: $CONTENT"
    curl -s -X POST -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d "{
        \"conversation_id\": \"$CONV_ID\",
        \"author_id\": \"$FROM_ID\",
        \"content\": \"$CONTENT\"
    }" "$PB_URL/api/collections/m_messages/records" > /dev/null
}

send_message $BOB_ID "Hey Alice! How's it going?"
sleep 1
send_message $ALICE_ID "Hi Bob! Pretty good, just testing this app."
sleep 1
send_message $BOB_ID "Nice! The real-time messaging seems to be working."
sleep 1
send_message $BOB_ID "Are those screenshots ready?"

echo "Seeding Complete."
