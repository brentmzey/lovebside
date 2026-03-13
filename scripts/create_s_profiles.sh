#!/bin/bash
# Re-creates s_profiles collection if missing and seeds Alice's profile

# 1. Admin Auth
TOKEN=$(curl -s -X POST http://localhost:8092/api/collections/_superusers/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"admin@bside.love","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
    echo "Admin auth failed with password123. Trying password123456..."
    TOKEN=$(curl -s -X POST http://localhost:8092/api/collections/_superusers/auth-with-password \
      -H "Content-Type: application/json" \
      -d '{"identity":"admin@bside.love","password":"password123456"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)
fi

if [ -z "$TOKEN" ]; then
    echo "Failed to get admin token."
    exit 1
fi

echo "Got Admin Token."

# 2. Check/Create Collection
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "Authorization: $TOKEN" http://localhost:8092/api/collections/s_profiles)

if [ "$STATUS" == "404" ]; then
    echo "Creating 's_profiles' collection..."
    SCHEMA='{
        "name": "s_profiles",
        "type": "base",
        "schema": [
            {"name": "userId", "type": "text", "required": true},
            {"name": "firstName", "type": "text"},
            {"name": "lastName", "type": "text"},
            {"name": "birthDate", "type": "text"},
            {"name": "seeking", "type": "select", "options": {"values": ["Friendship", "Relationship", "Both"]}}
        ],
        "listRule": "",
        "viewRule": "",
        "createRule": "@request.auth.id != \"\"",
        "updateRule": "@request.auth.id != \"\"",
        "deleteRule": "@request.auth.id != \"\""
    }'
    curl -X POST -H "Authorization: $TOKEN" -H "Content-Type: application/json" -d "$SCHEMA" http://localhost:8092/api/collections
    echo "Collection created."
else
    echo "'s_profiles' already exists."
fi

# 3. Create Profile for Alice
echo "Ensuring profile for Alice..."
ALICE_TOKEN=$(curl -s -X POST -H "Content-Type: application/json" -d '{"identity":"alice@test.com","password":"password123"}' http://localhost:8092/api/collections/users/auth-with-password | grep -o '"token":"[^"]*' | cut -d'"' -f4)
ALICE_ID=$(curl -s -X POST -H "Content-Type: application/json" -d '{"identity":"alice@test.com","password":"password123"}' http://localhost:8092/api/collections/users/auth-with-password | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -z "$ALICE_ID" ]; then
    echo "Alice not found."
    exit 1
fi

# Check existing profile
PROF_COUNT=$(curl -s -G http://localhost:8092/api/collections/s_profiles/records -H "Authorization: $ALICE_TOKEN" --data-urlencode "filter=(userId='$ALICE_ID')" | grep -o '"totalItems":[0-9]*' | cut -d: -f2)

if [ "$PROF_COUNT" == "0" ] || [ -z "$PROF_COUNT" ]; then
    echo "Creating profile for Alice..."
    curl -X POST -H "Authorization: $ALICE_TOKEN" -H "Content-Type: application/json" \
      -d "{\"userId\": \"$ALICE_ID\", \"firstName\": \"Alice\", \"lastName\": \"Wonderland\", \"birthDate\": \"1990-01-01\", \"seeking\": \"Both\"}" \
      http://localhost:8092/api/collections/s_profiles/records
    echo "Profile created."
else
    echo "Alice already has a profile."
fi
