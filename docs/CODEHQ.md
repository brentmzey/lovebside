# B-Side Project Overview

**Type**: Kotlin Multiplatform Dating/Connection App  
**Status**: Active Development (Beta/Refinement)  
**Last Updated**: December 26, 2025

## Quick Links

- [Project Roadmap](./docs/PROJECT_ROADMAP.md) - Comprehensive development roadmap
- [Development Workflow](./docs/DEVELOPMENT_WORKFLOW.md) - Build, run, and verify instructions
- [Build Status](./docs/BUILD_STATUS.md) - Platform compilation status
- [Design System](./docs/DESIGN_SYSTEM.md) - UI/UX guidelines
- [PocketBase Schema](./docs/POCKETBASE_SCHEMA.md) - Database structure
- [Scripts Guide](./scripts/README.md) - Development scripts

## Current Focus

### Phase 3: Messaging & Offline Support (Active)
- **Real-time Messaging**: Implemented with PocketBase SSE, threading, read receipts.
- **Offline Cache & Sync**: (In Progress)
    - [x] OfflineCacheManager with LRU cache and TTL support
    - [x] Platform-specific NetworkMonitor (Android, iOS, JVM, JS)
    - [x] Integrated into MessagingRepository with auto-sync queue
    - [x] Optimistic UI updates for offline message sends
    - [ ] Fix iOS compilation issues
    - [ ] Multi-platform demo with screen recordings

### Phase 3: Premium UX & Intelligence (Completed)
- **UI/UX Overhaul**: Implemented "Apple-Style" aesthetic.
    - [x] **Landing Screen**: Orbit Animation with real profile data hooks.
    - [x] **Auth Flow**: Seamless transitions, biometric integration, glassmorphism.
    - [x] **Dashboard**: Match Carousel, Hero sections.
- **Backend Intelligence**:
    - [x] **Matching Engine**: Jaccard + Proust + Location algorithm implemented in `cron_matching.ts`.
    - [ ] **Discovery**: Geolocation support (Lat/Lng added to Schema, need client implementation).

### Immediate Priorities
1.  **Offline Demo**: Build multi-platform apps and demo offline→online message sync.
2.  **Geolocation**: Implement client-side location fetching for "People Around You".
3.  **Screen Recordings**: Capture real-time messaging across Android, iOS, Web, Desktop.

## Project Stats

| Metric | Value |
|--------|-------|
| Platforms | 6 (Android, iOS, Desktop, Web JS, Wasm, Server) |
| Build Status | ⚠️  iOS compilation issues, Android/JVM/Web OK |
| DB Schema | Standardized (`pb_schema_standardized.json`) |
| SDK | KMP Messaging + Auth + Profiles + Offline Cache |
| Cache | Distributed (t_user_property) with auto-sync |

## Code-HQ Usage

```bash
# View tasks
code-hq tasks

# Start working on a task
code-hq start task:123 --description "Working on navigation"

# Log time
code-hq stop task:123

# Generate timesheet
code-hq timesheet --from 7d
```

See [.github/prompts/codehq.md.prompt.md](./.github/prompts/codehq.md.prompt.md) for full command reference.
