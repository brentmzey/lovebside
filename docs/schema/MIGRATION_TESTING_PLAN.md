# Schema Migration Testing & Validation Plan

**CRITICAL**: Test migrations locally BEFORE applying to PROD PocketHost

---

## 🎯 Goal

Ensure migrations can be safely applied to PROD by:

1. Testing on fresh local DB
2. Verifying schema consistency
3. Documenting results
4. Only then applying to PROD

---

## 📋 Pre-Migration Checklist

### Current State (as of 2026-01-03)

- ✅ **PROD Schema**: Captured in `schemas_archive/prod_snapshot_jan_2025.json`
- ✅ **Initial Migration**: Created from prod snapshot (`20260103_000000_initial_schema.ts`)
- ✅ **New Migration**: Rich media indices (`20260103_190000_add_rich_media_indices.ts`)
- ❌ **Migrations Applied to PROD**: NO - PROD still has original schema
- ❌ **Migrations Tested Locally**: NO - needs testing

---

## 🧪 Testing Workflow

### Phase 1: Test on Fresh Local DB

```bash
# 1. Stop and DESTROY local PocketBase (fresh start)
docker-compose down -v
rm -rf pocketbase/pb_data  # CAUTION: Deletes local data!

# 2. Start fresh PocketBase
docker-compose up -d pocketbase
# Wait 10 seconds for startup
sleep 10

# 3. Check migration status (should show 0 applied, 2 pending)
cd pocketbase && npm run migrate:status

# Expected output:
# ✓ 0 migrations applied
# ⏳ 2 migrations pending:
#   - 20260103_000000_initial_schema.ts
#   - 20260103_190000_add_rich_media_indices.ts

# 4. Apply migrations
npm run migrate:up

# Expected output:
# ✓ Applied 20260103_000000_initial_schema.ts
# ✓ Applied 20260103_190000_add_rich_media_indices.ts
# ✓ 2 migrations applied successfully

# 5. Validate schema matches prod snapshot
npm run schema:validate

# Expected output:
# ✅ Schema matches snapshot perfectly!
# OR
# ❌ Differences found: [list of issues]
```

### Phase 2: Verify Collections

```bash
# Export current local schema for comparison
npm run schema:export

# Compare files manually
diff schemas_archive/prod_snapshot_jan_2025.json \
     schemas_archive/local_$(date +%Y%m%d).json
```

### Phase 3: Test Idempotency

```bash
# Re-run migrations (should skip, not re-apply)
npm run migrate:up

# Expected output:
# ℹ️ All migrations already applied
# ✓ 0 new migrations applied

# Validate schema still matches
npm run schema:validate

# Expected output:
# ✅ Schema matches snapshot perfectly!
```

---

## 📊 Expected Results

### Phase 1: Fresh Migration

After running `migrate:up` on fresh DB, local should have:

**Collections (from prod snapshot)**:

- `t_user` (auth collection)
- `s_profiles`
- `m_conversations`
- `m_conversation_participants`
- `m_messages`
- `m_matches`
- `p_answers`
- `p_questions`
- `tenant_properties`
- `pb_migrations` (created by migration system)

**Indices (from second migration)**:

- Auto-indexed fields (PocketBase manages these)
- Recommendations for manual SQL indices logged

### Phase 2: Schema Validation

`schema:validate` should output:

```
✅ Schema matches snapshot perfectly!

Collections verified: 10
Fields verified: [count]
Indices verified: [count]
```

If there are differences:

```
❌ Missing collections:
   - [list]

⚠️ Extra collections:
   - [list]

⚠️ Schema differences:
   m_messages:
      - Missing field: [field_name]
      - Type mismatch: [details]
```

---

## ⚠️ What If Validation Fails?

### Scenario 1: Missing Collections

**Cause**: Initial migration incomplete  
**Fix**: Update `20260103_000000_initial_schema.ts` to include missing collections  
**Action**: Fix migration, destroy DB, re-test

