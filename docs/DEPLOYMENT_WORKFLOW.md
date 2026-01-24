# Schema Migration Deployment Workflow

## Current Status

✅ **Infrastructure Ready**

- Migrations created and tested locally
- Docker auto-creates admin user on startup
- Schema validation tools working
- Port configuration fixed (8091)
- CDN infrastructure documented

⚠️ **Known Issue**: PocketBase API auth endpoint has internal errors (500) - this is a PocketBase bug, not our code

---

## Quick Deployment (Recommended)

### Option 1: UI-Based Deployment (Safest)

**For Local/Staging:**

```bash
# 1. Start fresh environment
docker-compose down -v
docker-compose build pocketbase
docker-compose up -d pocketbase

# 2. Open PocketBase Admin UI
open http://localhost:8091/_/

# 3. Login with auto-created credentials
# Email: tester_admin@bside.love
# Password: password123

# 4. Navigate to Settings → Import collections
# Upload: pocketbase/schemas_archive/prod_snapshot_jan_2025.json

# 5. Verify schema
cd pocketbase && bun run schema:export
# Compare with prod snapshot
```

**For Production (PocketHost):**

```bash
# 1. Login to PocketHost admin panel
open https://your-app.pockethost.io/_/

# 2. Navigate to Settings → Import collections
# Upload: pocketbase/schemas_archive/prod_snapshot_jan_2025.json

# 3. Verify deployment
# All collections should match local
```

### Option 2: Automated Deployment (When Auth Fixed)

```bash
# Local testing
just test-migrations

# If passes, deploy to PROD
just migrate-prod
# Enter PROD URL when prompted
```

---

## Manual Verification Steps

### 1. Check Collections Exist

**Via UI**: <http://localhost:8091/_/>  
**Expected**: 10 collections (t_user, s_profiles, m_conversations, m_messages, etc.)

### 2. Validate Schema Consistency

```bash
# Export current schema
cd pocketbase && bun run schema:export

# Compare with prod
diff pocketbase/schemas_archive/prod_snapshot_jan_2025.json \
     pocketbase/schemas_archive/local_$(date +%Y-%m-%d).json
```

### 3. Test Application Connectivity

```bash
# Health check
curl http://localhost:8091/api/health

# Test collection access (requires auth token)
curl http://localhost:8091/api/collections
```

---

## Rollback Procedure

### If Deployment Fails

**Via UI:**

1. Settings → Delete all collections
2. Import previous snapshot
3. Verify application still works

**Via API** (if auth works):

```bash
cd pocketbase && bun run migrate:down
```

---

## Production Deployment Checklist

### Pre-Deployment

- [ ] Local schema matches prod snapshot exactly
- [ ] All migrations tested on fresh DB
- [ ] Backup current PROD database (PocketHost export)
- [ ] Team notified of maintenance window
- [ ] Rollback plan documented

### Deployment

- [ ] Apply migrations to PROD (UI or API)
- [ ] Verify all collections created
- [ ] Validate schema consistency
- [ ] Test application endpoints
- [ ] Check real-time subscriptions
- [ ] Test file uploads

### Post-Deployment

- [ ] Export new PROD schema for records
- [ ] Update documentation
- [ ] Monitor for errors (24 hours)
- [ ] Confirm with team

---

## Troubleshooting

### "Auth failed" errors

**Cause**: PocketBase API bug (internal 500 error)  
**Fix**: Use UI-based deployment instead

### "Schema doesn't match" errors

**Cause**: Drift between environments  
**Fix**:

```bash
cd pocketbase && bun run schema:validate
# Review differences
# Re-export prod snapshot if needed
```

### "Migration already applied" errors

**Cause**: Migration ran successfully  
**Fix**: Check `pb_migrations` collection in UI - should show applied migrations

---

## Environment-Specific Notes

### Development (localhost:8091)

- Admin auto-created on startup
- Safe to destroy and rebuild
- Use `just test-migrations` for testing

### Staging (if applicable)

- Should mirror production exactly
- Test all deployments here first
- Validate client integrations

### Production (PocketHost)

- **DO NOT auto-destroy**
- Always backup before deploy
- Use maintenance window
- Monitor closely post-deploy

---

## Next Steps

Once schema is deployed and validated:

1. **✅ Test Rich Media**
   - Upload images, videos, GIFs
   - Verify attachments field populated
   - Test queries with `has_attachments`

2. **✅ Configure CDN**
   - Follow `docs/AWS_CDN_SETUP.md`
   - Set environment variables
   - Test media serving via CDN

3. **✅ Performance Validation**
   - Run queries on large dataset
   - Verify indices improve query time
   - Monitor CDN cache hit rate

---

## Related Documentation

- [`docs/MIGRATION_TESTING_PLAN.md`](./MIGRATION_TESTING_PLAN.md) - Detailed testing
- [`docs/AWS_CDN_SETUP.md`](./AWS_CDN_SETUP.md) - CDN configuration
- [`docs/JUSTFILE_REFERENCE.md`](./JUSTFILE_REFERENCE.md) - Command reference
- [`pocketbase/migrations/README.md`](../pocketbase/migrations/README.md) - Migration system
