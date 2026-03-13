#!/bin/bash
set -e

# Cleanup function
cleanup() {
    echo -e "\n🧹 Cleaning up..."
    # We stop the container but might want to keep volume for inspection if it failed? 
    # But usually 'test-migrations' implies a clean run.
    docker-compose stop pocketbase || true
}
trap cleanup EXIT INT TERM

echo "⚠️  WARNING: This will DESTROY local PocketBase data!"
echo "Press Ctrl+C to cancel, or Enter to continue..."
read confirmation

echo "🗑️  Resetting database..."
docker-compose down -v

echo "🐳 Starting fresh PocketBase..."
docker-compose up -d pocketbase

echo "⏳ Waiting for PocketBase to start..."
MAX_WAIT=30
WAIT_COUNT=0
until curl -f http://localhost:8092/api/health > /dev/null 2>&1; do
    if [ $WAIT_COUNT -ge $MAX_WAIT ]; then
        echo "❌ PocketBase failed to start."
        exit 1
    fi
    sleep 1
    WAIT_COUNT=$((WAIT_COUNT + 1))
done

echo "🔄 Applying migrations..."
cd pocketbase && npm run migrate:up

echo "✅ Validating schema..."
npm run schema:validate

echo "🎉 Test complete!"
