# B-Side Justfile

set shell := ["bash", "-c"]

# Listing available commands

default:
    @just --list

# --- Golden Path ---

# Run all possible targets and the backend
start:
    node scripts/start.js

# --- Backend Stack ---

# Start backend services (PocketBase + Server) in Docker

up:
    @echo "Building Server JAR..."
    ./gradlew :server:shadowJar
    @echo "Starting Docker Stack..."
    docker-compose up --build

# Stop backend services

down:
    docker-compose down

# Restart backend services

restart: down up

# Run manual setup script for PocketBase (Local Binary)

pb-local:
    ./scripts/setup_dev_env.sh

# Run Ktor Server locally (JVM)

server-local:
    ./gradlew :server:run

# --- Frontend Clients ---

# Run Web Client (Hot Reload)

web:
    ./gradlew :composeApp:jsBrowserDevelopmentRun

# Run Desktop Client (Standard)

desktop:
    ./gradlew :composeApp:jvmRun

# Run Desktop Client (Hot Reload)

desktop-hot:
    ./gradlew :composeApp:hotRunJvm

# Install Android Debug App

android:
    ./gradlew :composeApp:installDebug

# Open iOS Project in Xcode

ios:
    open iosApp/iosApp.xcodeproj

# --- Database Migrations ---

# Run database migrations

migrate:
    cd pocketbase && npm run migrate:up

# Check migration status

migrate-status:
    cd pocketbase && npm run migrate:status

# Rollback last batch of migrations

migrate-down:
    cd pocketbase && npm run migrate:down

# Create a new migration

migrate-create NAME:
    cd pocketbase && npm run migrate:create {{NAME}}

# --- Schema Validation ---

# Export current schema to file

schema-export:
    cd pocketbase && npm run schema:export

# Validate current schema against prod snapshot

schema-validate:
    cd pocketbase && npm run schema:validate

# Compare schema with specific snapshot

schema-diff SNAPSHOT:
    cd pocketbase && npm run schema:diff {{SNAPSHOT}}

# --- Migration Testing Workflow ---

# Test migrations on fresh local DB (SAFE - destroys only local data)

test-migrations:
    @echo "⚠️  WARNING: This will DESTROY local PocketBase data!"
    @echo "Press Ctrl+C to cancel, or Enter to continue..."
    @read confirmation
    docker-compose down -v
    @echo "Starting fresh PocketBase..."
    docker-compose up -d pocketbase
    @echo "Waiting for PocketBase to start..."
    sleep 10
    @echo "Applying migrations..."
    cd pocketbase && npm run migrate:up
    @echo "Validating schema..."
    cd pocketbase && npm run schema:validate
    @echo "✅ Test complete!"

# Quick test: Check current migration status

test-migration-status:
    docker-compose up -d pocketbase
    sleep 5
    cd pocketbase && npm run migrate:status

# Apply migrations to PROD (DANGER - requires confirmation)

migrate-prod:
    @echo "🚨 DANGER: This will apply migrations to PRODUCTION!"
    @echo "Have you tested locally? (yes/no)"
    @read confirmation; \
    if [ "$$confirmation" != "yes" ]; then \
        echo "❌ Aborted. Test locally first with: just test-migrations"; \
        exit 1; \
    fi
    @echo "Enter PROD PocketBase URL:"
    @read url; \
    POCKETBASE_URL=$$url cd pocketbase && npm run migrate:up

# Full validation workflow (recommended before PROD deploy)

validate-all:
    @echo "🔍 Running full validation workflow..."
    @echo "1. Starting PocketBase..."
    docker-compose up -d pocketbase
    sleep 5
    @echo "2. Checking migration status..."
    cd pocketbase && npm run migrate:status
    @echo "3. Validating schema..."
    cd pocketbase && npm run schema:validate
    @echo "4. Exporting current schema..."
    cd pocketbase && npm run schema:export
    @echo "✅ Validation complete!"
