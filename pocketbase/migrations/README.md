# Database Migrations

## Overview

This directory contains the idempotent migration system for B-Side's PocketBase database.

## Quick Start

```bash
# From pocketbase directory
cd pocketbase

# Check migration status
npm run migrate:status

# Apply all pending migrations
npm run migrate:up

# Rollback last batch
npm run migrate:down

# Create a new migration
npm run migrate:create add_user_profiles
```

## How It Works

### Migration Tracking

- Migrations are tracked in the `pb_migrations` collection
- Each migration has a **checksum** to detect tampering
- Migrations run in **batches** for easy rollback
- System is **idempotent** - safe to run multiple times

### Migration Files

Migrations live in `pocketbase/migrations/` with timestamp prefixes:

```
20260103_120000_initial_schema.ts
20260103_121500_add_messaging.ts
20260103_123000_add_rich_media_indices.ts
```

### Migration Structure

```typescript
/**
 * Migration: Add User Profiles
 */

export async function up(pbUrl: string, token: string) {
  // Create/modify collections, add fields, create indices
  const response = await fetch(`${pbUrl}/api/collections`, {
    method: 'POST',
    headers: {
      'Authorization': token,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      name: 's_profiles',
      type: 'base',
      fields: [/* ... */]
    })
  });
}

export async function down(pbUrl: string, token: string) {
  // Reverse the migration (optional but recommended)
  await fetch(`${pbUrl}/api/collections/s_profiles`, {
    method: 'DELETE',
    headers: { 'Authorization': token }
  });
}
```

## Environment Variables

Set these before running migrations:

```bash
export POCKETBASE_URL=http://127.0.0.1:8090
export POCKETBASE_ADMIN_EMAIL=admin@bside.love
export POCKETBASE_ADMIN_PASSWORD=your_password
```

Or use `.env` file in the `pocketbase/` directory.

## Best Practices

### 1. **Always Test Locally First**

```bash
# Test on local dev DB
npm run migrate:up

# Verify with status check
npm run migrate:status
```

### 2. **Use Descriptive Names**

```bash
npm run migrate:create add_message_attachments_field
npm run migrate:create create_read_receipts_index
```

### 3. **Keep Migrations Small**

- One logical change per migration
- Easier to debug and rollback

### 4. **Never Edit Applied Migrations**

- Changes break checksum validation
- Create a new migration to fix issues

### 5. **Always Implement `down()`**

- Makes rollbacks safe and predictable
- Documents your intent

## Production Deployment

### PocketHost or Hosted Instance

1. **Backup First!**

   ```bash
   # Export current schema
   curl ${POCKETBASE_URL}/api/collections \
     -H "Authorization: ${TOKEN}" > backup_$(date +%Y%m%d).json
   ```

2. **Set Environment**

   ```bash
   export POCKETBASE_URL=https://your-app.pockethost.io
   export POCKETBASE_ADMIN_EMAIL=admin@yourdomain.com
   export POCKETBASE_ADMIN_PASSWORD=secure_password
   ```

3. **Run Migrations**

   ```bash
   npm run migrate:up
   ```

4. **Verify**

   ```bash
   npm run migrate:status
   ```

## Troubleshooting

### "Authentication failed"

- Check `POCKETBASE_URL` is correct and accessible
- Verify admin credentials
- Ensure PocketBase is running

### "Checksum mismatch"

- Migration file was modified after being applied
- **Solution**: Revert the file or create a new migration

### "Migration already applied"

- Safe to ignore - system is idempotent
- Run `npm run migrate:status` to confirm

### "Collection already exists"

- Make migrations idempotent by checking existence first:

  ```typescript
  export async function up(pbUrl: string, token: string) {
    const check = await fetch(`${pbUrl}/api/collections/my_collection`, {
      headers: { 'Authorization': token }
    });
    
    if (check.status === 404) {
      // Create collection
    } else {
      console.log('Collection already exists, skipping');
    }
  }
  ```

## Integration with `just`

Add to `/Justfile`:

```just
# Run database migrations
migrate:
    cd pocketbase && npm run migrate:up

# Check migration status
migrate-status:
    cd pocketbase && npm run migrate:status
```

## Schema Sync from Production

To sync production schema to local:

1. **Export production schema**

   ```bash
   curl ${PROD_URL}/api/collections \
     -H "Authorization: ${TOKEN}" \
     > schemas_archive/prod_snapshot_$(date +%Y%m%d).json
   ```

2. **Create migration from snapshot**

   ```bash
   npm run migrate:create import_prod_schema_jan_2025
   ```

3. **Edit migration to apply snapshot**
   - Parse JSON
   - Apply collections one by one

## Related Documentation

- [SCHEMA_HARDENING.md](../docs/SCHEMA_HARDENING.md) - Overall strategy
- [STORIES.md](../.code-hq/STORIES.md) - User stories and tasks
