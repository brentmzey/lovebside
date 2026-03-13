# B-Side Justfile

set shell := ["bash", "-c"]

# Listing available commands

default:
    @just --list

# --- Golden Path ---

# Interactive development startup (recommended)
dev:
    ./scripts/dev/dev-start.sh

# Start just the backend services (PocketBase + Ktor)
backend:
    ./scripts/dev/backend-start.sh

# Run all possible targets and the backend (automated)
start:
    node scripts/dev/start.js

# Stop all B-Side processes and Docker containers
stop:
    ./scripts/dev/stop-all.sh

# Open project in Android Studio
android-studio:
    ./scripts/dev/open-android-studio.sh

# --- Backend Stack ---

# Start backend services (PocketBase + Server) in Docker

up:
    @echo "Building Server JAR..."
    ./gradlew :server:shadowJar
    @echo "Starting Docker Stack..."
    bash -c 'trap "docker-compose down" EXIT INT TERM; docker-compose up --build'

# Stop backend services

down:
    docker-compose down

# Restart backend services

restart: down up

# Run manual setup script for PocketBase (Local Binary)

pb-local:
    ./scripts/setup/setup_dev_env.sh

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
    ./scripts/test/test-migrations.sh

# Quick test: Check current migration status

test-migration-status:
    bash -c 'trap "docker-compose stop pocketbase" EXIT; docker-compose up -d pocketbase; sleep 5; cd pocketbase && npm run migrate:status'

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
    ./scripts/test/validate-all.sh

# --- Phase Management ---

# Run Phase 0 validation (test all clients before deployment)

phase-0:
    @echo "🧪 Starting Phase 0: Pre-Deployment Validation"
    @echo "This will test all client targets and backend services"
    @echo ""
    ./scripts/test/validate-phase-0.sh

# Run complete testing walkthrough (all environments)

phase-test:
    @echo "🧪 Starting Complete Stack Testing Walkthrough"
    @echo "Tests: dev, staging, production configs"
    @echo ""
    ./scripts/test/test-walkthrough.sh

# View Phase 0 status and checklist

phase-status:
    @echo "📋 Phase 0 Status - Pre-Deployment Validation"
    @echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    @echo ""
    @echo "MUST COMPLETE before deployment:"
    @echo "  [ ] Backend validated"
    @echo "  [ ] Desktop client tested"
    @echo "  [ ] Web client tested"
    @echo "  [ ] Android client tested"
    @echo "  [ ] iOS client tested"
    @echo "  [ ] Integration tests passed"
    @echo ""
    @echo "📝 Full checklist: .code-hq/PROJECT_TRACKER.md"
    @echo "🧪 Run validation: just phase-0"
    @echo "📖 Testing guide: docs/guides/TESTING_GUIDE.md"
    @echo ""

# Quick validation - just backend and one client

phase-quick:
    @echo "⚡ Quick Phase 0 Validation"
    @echo "━━━━━━━━━━━━━━━━━━━━━━━━"
    @echo ""
    @echo "1. Starting backend..."
    just backend &
    @sleep 10
    @echo ""
    @echo "2. Testing backend health..."
    @curl -sf http://localhost:8092/api/health > /dev/null && echo "   ✅ PocketBase: OK" || echo "   ❌ PocketBase: FAIL"
    @curl -sf http://localhost:8081/health > /dev/null && echo "   ✅ Ktor: OK" || echo "   ❌ Ktor: FAIL"
    @echo ""
    @echo "3. Building desktop client..."
    @./gradlew :composeApp:jvmJar --quiet && echo "   ✅ Desktop build: OK" || echo "   ❌ Desktop build: FAIL"
    @echo ""
    @echo "✅ Quick validation complete!"
    @echo "📝 Run full validation: just phase-0"
    @echo ""
