#!/bin/bash

# Configuration
PORT=8099
PB_DIR="pocketbase_verify_data"
ADMIN_EMAIL="verify@bside.love"
ADMIN_PASS="password123"

# Cleanup function
cleanup() {
    echo "🧹 Cleaning up..."
    if [ -n "$PID" ]; then
        kill $PID 2>/dev/null
    fi
    rm -rf $PB_DIR
    rm -f pocketbase_verify.log
}

# Trap interrupts
trap cleanup EXIT

# 1. Setup Environment
echo "🚀 Setting up verification environment..."
rm -rf $PB_DIR
mkdir -p $PB_DIR

# 2. Create Admin & Start Server
echo "👤 Creating admin user..."
./pocketbase/pocketbase superuser create $ADMIN_EMAIL $ADMIN_PASS --dir=./$PB_DIR/pb_data > /dev/null

echo "🔌 Starting PocketBase on port $PORT..."
./pocketbase/pocketbase serve --http=127.0.0.1:$PORT --dir=./$PB_DIR/pb_data --migrationsDir=./pocketbase/pb_migrations > pocketbase_verify.log 2>&1 &
PID=$!

# Wait for server
echo "⏳ Waiting for server to be ready..."
for i in {1..30}; do
  if nc -z 127.0.0.1 $PORT; then
    echo "✅ Server is up!"
    break
  fi
  sleep 1
done

# 3. Run Tests
echo "🧪 Running Integration Tests..."
echo "   (This verifies Reactions, Presence, and Messaging Threads)"

# We need to tell the test to use our custom port. 
# Since the test has hardcoded 8091, we'll just sed it temporarily in a copy or assume the user is okay with us modifying the test file slightly? 
# Better: Pass it as a system property? The test doesn't read system properties currently.
# For now, I'll rely on the standard test port 8091 in the script to match the code.
# Let's restart with port 8091.
kill $PID 2>/dev/null
PORT=8091
./pocketbase/pocketbase serve --http=127.0.0.1:$PORT --dir=./$PB_DIR/pb_data --migrationsDir=./pocketbase/pb_migrations > pocketbase_verify.log 2>&1 &
PID=$!
sleep 2

# Export admin credentials for test to use
export PB_ADMIN_EMAIL="$ADMIN_EMAIL"
export PB_ADMIN_PASSWORD="$ADMIN_PASS"

./gradlew :shared:jvmTest --tests "love.bside.app.integration.MessagingThreadIntegrationTest"

TEST_EXIT_CODE=$?

if [ $TEST_EXIT_CODE -eq 0 ]; then
    echo ""
    echo "🎉 SUCCESS: All messaging backend features verified!"
    echo ""
    echo "👀 Want to inspect the data?"
    echo "   1. Open: http://localhost:$PORT/_/"
    echo "   2. Login: $ADMIN_EMAIL / $ADMIN_PASS"
    echo "   3. Check collections: m_reactions, m_presence, m_messages"
    echo ""
    read -p "Press [Enter] to stop the server and cleanup..."
else
    echo "❌ Tests Failed. Check pocketbase_verify.log for server details."
fi
