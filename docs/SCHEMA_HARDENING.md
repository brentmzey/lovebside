# Schema Hardening & Migration Strategy

## Overview

This document outlines the strategy for achieving **idempotent, 12-factor compliant database schema management** for B-Side's PocketBase database.

## Goals

1. **Idempotency**: Migrations can be run multiple times safely
2. **Reproducibility**: Schema can be built from scratch or repaired on existing DBs
3. **Environment Agnostic**: Works on local, PocketHost, or self-hosted instances
4. **12-Factor Compliance**: Configuration via environment, not hardcoded values

## Current State

- **Production Schema**: Captured in `pocketbase/schemas_archive/prod_snapshot_jan_2025.json`
- **Local Setup**: `scripts/setup_dev_env.sh` (bash-based, not idempotent)
- **Gap**: No formal migration system, schema drift between environments

## Proposed Architecture

### Migration System

```
pocketbase/
├── pb_migrations/           # Generated JS migrations (PocketBase native)
├── migrations/              # Source migration scripts (versioned)
│   ├── 001_initial_schema.sql
│   ├── 002_add_messaging.sql
│   └── 003_add_rich_media.sql
└── migrations-manager/      # Migration orchestration tool
    └── migrate.ts           # Idempotent migration runner
```

### Key Principles

1. **Versioned Migrations**: Each migration has a timestamp/version
2. **Checksum Validation**: Detect schema drift
3. **Rollback Support**: Safe downgrades
4. **Index Management**: Create indices without blocking writes

## Migration Tasks

### Phase 1: Foundation (P0)

- [ ] Create migration tracking table (`pb_migrations`)
- [ ] Port production schema to base migration (`001_initial_schema`)
- [ ] Implement idempotent migration runner
- [ ] Add schema validation/repair tool

### Phase 2: Rich Media (P0)

- [ ] Verify `m_messages.attachments` field configuration
- [ ] Add indices for media queries
- [ ] Configure CDN URL patterns via env vars
- [ ] Implement file size limits per environment

### Phase 3: Performance (P1)

- [ ] Audit existing indices (remove unused, add missing)
- [ ] Add composite indices for common queries
- [ ] Implement query performance monitoring

## Rich Media Schema Requirements

### Fields (Already in Production)

```javascript
{
  name: "attachments",
  type: "file",
  maxSelect: 20,
  maxSize: 10485760,  // 10MB
  mimeTypes: ["image/png", "image/jpeg", "image/webp", "video/mp4"],
  thumbs: ["200x200", "800x800"]
}
```

### Recommended Indices

```sql
-- Fast lookup of messages with attachments
CREATE INDEX idx_messages_has_attachments 
ON m_messages(conversation_id, sent_at) 
WHERE length(attachments) > 0;

-- Thread root lookups
CREATE INDEX idx_messages_thread_root 
ON m_messages(thread_root_id, sent_at) 
WHERE thread_root_id IS NOT NULL;
```

## Environment Configuration

### Required Env Vars

```bash
# Database
POCKETBASE_URL=http://localhost:8090
POCKETBASE_ADMIN_EMAIL=admin@bside.love
POCKETBASE_ADMIN_PASSWORD=<secret>

# Storage
CDN_BASE_URL=https://cdn.bside.app  # Production
MAX_FILE_SIZE=10485760              # 10MB
THUMBNAILS_ENABLED=true

# API Routing
NGINX_UPSTREAM_KTOR=http://server:8080
NGINX_UPSTREAM_PB=http://pocketbase:8090
```

## Nginx Routing Strategy

### Current Issues

- Hardcoded ports in client code
- No intelligent routing based on request type
- Missing FQDN configuration

### Proposed Routes

```nginx
# Direct to PocketBase (CRUD, Real-time)
location /api/pb/ {
    proxy_pass http://pocketbase:8090/;
}

# Route to Ktor (Business Logic, Jobs)
location /api/v1/ {
    proxy_pass http://server:8080/;
}

# File uploads (optimize for large transfers)
location /api/pb/files/ {
    client_max_body_size 50M;
    proxy_pass http://pocketbase:8090/api/files/;
}
```

## Next Steps

1. **Immediate**: Create `docs/SCHEMA_MIGRATIONS_PLAN.md` with detailed migration scripts
2. **Week 1**: Implement migration runner, port production schema
3. **Week 2**: Add rich media indices, configure CDN
4. **Week 3**: Update Nginx config, test routing
5. **Week 4**: Full integration test across all platforms

## Related Documents

- [RUNNING.md](./RUNNING.md) - How to run the stack
- [TEMP_README.md](./TEMP_README.md) - Session restore notes
