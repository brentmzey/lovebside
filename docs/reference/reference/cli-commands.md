---
title: "CLI Commands Reference"
excerpt: "Complete reference for all B-Side CLI commands"
category: "reference"
slug: "cli-commands"
order: 1
---

# CLI Commands Reference

B-Side uses **Just** as a command runner for simplified development workflows.

## Quick Reference

| Command | Description |
|---------|-------------|
| `just` | List all available commands |
| `just backend` | Start backend services |
| `just dev` | Interactive development mode |
| `just start` | Start everything (backend + all clients) |
| `just stop` | Stop all services |
| `just desktop` | Run desktop app |
| `just web` | Run web app |
| `just android` | Install Android debug app |
| `just ios` | Open iOS project in Xcode |

## Backend Commands

### Start Backend
```bash
just backend
```

Starts Docker containers for:
- PocketBase (port 8092)
- Ktor Server (port 8081)  
- Redis (port 6379)
- Nginx (port 80)
- GoAccess analytics

**Output:**
```
🐳 Starting B-Side Backend Services
📦 Building server JAR...
✅ Backend services are running!
  PocketBase:      http://localhost:8092
  Ktor API:        http://localhost:8081
```

### Stop All Services
```bash
just stop
```

Stops all Docker containers and kills running client apps.

### Restart Backend
```bash
just restart
```

Equivalent to `just down && just up`.

### View Logs
```bash
docker logs -f bside-pocketbase
docker logs -f bside-server
docker logs -f bside-redis
```

## Frontend Commands

### Desktop

**Standard mode:**
```bash
just desktop
```

**With hot reload (experimental):**
```bash
just desktop-hot
```

**Create distributable:**
```bash
# macOS DMG
./gradlew :composeApp:packageDmg

# Windows MSI
./gradlew :composeApp:packageMsi

# Linux DEB
./gradlew :composeApp:packageDeb
```

### Web

**Development server:**
```bash
just web
```
Opens browser at http://localhost:8080 with hot reload.

**Production build:**
```bash
./gradlew :composeApp:jsBrowserProductionWebpack
```
Output: `composeApp/build/dist/js/productionExecutable/`

### Android

**Open Android Studio:**
```bash
just android-studio
```

**Install debug APK:**
```bash
just android
# Equivalent to: ./gradlew :composeApp:installDebug
```

**Build release APK:**
```bash
./gradlew :composeApp:assembleRelease
```
Output: `composeApp/build/outputs/apk/release/`

### iOS

**Open Xcode:**
```bash
just ios
# Equivalent to: open iosApp/iosApp.xcodeproj
```

**Build from command line:**
```bash
./gradlew :composeApp:linkReleaseFrameworkIosArm64
```

## Database Commands

### Run Migrations
```bash
just migrate
```

Applies pending database migrations.

### Check Migration Status
```bash
just migrate-status
```

Shows which migrations have been applied.

### Rollback Last Migration
```bash
just migrate-down
```

⚠️ Use with caution - rolls back the last batch of migrations.

### Create New Migration
```bash
just migrate-create migration_name
```

Creates a new migration file in `pocketbase/migrations/`.

### Test Migrations (Safe)
```bash
just test-migrations
```

Tests migrations on a fresh local database. Safe to run - doesn't affect production.

### Apply to Production
```bash
just migrate-prod
```

⚠️ **DANGER** - Applies migrations to production. Requires confirmation.

## Schema Commands

### Export Current Schema
```bash
just schema-export
```

Exports current PocketBase schema to `pocketbase/schema.json`.

### Validate Schema
```bash
just schema-validate
```

Validates current schema against production snapshot.

### Compare Schemas
```bash
just schema-diff snapshot_name
```

Shows differences between current and specific snapshot.

### Full Validation
```bash
just validate-all
```

Runs complete validation workflow:
1. Schema export
2. Schema validation
3. Migration testing
4. Type generation

## Build Commands

### Clean Build
```bash
./gradlew clean
```

### Build Everything
```bash
./gradlew build
```

### Build Specific Module
```bash
./gradlew :shared:build
./gradlew :server:build
./gradlew :composeApp:build
```

### Build with Specific Target
```bash
# Android
./gradlew :composeApp:assembleDebug

# iOS
./gradlew :composeApp:iosX64Test

# Desktop/JVM
./gradlew :composeApp:jvmJar

# Web
./gradlew :composeApp:jsBrowserDevelopmentWebpack
```

## Test Commands

### Run All Tests
```bash
./gradlew test
```

### Run Specific Tests
```bash
./gradlew :shared:test
./gradlew :server:test
./gradlew :composeApp:jvmTest
```

### Run Tests with Coverage
```bash
./gradlew test jacocoTestReport
```

Report: `build/reports/jacoco/test/html/index.html`

## Docker Commands

### Start Services
```bash
docker-compose up -d
```

### Stop Services
```bash
docker-compose down
```

### Rebuild Containers
```bash
docker-compose up -d --build
```

### View Container Status
```bash
docker-compose ps
```

### Remove All Data (⚠️ Destructive)
```bash
docker-compose down -v
```

Removes all volumes, including database data.

## Development Workflow

### Typical Daily Workflow

```bash
# 1. Start backend
just backend

# 2. In a new terminal, start your client
just desktop  # or web, android, ios

# 3. Make changes, hot reload updates automatically

# 4. When done, stop everything
just stop
```

### Full Stack Testing

```bash
# Start backend
just backend

# Open multiple clients to test real-time features
just desktop &
just web &
# Open multiple browser tabs
```

## Advanced Commands

### Custom Gradle Tasks

List all tasks:
```bash
./gradlew tasks
```

### Server JAR Only
```bash
./gradlew :server:shadowJar
```
Output: `server/build/libs/server-all.jar`

### Generate Types from Schema
```bash
cd pocketbase && npm run generate:types
```

### Run Specific Platform Tests
```bash
# JVM only
./gradlew jvmTest

# Android
./gradlew connectedAndroidTest

# iOS (requires Xcode)
./gradlew iosX64Test
```

## Environment Variables

Create `.env` file from template:
```bash
cp .env.example .env
```

**Key variables:**
```bash
POCKETBASE_URL=http://localhost:8092
API_URL=http://localhost:8081
JWT_SECRET=your-secret-key
REDIS_URL=redis://localhost:6379
```

## Troubleshooting Commands

### Check Port Usage
```bash
# macOS/Linux
lsof -i :8092
lsof -i :8081

# Windows
netstat -ano | findstr :8092
```

### Kill Processes on Port
```bash
# macOS/Linux
kill $(lsof -t -i:8092)

# Windows
taskkill /PID <PID> /F
```

### Clean Everything
```bash
# Stop all services
just stop

# Clean Gradle cache
./gradlew clean

# Remove Docker volumes
docker-compose down -v

# Clean npm
cd pocketbase && npm clean-install

# Restart fresh
just backend
```

### Check Backend Health
```bash
curl http://localhost:8092/api/health
curl http://localhost:8081/health
```

## Aliases

Add these to your `~/.bashrc` or `~/.zshrc`:

```bash
alias bside-start='just backend'
alias bside-stop='just stop'
alias bside-desktop='just desktop'
alias bside-web='just web'
alias bside-logs='docker-compose logs -f'
alias bside-clean='just stop && ./gradlew clean && docker-compose down -v'
```

---

> 💡 Pro Tip
> 
> Run `just` without arguments to see all available commands with descriptions.

> 📘 Need More Help?
> 
> Check the [Troubleshooting Guide](troubleshooting) for common issues.
