#!/bin/bash

# Setup Local Dev Environment for B-Side
# usage: ./scripts/setup_dev_env.sh

PB_PORT=8090
PB_HOST="127.0.0.1"
PB_URL="http://$PB_HOST:$PB_PORT"
PB_BIN="./pocketbase_local/pocketbase"
ADMIN_EMAIL="tester_admin@bside.love"
ADMIN_PASS="password123"

echo "Stopping existing PocketBase..."
pkill -f "pocketbase serve" || true
sleep 1

echo "Starting PocketBase ($PB_BIN)..."
nohup $PB_BIN serve --http="$PB_HOST:$PB_PORT" --dir=pocketbase/pb_data --publicDir=pocketbase/pb_public --migrationsDir=pocketbase/pb_migrations --hooksDir=pocketbase/pb_hooks --dev > pocketbase.log 2>&1 &
PB_PID=$!
echo "PocketBase PID: $PB_PID"

# Wait for startup
sleep 3

echo "Creating Admin User..."
$PB_BIN superuser upsert $ADMIN_EMAIL $ADMIN_PASS --dir=pocketbase/pb_data

echo "Authenticating to get Token..."
AUTH_RESP=$(curl -s -X POST -H "Content-Type: application/json" -d "{\"identity\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASS\"}" "$PB_URL/api/collections/_superusers/auth-with-password")
TOKEN=$(echo $AUTH_RESP | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Failed to get token. Response: $AUTH_RESP"
    exit 1
fi
echo "Got Token."

# Define Headers
AUTH_HEADER="Authorization: $TOKEN"
CONTENT_TYPE="Content-Type: application/json"

# 1. Create Conversations Collection
echo "Creating/Updating 'conversations' collection..."
# Check if exists
EXIST_CHECK=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH_HEADER" "$PB_URL/api/collections/conversations")

if [ "$EXIST_CHECK" != "200" ]; then
    curl -s -X POST -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d '{
        "name": "conversations",
        "type": "base",
        "fields": [
            {
                "name": "type",
                "type": "select",
                "required": true,
                "options": { "maxSelect": 1, "values": ["direct", "group"] }
            },
            {
                "name": "participants",
                "type": "relation",
                "required": true,
                "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": false }
            },
            {
                "name": "name",
                "type": "text"
            },
            {
                "name": "last_message_at",
                "type": "date"
            }
        ],
        "listRule": "@request.auth.id != \"\"",
        "viewRule": "@request.auth.id != \"\"",
        "createRule": "@request.auth.id != \"\"",
        "updateRule": "@request.auth.id != \"\"",
        "deleteRule": "@request.auth.id != \"\""
    }' "$PB_URL/api/collections"
    echo " -> Created 'conversations'"
else
    echo " -> 'conversations' already exists."
fi

# 2. Update Messages schema (assumed to exist via base migration, or create if missing)
echo "Ensuring 'm_messages' collection..."
# For this demo, let's assume we might need to create it if we wiped data
EXIST_CHECK_MSG=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH_HEADER" "$PB_URL/api/collections/m_messages")

if [ "$EXIST_CHECK_MSG" != "200" ]; then
    echo " -> Creating 'm_messages' base..."
    # We need conversation ID but creating with relation requires it to exist.
    # Actually, we can just refer by name or ID.
    # Get conversation ID
    CONV_ID=$(curl -s -H "$AUTH_HEADER" "$PB_URL/api/collections/conversations" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    curl -s -X POST -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d '{
        "name": "m_messages",
        "type": "base",
        "fields": [
            {
                "name": "content",
                "type": "text"
            },
             {
                "name": "conversation_id",
                "type": "relation",
                "required": true,
                "options": { "collectionId": "'$CONV_ID'", "cascadeDelete": true, "maxSelect": 1 }
            },
             {
                "name": "author_id",
                "type": "relation",
                "required": true,
                "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": false, "maxSelect": 1 }
            }
        ],
        "listRule": "@request.auth.id != \"\"",
        "viewRule": "@request.auth.id != \"\"",
        "createRule": "@request.auth.id != \"\"",
        "updateRule": "@request.auth.id != \"\"",
        "deleteRule": "@request.auth.id != \"\""
    }' "$PB_URL/api/collections"
    echo " -> Created 'messages'"
fi
# Now Update 'm_messages' with Threading fields if needed
echo "Updating 'm_messages' schema for threading..."
# We fetch the current collection to check fields or just blindly PATCH
MSG_INFO=$(curl -s -H "$AUTH_HEADER" "$PB_URL/api/collections/m_messages")
MSG_ID=$(echo $MSG_INFO | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

# Check if reply_to exists
HAS_THREADING=$(echo $MSG_INFO | grep "reply_to_message_id")

if [ -z "$HAS_THREADING" ]; then
    echo " -> Patching 'm_messages' with threading fields..."
    # We must construct the NEW schema list.
    # A bit complex in bash to append to existing schema JSON.
    # However, since we defined the base schema above, we know it has 3 fields.
    # We can just overwrite the schema with Base + New fields.
    
    # Base Schema + New Fields
    curl -s -X PATCH -H "$AUTH_HEADER" -H "$CONTENT_TYPE" -d '{
        "fields": [
            {
                "name": "content",
                "type": "text"
            },
             {
                "name": "conversation_id",
                "type": "relation",
                "required": true,
                "options": { "collectionId": "'$CONV_ID'", "cascadeDelete": true, "maxSelect": 1 }
            },
             {
                "name": "author_id",
                "type": "relation",
                "required": true,
                "options": { "collectionId": "_pb_users_auth_", "cascadeDelete": false, "maxSelect": 1 }
            },
            {
                "name": "reply_to_message_id",
                "type": "relation",
                "options": { "collectionId": "'$MSG_ID'", "cascadeDelete": false, "maxSelect": 1 }
            },
            {
                "name": "thread_root_id",
                "type": "relation",
                "options": { "collectionId": "'$MSG_ID'", "cascadeDelete": false, "maxSelect": 1 }
            },
            {
                "name": "thread_depth",
                "type": "number",
                "options": { "noDecimal": true }
            },
            {
                "name": "attachments",
                "type": "file",
                "options": {
                    "maxSelect": 10,
                    "maxSize": 52428800,
                    "mimeTypes": ["image/jpeg", "image/png", "image/gif", "video/mp4"],
                    "thumbs": ["200x200"],
                    "protected": false
                }
            }
        ]
    }' "$PB_URL/api/collections/$MSG_ID"
    echo " -> Patched schema."
else
    echo " -> Threading fields already exist."
fi

echo "Environment Setup Complete."
