# Phase 2: "B-Side" Polish & Expansion Roadmap

This roadmap focuses on enriching the backend, finalizing core features (Matching, Messaging), and achieving a premium, dynamic UI/UX across all platforms.

## 1. Backend Enrichment & Smart Matching

**Goal**: Create a robust, intelligent matching system that goes beyond simple filtering.

- [ ] **Job Queue System**: Implement a background job runner (e.g., Coroutines-based or external worker) for heavy tasks like match calculation.
- [ ] **Advanced Matching Algorithms**:
  - **Affinity Match**: Deep compatibility based on Proust Questionnaire and interests.
  - **Cutting Edge Match**: "Wildcard" or high-potential matches that challenge preferences slightly.
  - **Relationship-Type Matching**: Weighting algorithms differently for Friendship vs. Relationship vs. Both.
- [ ] **Geometric Profiles**: Implement the data structures and API logic to support "Geometric" profile visualization (presumably a multi-dimensional representation of personality).

## 2. Full-Featured Messaging & Federated Architecture

**Goal**: A scalable, production-ready messaging platform with rich media support, backed by a federated architecture.

- [ ] **Federated Architecture**:
  - **Nginx**: Reverse proxy to route traffic.
  - **Splitting**: `/api/pb/*` -> PocketBase (CRUD, Realtime) vs `/api/v1/*` -> Ktor (Heavy Compute).
  - **Storage**: S3/CDN integration for media.
    - [ ] **Prod Schema**: Ensure `attachments` field is synced to Prod environment.
    - [ ] **Efficiency**: Investigate generic `resource_uri` vs PocketBase file handling for CDN offloading.
- [ ] **Real-time Updates**: Ensure WebSocket/SSE integration for instant message delivery.
- [ ] **Rich Media**:
  - Schema updates for `m_messages` to support `attachments` (file array).
  - UI for uploading and viewing photos/videos/GIFs.
  - Optimization (thumbnails, lazy loading).
- [/] **Threading**: Verify and polish the threaded reply interface.
- [/] **Testing**: `MessagingThreadIntegrationTest` stabilization and broad device verification.

### Expanded Messaging Features (New)
- [X] **Reactions** (Schema Complete): 
  - Backend: `m_reactions` collection ✅ DONE
  - Frontend: UI for adding/removing/viewing reactions ⏳ TODO
  - API: REST endpoints for CRUD ⏳ TODO
- [/] **Typing Indicators**: Real-time typing status (`m_typing_status`).
  - Backend: Schema ✅ COMPLETE
  - API: WebSocket events ⏳ TODO
  - Frontend: UI indicators ⏳ TODO
- [X] **Presence** (Schema Complete): Online/Activity status (`m_presence`).
  - Backend: Schema ✅ DONE (migration 1769810000)
  - API: Real-time updates ⏳ TODO
  - Frontend: Status display ⏳ TODO
- [ ] **Mentions**: `@user` support in messages.
  - Backend: Schema needed ⏳ TODO
  - API: Parse and notify ⏳ TODO
  - Frontend: Autocomplete UI ⏳ TODO
- [ ] **Polls**: Create and vote on polls.
  - Backend: Schema needed ⏳ TODO
  - Frontend: Poll creation/voting UI ⏳ TODO
- [ ] **Rich Link Previews**: OpenGraph parsing for links.
  - Backend: URL metadata extraction ⏳ TODO
  - Frontend: Preview cards ⏳ TODO
- [ ] **Disappearing Messages**: Ephemeral messages.
  - Backend: TTL logic ⏳ TODO
  - Frontend: Timer UI ⏳ TODO
- [ ] **E2E Encryption**: (Planned for Phase 3).
- [/] **Read Receipts**: Track message read status.
  - Backend: `m_read_receipts` ✅ COMPLETE
  - API: Update endpoints ⏳ TODO
  - Frontend: Read indicators ⏳ TODO
- [ ] **Voice Messages**: Record and playback audio.
  - Backend: Audio storage ⏳ TODO
  - Frontend: Recorder/player UI ⏳ TODO

## 3. Premium UI/UX (Apple Guidelines)

**Goal**: "Wow" the user with dynamic, fluid, and beautiful interfaces.

- [ ] **Design System Overhaul**:
  - Unified typography (Inter/Outfit) and color palettes (HSL, avoiding generic colors).
  - Glassmorphism and modern blur effects.
  - Consistency across Web, Desktop, iOS, and Android.
- [ ] **Dynamic Rendering**:
  - Micro-animations for interactions (hovers, clicks, transitions).
  - Skeleton loaders and smooth state transitions (no jarring pops).
- [ ] **Geometric Profile UI**: Visualizing the user's "shape" dynamically.
- [ ] **Platform specifics**:
  - **iOS**: Native feel, proper safe-area handling, swipe gestures.
  - **Android**: Material You integration where appropriate, edge-to-edge drawing.

## 4. Profile & Proust Questionnaire

**Goal**: The core data entry point must be flawless and engaging.

- [ ] **Data Integrity**: Ensure all profile updates (texts, photos, answers) save correctly.
- [ ] **UX Flow**: smooth multi-step questionnaire with progress saving.
- [ ] **Avatar & Media**: High-quality image cropping, uploading, and display.

## 5. Mobile Verification (iOS & Android)

**Goal**: The app must work perfectly on devices, not just simulators.

- [ ] **Android**: Verify build, run, permissions (camera, location).
- [ ] **iOS**: Verify build (Compose Multiplatform), permissions, safe areas.