# Project Kanban Board

## 🚨 Blockers

None currently.

## 📋 P0 - Critical Path (Start Here)

### ✅ DONE: Idempotent Migration System

- [x] Created migration runner with checksum tracking
- [x] Saved production schema snapshot
- [x] Built initial migration from prod schema
- [x] Added `just migrate` commands
- [x] Schema validation tool (export, diff, validate)

### ✅ DONE: Nginx Smart Routing

- [x] Updated `nginx/nginx.conf` with FQDN placeholders
- [x] Configured `/api/pb/` → PocketBase routing (CRUD, auth, real-time)
- [x] Configured `/api/v1/` → Ktor routing (business logic)
- [x] Set file upload size limits (50MB) per route
- [x] Added WebSocket support, rate limiting, load balancing
- [ ] Test all client targets with new routing

### ✅ DONE: Rich Media Performance ⭐

- [x] Add `idx_messages_has_attachments` index (documented)
- [x] Add `idx_messages_thread_root` index (documented)  
- [x] Configure CDN for attachments (AWS CloudFront + S3 guide)
- [x] Environment-based configuration (.env.example)
- [x] Docker auto-admin creation

### ✅ DONE: Discovery Geolocation

- [x] Platform-specific LocationService (Android, iOS, JVM, JS)
- [x] GeoUtils distance calculation (Haversine formula)
- [x] DI configuration with expect/actual factories
- [x] All platforms build successfully
- [ ] Integrate with discovery queries (next step)

### 🔄 NEXT: Multi-Platform Testing & Integration

- [ ] Test Android app with real location
- [ ] Test iOS app (currently using stub)
- [ ] Test Desktop app (IP geolocation)
- [ ] Test Web app (browser geolocation)
- [ ] Integrate location filtering into discovery
- [ ] Test rich media upload/playback with CDN

### 🎨 UI-003: Apple HIG Implementation (P0)

- [x] Create ResponsiveModifiers & Theme
- [x] Document key components
- [x] Apply constraints to Messaging UI
- [ ] Apply to LocationScreen
- [ ] Accessibility (Phase 2)

## 📋 P1 - High Priority

- [ ] **CDN/Media Storage Setup**: Configure AWS CloudFront + S3 or Google Drive for media delivery
  - Timing: After local media upload testing completes
  - See: `docs/AWS_CDN_SETUP.md` and `cdn_timing.md`
  - Dependencies: Media upload UI, local testing
- [ ] **Fix Verification Tests**: `MessagingAttachmentVerificationTest` failing
- [ ] **Job Queue**: Background worker for matching algo
- [ ] **Geometric Profiles**: UI implementation

## 🏃 In Progress

- [ ] **Threading**: Finalizing integration tests for threaded replies.
- [ ] **Testing**: `MessagingThreadIntegrationTest` stabilization.
- [ ] **Consistency**: Ensuring test admin user exists in all envs.

## ✅ Done

- [x] Create `MessagingRepository`
- [x] Define Data Models
- [x] Create Chat UI
- [x] Implement Real-time Subscriptions
- [x] Rich Media UI (FileKit + Coil)
- [x] Idempotent Migration System
- [x] Production Schema Snapshot
- [x] Discovery Geolocation (LocationService + GeoUtils)
- [x] Platform-specific location providers (Android, iOS, JVM, JS)

---

## 🤖 For AI Agents

**Read First**: [CONTEXT.md](file:///Users/brentzey/bside/.code-hq/CONTEXT.md) for project orientation

**Before Starting Work**:

1. Check P0 section above for current priorities
2. Review [STORIES.md](file:///Users/brentzey/bside/.code-hq/STORIES.md) for acceptance criteria
3. Update [task.md](file:///Users/brentzey/.gemini/antigravity/brain/e9de2573-c41f-4db6-a3c5-5597c4bd61fe/task.md) as you progress

**After Completing Work**:

1. Move completed items from P0 to ✅ Done
2. Update [CONTEXT.md](file:///Users/brentzey/bside/.code-hq/CONTEXT.md) "Last Updated" and "Recent Session Notes"
3. Note any new blockers or tech debt
