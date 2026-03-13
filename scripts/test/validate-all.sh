#!/bin/bash
set -e

cleanup() {
    echo -e "\n🧹 Stopping PocketBase..."
    docker-compose stop pocketbase || true
}
trap cleanup EXIT INT TERM

echo "🔍 Running full validation workflow..."

echo "1. Starting PocketBase..."
docker-compose up -d pocketbase

echo "⏳ Waiting for PocketBase..."
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

echo "2. Checking migration status..."
cd pocketbase
npm run migrate:status

echo "3. Validating schema..."
npm run schema:validate

echo "4. Exporting current schema..."
npm run schema:export

echo "✅ Validation complete!"
