# Epic: Database Schema Hardening & 12-Factor Compliance

## Story 1: Idempotent Migration System

**Priority**: P0  
**Est**: 3 days  
**Dependencies**: None

### User Story

As a **DevOps engineer**, I want **idempotent database migrations** so that I can **safely deploy schema changes to any environment without manual intervention**.

### Acceptance Criteria

- [ ] Migration tracking table (`pb_migrations`) exists
- [ ] Migrations can run multiple times without errors
- [ ] Schema validation detects and reports drift
- [ ] Works on empty DB and existing production DB

### Technical Tasks

1. Create `pocketbase/migrations-manager/migrate.ts`
2. Implement checksum-based migration tracking
3. Port prod schema to `001_initial_schema.js`
4. Add CLI commands: `migrate up`, `migrate down`, `migrate status`
5. Test on fresh PocketBase instance
6. Test on production snapshot

---

## Story 2: Environment-Based Configuration

**Priority**: P0  
**Est**: 2 days  
**Dependencies**: Story 1

### User Story

As a **developer**, I want **environment-specific configuration** so that **the same codebase works in dev, staging, and production**.

### Acceptance Criteria

- [ ] CDN URLs configured via `CDN_BASE_URL` env var
- [ ] File size limits configured via `MAX_FILE_SIZE`
- [ ] PocketBase URL configured via `POCKETBASE_URL`
- [ ] No hardcoded URLs in application code

### Technical Tasks

1. Create `t_tenant_property` records for env config
2. Update `MessagingRepository` to use env-based URLs
3. Add file URL builder utility
4. Update all file references to use builder
5. Document required env vars in `docs/RUNNING.md`

---

## Story 3: Nginx Intelligent Routing

**Priority**: P0  
**Est**: 2 days  
**Dependencies**: None

### User Story

As a **backend developer**, I want **smart reverse proxy routing** so that **simple requests go directly to PocketBase and complex logic routes through Ktor**.

### Acceptance Criteria

- [ ] `/api/pb/*` routes to PocketBase
- [ ] `/api/v1/*` routes to Ktor
- [ ] File uploads support up to 50MB
- [ ] FQDN configured for production
- [ ] All client targets work with new routing

### Technical Tasks

1. Update `nginx/nginx.conf` with new upstream definitions
2. Add location blocks for `/api/pb/` and `/api/v1/`
3. Configure `client_max_body_size` for file uploads
4. Update `docker-compose.yml` with Nginx environment vars
5. Test routing with Postman/curl
6. Update client code to use new endpoints

---

## Story 4: Rich Media Performance Optimization

**Priority**: P0  
**Est**: 3 days  
**Dependencies**: Story 1, Story 2

### User Story

As a **mobile user**, I want **fast media loading** so that **I can view photos and videos without lag**.

### Acceptance Criteria

- [ ] Queries for messages with attachments use optimized indices
- [ ] Thumbnails generated for images (200x200, 800x800)
- [ ] Video thumbnails extracted on upload
- [ ] CDN serves media files in production
- [ ] GIF, photo, video upload/playback tested on all platforms

### Technical Tasks

1. Add `idx_messages_has_attachments` index via migration
2. Add `idx_messages_thread_root` index for threaded media
3. Configure PocketBase thumbnail generation
4. Implement video thumbnail hook (FFmpeg)
5. Test media queries with 1000+ messages
6. Benchmark load times with CDN vs direct

---

## Story 5: Build Verification & Integration

**Priority**: P0  
**Est**: 1 day  
**Dependencies**: All above

### User Story

As a **QA engineer**, I want **all platforms to build successfully** so that **schema changes don't break the app**.

### Acceptance Criteria

- [ ] `./gradlew :composeApp:assemble` succeeds
- [ ] Android emulator runs successfully
- [ ] iOS simulator runs successfully  
- [ ] Desktop app launches
- [ ] Web app loads in browser
- [ ] All integration tests pass

### Technical Tasks

1. Run `just up` to start backend
2. Run `./gradlew :composeApp:assemble`
3. Fix any compilation errors
4. Run `just android` and verify app launches
5. Run `just ios` and verify app launches
6. Run `just desktop` and verify app launches
7. Run `just web` and verify app loads
8. Run `./gradlew test` and verify all tests pass

---

## Dependencies Graph

```
Story 1 (Migration System)
  ├─→ Story 2 (Env Config)
  └─→ Story 4 (Performance)
  
Story 3 (Nginx Routing) → Independent

Story 5 (Build Verification) → Depends on ALL
```

## Success Metrics

- **Migration Time**: < 30 seconds for full schema setup
- **Query Performance**: < 100ms for message list with media
- **Build Time**: < 10 minutes for all targets
- **Zero Downtime**: Migrations run without app restart
