# B-Side Project Context & Status

**Last Updated**: 2026-01-04 02:44 CST  
**Current Phase**: Discovery Geolocation Complete → Testing & Integration  
**Active Agents**: Gemini 3 (High)

---

## 🎯 Current Focus (P0)

Discovery Geolocation implementation complete! Platform-specific LocationService implementations working on all targets. Next priorities: integrate location filtering into discovery queries and test all platforms end-to-end.

### Active Work Streams

1. **✅ Schema Migration System** - COMPLETE
   - Idempotent migration runner built
   - Production schema captured and validated
   - Schema comparison tool working

2. **✅ Nginx Smart Routing** - COMPLETE
   - Path-based routing configured
   - WebSocket support for real-time
   - File upload limits and rate limiting
   - Load balancing with health checks

3. **✅ Discovery Geolocation** - COMPLETE (Core Implementation)
   - Platform-specific LocationService (Android, iOS, JVM, JS)
   - GeoUtils distance calculation (Haversine formula)
   - All platforms build successfully
   - Next: Integrate with discovery queries

4. **🔄 Multi-Platform Testing** - NEXT
   - Test location services on all platforms
   - Integrate location filtering into discovery
   - Test rich media upload/playback with CDN

5. **📦 CDN/Media Storage** - TRACKED (P1)
   - **When**: After local media upload testing
   - **Options**: AWS CloudFront + S3 (production) or Google Drive (interim)
   - **Status**: Documented in `cdn_timing.md`
   - **Dependencies**: Media upload UI, local testing

---

## 📊 Project Status Dashboard

### Build Health

- ✅ Gradle build: PASSING
- ✅ Migration system: WORKING
- ✅ Nginx config: READY
- ⏳ Full platform verification: PENDING

### Tech Stack

- **Frontend**: Kotlin Multiplatform (Compose), Web (JS), iOS, Android
- **Backend**: Ktor (JVM), PocketBase (SQLite/auth)
- **Infra**: Docker Compose, Nginx reverse proxy
- **Tools**: `just` (task runner), npm (migrations), Gradle

### Recent Completions

- Rich Media UI (FileKit + Coil for image/video uploads)
- Justfile with streamlined run commands
- Migration system with checksum tracking and validation
- Production schema snapshot saved
- Nginx smart routing with WebSocket, rate limiting, load balancing
- **Discovery Geolocation** (LocationService + GeoUtils for all platforms)

---

## 🗺️ Navigation Guide

### For Quick Orientation

- **What to run**: [`docs/RUNNING.md`](file:///Users/brentzey/bside/docs/RUNNING.md)
- **Current tasks**: [`.code-hq/KANBAN.md`](file:///Users/brentzey/bside/.code-hq/KANBAN.md)
- **Detailed stories**: [`.code-hq/STORIES.md`](file:///Users/brentzey/bside/.code-hq/STORIES.md)
- **Architecture strategy**: [`docs/SCHEMA_HARDENING.md`](file:///Users/brentzey/bside/docs/SCHEMA_HARDENING.md)

### For Deep Dives

- **Messaging architecture**: [`implementation_plan.md`](file:///Users/brentzey/.gemini/antigravity/brain/e9de2573-c41f-4db6-a3c5-5597c4bd61fe/implementation_plan.md)
- **Migration guide**: [`pocketbase/migrations/README.md`](file:///Users/brentzey/bside/pocketbase/migrations/README.md)
- **Session notes**: [`docs/TEMP_README.md`](file:///Users/brentzey/bside/docs/TEMP_README.md)

---

## 🤖 Agent Coordination

### When Starting a Session

1. Read this file (`CONTEXT.md`) for current state
2. Check [`KANBAN.md`](file:///Users/brentzey/bside/.code-hq/KANBAN.md) for P0 tasks
3. Review [`STORIES.md`](file:///Users/brentzey/bside/.code-hq/STORIES.md) for acceptance criteria
4. Update [`task.md`](file:///Users/brentzey/.gemini/antigravity/brain/e9de2573-c41f-4db6-a3c5-5597c4bd61fe/task.md) as you complete items

### When Handing Off

1. Update this file with **Current Focus** section
2. Update **Last Updated** timestamp
3. Mark completed items in [`KANBAN.md`](file:///Users/brentzey/bside/.code-hq/KANBAN.md)
4. Document any blockers or questions

### Key Principles

- **Idempotency**: All scripts/migrations should be safe to re-run
- **12-Factor**: Config via env vars, not hardcoded
- **Documentation**: Update docs as you code
- **Build Verification**: Run `./gradlew :composeApp:assemble` before major commits

---

## 🚀 Quick Commands

```bash
# Backend
just up              # Start PocketBase + Ktor in Docker
just migrate         # Apply database migrations
just migrate-status  # Check migration state

# Frontend clients
just web            # Web app (hot reload)
just desktop        # Desktop app
just android        # Android emulator
just ios            # iOS simulator (Xcode)

# Verification
./gradlew :composeApp:assemble   # Build all targets
```

---

---

## 🔗 External Resources

- **PocketBase Docs**: <https://pocketbase.io/docs>
- **Compose Multiplatform**: <https://www.jetbrains.com/compose-multiplatform/>
- **Just Command Runner**: <https://github.com/casey/just>

---

## ⚠️ Known Issues / Tech Debt

1. `MessagingAttachmentVerificationTest` failing (User B lookup)
2. Hardcoded base URL for file attachments (needs env var)
3. No CDN integration yet for media files
4. Video thumbnail generation not implemented
5. Nginx routing needs testing with all client targets

See [KANBAN.md](file:///Users/brentzey/bside/.code-hq/KANBAN.md) for prioritized backlog.