### Scenario 2: Extra Collections

**Cause**: Migration creates collections not in prod  
**Fix**: Remove extra collection creation from migration  
**Action**: Fix migration, destroy DB, re-test

### Scenario 3: Field Differences

**Cause**: Field definitions don't match prod  
**Fix**: Update migration to match exact prod schema  
**Action**: Fix migration, destroy DB, re-test

---

## ✅ Sign-Off Criteria

Before applying to PROD, ALL of these must be TRUE:

- [ ] Fresh local DB created successfully
- [ ] All migrations applied without errors
- [ ] `schema:validate` shows ✅ perfect match
- [ ] Re-running migrations shows "already applied"
- [ ] Schema still validates after re-run
- [ ] All collections from prod snapshot present
- [ ] No extra collections created
- [ ] All fields match exactly

**Only proceed to PROD if ALL boxes checked**

---

## 🚀 Applying to PROD (PocketHost)

**STOP**: Do NOT proceed until local testing complete and validated!

### Pre-Production Checklist

- [ ] Local testing complete and validated
- [ ] Backup PROD database (PocketHost export)
- [ ] Migrations tested on fresh local DB
- [ ] Schema validation passed
- [ ] Rollback plan documented

### Production Migration Steps

```bash
# 1. Set PROD environment variables
export POCKETBASE_URL=https://your-app.pockethost.io
export POCKETBASE_ADMIN_EMAIL=admin@bside.app
export POCKETBASE_ADMIN_PASSWORD='your-prod-password'

# 2. Check migration status on PROD
npm run migrate:status

# Expected: Should show initial schema not applied (PROD has no migration tracking yet)

# 3. Apply migrations to PROD
npm run migrate:up

# Expected: Should apply both migrations successfully

# 4. Validate PROD schema
npm run schema:validate

# Expected: ✅ Schema matches snapshot perfectly!
```

### Post-Migration Verification

```bash
# 1. Check PROD app still works
curl https://your-app.pockethost.io/api/health

# 2. Test user login
# 3. Test messaging
# 4. Test file uploads

# 5. Export PROD schema for records
npm run schema:export
mv schemas_archive/local_*.json schemas_archive/prod_post_migration_$(date +%Y%m%d).json
```

---

## 🔄 Rollback Plan (If Something Goes Wrong)

### If Migration Fails

```bash
# 1. Check error message
# 2. Run: npm run migrate:down
# 3. Fix migration file
# 4. Re-test locally
# 5. Try again
```

### If Validation Fails After Migration

```bash
# 1. Do NOT proceed with app deployment
# 2. Export current PROD schema
# 3. Compare with expected
# 4. Document differences
# 5. Fix migration
# 6. Restore PROD from backup (PocketHost UI)
# 7. Re-test locally
```

---

## 📝 Testing Log Template

```
=== Schema Migration Test ===
Date: 2026-01-03
Tester: [Name]
Environment: Local (Fresh DB)

Phase 1: Fresh Migration
- [ ] DB destroyed and recreated
- [ ] Migrations applied: [count]
- [ ] Errors: [none/list]

Phase 2: Schema Validation
- [ ] Validation result: [✅ / ❌]
- [ ] Differences found: [none/list]

Phase 3: Idempotency Test  
- [ ] Re-run result: [skipped as expected]
- [ ] Schema still valid: [yes/no]

Sign-off:
- [ ] Ready for PROD: [YES / NO]
- [ ] Notes: [any observations]
```

---

## 🔗 Related Documents

- [`pocketbase/migrations/README.md`](../pocketbase/migrations/README.md) - Migration usage
- [`docs/SCHEMA_HARDENING.md`](./SCHEMA_HARDENING.md) - Overall strategy
- [`docs/SCHEMA_VERIFICATION.md`](./SCHEMA_VERIFICATION.md) - Validation tools
