# Justfile Command Reference

Quick reference for all `just` commands available in the B-Side project.

---

## 📦 Services

### Start/Stop Services

```bash
just up              # Start all services (PocketBase + Ktor + Nginx)
just down            # Stop all services
just restart         # Restart all services
just logs [service]  # View logs (optional: specify pocketbase, server, or nginx)
```

### Individual Services

```bash
just pb              # Start only PocketBase
just backend         # Start only Ktor backend
```

---

## 🏗️ Building

```bash
just build           # Build Ktor backend JAR
just build-server    # Alias for build
```

---

## 🖥️ Running Clients

```bash
just web             # Run web client (hot reload)
just desktop         # Run desktop client
just android         # Run Android client
just ios             # Open iOS project in Xcode
```

---

## 🗄️ Database Migrations

### Basic Migration Commands

```bash
just migrate                  # Apply all pending migrations
just migrate-status           # Check which migrations are applied/pending
just migrate-down             # Rollback last batch of migrations
just migrate-create NAME      # Create a new migration file
```

**Examples:**

```bash
just migrate-create add_user_profiles
just migrate-create fix_messaging_indices
```

---

## 🔍 Schema Validation

### Schema Commands

```bash
just schema-export                     # Export current schema to file
just schema-validate                   # Validate against prod snapshot
just schema-diff SNAPSHOT              # Compare with specific snapshot
```

**Examples:**

```bash
# Export current schema
just schema-export

# Validate current matches prod
just schema-validate

# Compare with specific snapshot
just schema-diff pocketbase/schemas_archive/prod_snapshot_jan_2025.json
```

---

## 🧪 Migration Testing

### Testing Workflow Commands

```bash
just test-migrations           # Test migrations on FRESH local DB (destroys local data!)
just test-migration-status     # Quick check of migration status
just validate-all              # Full validation workflow  
just migrate-prod              # Apply migrations to PRODUCTION (requires confirmation)
```

### Recommended Testing Workflow

**Before deploying to PROD:**

```bash
# 1. Test migrations on fresh local DB
just test-migrations
# ⚠️ This destroys local data and recreates from migrations

# 2. If test passes, run full validation
just validate-all

# 3. Only then, apply to PROD
just migrate-prod
# 🚨 Requires confirmation and PROD URL
```

---

## 📊 Validation Details

### `just test-migrations`

**What it does:**

1. Stops and destroys local PocketBase (including data)
2. Starts fresh PocketBase instance
3. Applies all pending migrations
4. Validates schema matches prod snapshot
5. Reports success or errors

**Use when:**

- Testing new migrations before PROD
- Verifying migration idempotency
- Ensuring fresh deployment works

**⚠️ WARNING:** Destroys local PocketBase data!

### `just validate-all`

**What it does:**

1. Starts PocketBase (if not running)
2. Checks migration status
3. Validates schema against prod snapshot
4. Exports current schema for comparison

**Use when:**

- Quick validation of current state
- Before deploying to PROD
- After applying migrations

**Safe:** Does NOT destroy data

### `just migrate-prod`

**What it does:**

1. Confirms you've tested locally
2. Prompts for PROD PocketBase URL
3. Applies migrations to PROD
4. Reports success or errors

**Use when:**

- Local testing is complete and validated
- Ready to update PROD schema

**🚨 DANGER:** Modifies PROD database!

---

## 🔄 Complete Deployment Workflow

### Scenario 1: New Migration (Local → PROD)

```bash
# 1. Create migration
just migrate-create add_new_feature

# 2. Edit the migration file in pocketbase/migrations/

# 3. Test on fresh local DB
just test-migrations
# Expected: ✅ Schema matches perfectly

# 4. If test passes, apply to PROD
just migrate-prod
# Enter PROD URL when prompted
```

### Scenario 2: Fresh Environment Setup

```bash
# 1. Clone repo
git clone <repo>
cd bside

# 2. Copy environment file
cp .env.example .env
# Edit .env with your settings

# 3. Start services
just up

# 4. Apply migrations
just migrate

# 5. Validate
just schema-validate
# Expected: ✅ Schema matches perfectly
```

### Scenario 3: Verify Current State

```bash
# Quick check
just validate-all

# Expected output:
# 1. PocketBase started
# 2. Migration status: X applied, Y pending
# 3. Schema validation: ✅ matches perfectly
# 4. Schema exported to schemas_archive/
```

---

## 🚨 Troubleshooting

### "Migration failed" errors

```bash
# Check migration status
just migrate-status

# View PocketBase logs
just logs pocketbase

# If needed, rollback
just migrate-down
```

### "Schema doesn't match" errors

```bash
# Export current schema for inspection
just schema-export

# Compare with prod snapshot
just schema-diff pocketbase/schemas_archive/prod_snapshot_jan_2025.json

# If drift, re-run migrations
just test-migrations
```

### "Connection refused" errors

```bash
# Ensure PocketBase is running
just up

# Check status
docker-compose ps

# View logs
just logs pocketbase
```

---

## 💡 Tips

1. **Always test first**: Use `just test-migrations` before `just migrate-prod`
2. **Save exports**: `just schema-export` creates timestamped backups
3. **Check status**: `just migrate-status` before applying migrations
4. **Read logs**: `just logs` shows real-time errors
5. **Validate often**: `just validate-all` ensures consistency

---

## 🔗 Related Documentation

- [`docs/RUNNING.md`](./RUNNING.md) - General running guide
- [`docs/MIGRATION_TESTING_PLAN.md`](./MIGRATION_TESTING_PLAN.md) - Detailed testing workflow
- [`pocketbase/migrations/README.md`](../pocketbase/migrations/README.md) - Migration system docs
- [`docs/SCHEMA_VERIFICATION.md`](./SCHEMA_VERIFICATION.md) - Schema validation details
