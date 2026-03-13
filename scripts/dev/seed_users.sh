#!/bin/bash
# Seed test users (Alice and Bob)

# 1. Get Admin Token
TOKEN=$(curl -s -X POST http://127.0.0.1:8090/api/collections/_superusers/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"admin@bside.love","password":"password123"}' | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Failed to get admin token"
  exit 1
fi

# Helper function to ensure user exists and is verified
ensure_user() {
  EMAIL=$1
  USERNAME=$2
  NAME=$3
  
  echo "Ensuring user $EMAIL..."
  # Try to create
  CREATE_RES=$(curl -s -X POST http://127.0.0.1:8090/api/collections/users/records \
    -H "Authorization: $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"username\": \"$USERNAME\",
      \"email\": \"$EMAIL\",
      \"emailVisibility\": true,
      \"password\": \"password123\",
      \"passwordConfirm\": \"password123\",
      \"name\": \"$NAME\",
      \"verified\": true
    }")

  if echo "$CREATE_RES" | grep -q "validation_not_unique"; then
    echo "User exists. Updating to verified..."
    # Find ID
    ID=$(curl -s -G http://127.0.0.1:8090/api/collections/users/records \
       -H "Authorization: $TOKEN" \
       --data-urlencode "filter=(email='$EMAIL')" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)
    
    if [ -n "$ID" ]; then
        curl -s -X PATCH "http://127.0.0.1:8090/api/collections/users/records/$ID" \
          -H "Authorization: $TOKEN" \
          -H "Content-Type: application/json" \
          -d '{"verified": true}'
        echo "User $EMAIL updated."
    else
        echo "Could not find ID for $EMAIL to update."
    fi
  else
    echo "User created: $EMAIL"
  fi
}

# 2. Setup Users
ensure_user "alice@bside.love" "alice_test" "Alice Tests"
ensure_user "bob@bside.love" "bob_test" "Bob Tests"

echo "Seeding/Verifying complete."
