#!/bin/sh
set -e

# Auto-create admin user on first run
# Controlled by environment variables

ADMIN_EMAIL="${ADMIN_EMAIL:-tester_admin@bside.love}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-password123}"

echo "Starting PocketBase..."

# Create/update superuser before starting (safe to run multiple times)
echo "Ensuring admin user exists: $ADMIN_EMAIL"
/usr/local/bin/pocketbase admin create "$ADMIN_EMAIL" "$ADMIN_PASSWORD" --dir=/pb_data 2>/dev/null || /usr/local/bin/pocketbase admin update "$ADMIN_EMAIL" "$ADMIN_PASSWORD" --dir=/pb_data 2>/dev/null || true

# Start PocketBase server
echo "✓ Admin user ready"
echo "✓ Starting PocketBase at http://0.0.0.0:8090"
exec /usr/local/bin/pocketbase serve --http=0.0.0.0:8090 "$@"
