# B-Side Epic Roadmap

**Last Updated**: January 30, 2026  
**Total Tasks**: 102  
**Total Estimated Hours**: 580h (~14 weeks @ 40h/week)

## 📊 Overview

This roadmap outlines all major epics for building B-Side, a modern dating app with:
- Beautiful Kotlin Multiplatform UI (Android, iOS, Web, Desktop)
- Scalable PocketBase backend with smart queue processing
- Real-time messaging with SSE
- Advanced matching algorithms (statistical, soft skills, geographic)
- Comprehensive testing and monitoring

---

## 🎯 Epic 1: Foundation & Infrastructure (M1-M2)

**Goal**: Establish core navigation, auth, and scalable backend infrastructure  
**Duration**: 8 weeks  
**Est. Hours**: 156h

### Key Deliverables
- ✅ Navigation with Decompose
- ✅ Email/Password + Google OAuth authentication
- ✅ Onboarding wizard with profile creation
- 🆕 Redis queue system for high-traffic write operations
- 🆕 Read/Write separation with SQLite WAL mode
- 🆕 Nginx load balancing with health checks
- 🆕 Event-driven architecture with Redis Streams

### Critical Tasks
- `task:nav-001` - RootComponent Navigation (8h)
- `task:auth-001` - Login Integration (4h)
- `task:auth-002` - Signup Flow (8h)
- `task:auth-007` - Google OAuth Backend (8h)
- `task:auth-008` - Google OAuth UI (10h)
- `task:infra-005` - Smart Queue System (16h) 🔥
- `task:infra-006` - Read/Write Separation (12h)
- `task:infra-007` - Nginx Load Balancing (10h)
- `task:infra-008` - Event Streams (14h)
- `task:infra-009` - Race Condition Prevention (8h)

### Infrastructure Architecture
```
┌─────────────┐
│   Nginx     │ ← Load Balancer + Sticky Sessions
│ (port 80)   │
└──────┬──────┘
       │
   ┌───┴───┬───────┬───────┐
   ▼       ▼       ▼       ▼
┌──────┐┌──────┐┌──────┐┌──────┐
│ PB 1 ││ PB 2 ││ PB 3 ││ PB 4 │ ← PocketBase Instances
└───┬──┘└───┬──┘└───┬──┘└───┬──┘
    │       │       │       │
    └───────┴───┬───┴───────┘
                ▼
         ┌─────────────┐
         │ Redis Queue │ ← BullMQ Job Queue
         └──────┬──────┘
                │
         ┌──────▼──────┐
         │   Workers   │ ← Match Calc, Notifications
         └─────────────┘
```

---

## 🎨 Epic 2: Core User Experience (M2)

**Goal**: Beautiful, consistent UI/UX across all platforms  
**Duration**: 6 weeks  
**Est. Hours**: 122h

### Key Deliverables
- 🆕 Design system with tokens (colors, spacing, typography)
- 🆕 Shared composable component library
- 🆕 Profile system with photo management
- 🆕 Proust questionnaire with auto-save
- 🆕 Relationship goals & preferences UI
- ✅ Smooth animations and transitions

### High Priority Tasks
- `task:ui-003` - Design System Tokens (8h)
- `task:ui-004` - Component Library (16h)
- `task:ui-005` - Animations Library (12h)
- `task:proust-001` - Questionnaire UI (12h)
- `task:proust-007` - Beautiful Proust UX (12h)
- `task:proust-008` - Auto-Save & Drafts (6h)
- `task:profile-007` - Photo Upload (12h)
- `task:profile-008` - Profile Prompts (10h)
- `task:profile-009` - Relationship Goals UI (8h)
- `task:onboard-001` - Profile Creation Wizard (12h)

### Design System Tokens
```kotlin
object BSideTokens {
  // Colors
  val Primary = Color(0xFF6366F1)      // Indigo
  val Secondary = Color(0xFFF59E0B)    // Amber
  val Surface = Color(0xFF1F2937)      // Dark Gray
  val OnSurface = Color(0xFFF9FAFB)    // Light Gray
  
  // Spacing
  val SpaceXXS = 4.dp
  val SpaceXS = 8.dp
  val SpaceS = 12.dp
  val SpaceM = 16.dp
  val SpaceL = 24.dp
  val SpaceXL = 32.dp
  
  // Radius
  val RadiusS = 8.dp
  val RadiusM = 12.dp
  val RadiusL = 16.dp
  val RadiusFull = 9999.dp
}
```

---

## 💬 Epic 3: Real-Time Messaging Excellence (M3)

**Goal**: Feature-rich, real-time messaging with offline support  
**Duration**: 6 weeks  
**Est. Hours**: 102h

### Key Deliverables
- ✅ Conversations list with realtime updates
- ✅ Chat screen with message threading
- ✅ Offline cache & sync queue
- 🆕 Rich message types (images, voice, emoji reactions)
- 🆕 Typing indicators with SSE
- 🆕 Read receipts with delivery status
- 🆕 Message search & filtering
- 🆕 Push notifications (FCM/APNs)

### High Priority Tasks
- `task:msg-001` - Conversations List (6h)
- `task:msg-002` - Chat Screen (12h)
- `task:msg-003` - Push Notifications (8h)
- `task:msg-004` - Offline Cache (12h)
- `task:msg-007` - Rich Message Types (16h)
- `task:msg-008` - Delivery Status UI (6h)
- `task:msg-009` - Typing Indicators (4h)
- `task:msg-010` - Message Search (8h)

### Messaging Features Checklist
- [x] Text messages
- [x] Real-time updates via SSE
- [x] Offline queue
- [ ] Image uploads with compression
- [ ] Voice messages
- [ ] Emoji reactions
- [ ] GIF support (Giphy)
- [ ] Read receipts
- [ ] Typing indicators
- [ ] Message search
- [ ] Thread replies
- [ ] Link previews

---

## 🧠 Epic 4: Advanced Matching & Discovery (M4)

**Goal**: Multi-dimensional matching algorithms for quality connections  
**Duration**: 8 weeks  
**Est. Hours**: 114h

### Key Deliverables
- ✅ Basic Jaccard + Proust + Location matching
- 🆕 Statistical affinity scoring (Big 5, values)
- 🆕 Soft skills & vibes matching with NLP
- 🆕 Geographic distance weighting
- 🆕 Batch job orchestration with incremental updates
- 🆕 Discovery screen with swipe gestures
- 🆕 Compatibility score visualization

### High Priority Tasks
- `task:match-001` - Discover Screen (8h)
- `task:match-002` - Profile Card (8h)
- `task:match-003` - Compatibility Display (4h)
- `task:match-004` - Swipe Gestures (8h)
- `task:match-005` - Algorithm Integration (12h)
- `task:match-008` - Affinity Score (16h)
- `task:match-009` - Vibes Matching (20h)
- `task:match-010` - Geographic Matching (10h)
- `task:match-011` - Batch Job Orchestration (8h)
- `task:backend-002` - Geolocation (6h)

### Matching Algorithm Weights
```typescript
const matchScore = {
  jaccard: 0.30,        // Shared interests/values
  proust: 0.25,         // Personality compatibility
  location: 0.15,       // Geographic proximity
  affinity: 0.20,       // Statistical compatibility
  vibes: 0.10          // Soft skills & communication style
};

// Final score: 0-100
const finalScore = Math.round(
  (jaccard * 30) + (proust * 25) + (location * 15) +
  (affinity * 20) + (vibes * 10)
);
```

---

## 🎨 Epic 5: UI/UX Polish & Performance (M4)

**Goal**: Consistent, beautiful, performant UI across all platforms  
**Duration**: 4 weeks  
**Est. Hours**: 52h

### Key Deliverables
- 🆕 Platform-specific adaptations (iOS/Android)
- 🆕 Responsive layouts (tablets, foldables)
- 🆕 Performance optimization (60fps target)
- 🆕 Dark mode support
- 🆕 Accessibility compliance (WCAG 2.1 AA)
- 🆕 Haptic feedback & system gestures

### High Priority Tasks
- `task:ui-006` - Platform Adaptations (10h)
- `task:ui-007` - Performance Optimization (12h)
- `task:polish-001` - Animations & Transitions (8h)
- `task:polish-002` - Loading States (6h)
- `task:polish-003` - Error Handling UX (4h)
- `task:a11y-001` - Accessibility Audit (6h)
- `task:a11y-002` - Screen Reader Support (8h)
- `task:a11y-003` - Keyboard Navigation (6h)

---

## 🧪 Epic 6: Comprehensive Testing (M3-M5)

**Goal**: 80%+ code coverage, E2E automation, performance validation  
**Duration**: 6 weeks  
**Est. Hours**: 74h

### Key Deliverables
- 🆕 Backend integration tests (testcontainers)
- 🆕 KMP SDK unit tests (80%+ coverage)
- 🆕 UI screenshot regression tests (Paparazzi)
- 🆕 E2E multi-platform tests (Maestro)
- 🆕 Load & performance testing (k6)
- ✅ Existing messaging tests (55 tests)

### High Priority Tasks
- `task:test-001` - Backend API Tests (16h)
- `task:test-002` - KMP Unit Tests (20h)
- `task:test-003` - Screenshot Tests (12h)
- `task:test-004` - E2E Tests (16h)
- `task:test-005` - Load Testing (10h)

### Testing Strategy
```
┌─────────────────────────────────────┐
│         Test Pyramid                │
├─────────────────────────────────────┤
│  E2E (10%)     │ Critical flows     │
│  Integration   │ API + DB           │
│  (20%)         │ interactions       │
│  Unit (70%)    │ Business logic,    │
│                │ ViewModels, repos  │
└─────────────────────────────────────┘

Target Coverage:
- Backend: 85%
- SDK: 80%
- UI: 60% (screenshot tests)
```

---

## 📊 Epic 7: Observability & DevOps (M5)

**Goal**: Production-ready monitoring, logging, and CI/CD  
**Duration**: 3 weeks  
**Est. Hours**: 28h

### Key Deliverables
- 🆕 APM with Sentry/DataDog
- 🆕 Structured logging with correlation IDs
- 🆕 Distributed tracing (OpenTelemetry)
- 🆕 Multi-platform CI/CD pipeline
- 🆕 Blue-green deployments
- 🆕 Automated rollbacks

### High Priority Tasks
- `task:devops-001` - APM Integration (8h)
- `task:devops-002` - Logging & Tracing (8h)
- `task:devops-003` - CI/CD Pipeline (12h)

### Monitoring Dashboard
```
Key Metrics:
- API Latency (p50, p95, p99)
- Error Rate (< 0.5%)
- Queue Lag (< 100 jobs)
- DB Connections (< 80% capacity)
- Memory Usage (< 80%)
- Match Job Duration (< 5min)

Alerts:
- Error spike (> 1% in 5min)
- API latency > 500ms
- Queue > 1000 jobs
- DB write conflicts > 10/min
```

---

## 📋 Epic 8: Project Management Automation (M4-M5)

**Goal**: Sync .code-hq with JIRA/Notion, automated reporting  
**Duration**: 2 weeks  
**Est. Hours**: 14h

### Key Deliverables
- 🆕 Automated JIRA CSV export
- 🆕 Notion database sync
- 🆕 Progress dashboard with charts
- 🆕 Daily GitHub Actions sync

### Tasks
- `task:pm-001` - JIRA/Notion Sync (8h)
- `task:pm-002` - Progress Dashboard (6h)

---

## 🚀 Epic 9: Beta Launch Preparation (M5)

**Goal**: Prepare for TestFlight & internal testing  
**Duration**: 4 weeks  
**Est. Hours**: 32h

### Key Deliverables
- 📱 TestFlight builds (iOS)
- 🤖 Internal testing track (Android)
- 🔐 Beta user management
- 📊 Analytics & crash reporting
- 📧 Feedback collection system

### Tasks
- `task:beta-001` - TestFlight Setup (8h)
- `task:beta-002` - Beta Feedback System (8h)
- `task:infra-001` - Staging Environment (8h)
- `task:infra-002` - Production Setup (8h)

---

## 📅 Timeline Summary

| Milestone | Focus | Duration | Due Date |
|-----------|-------|----------|----------|
| **M1** | Navigation & Auth | 4 weeks | Jan 15, 2026 |
| **M2** | Core UX & Profiles | 6 weeks | Mar 31, 2026 |
| **M3** | Messaging Beta | 8 weeks | May 31, 2026 |
| **M4** | Matching & Polish | 8 weeks | Jul 31, 2026 |
| **M5** | Testing & DevOps | 6 weeks | Sep 30, 2026 |
| **M6** | Public Launch | 8 weeks | Nov 30, 2026 |

---

## 📊 Task Distribution by Category

| Category | Tasks | Est. Hours | Priority Breakdown |
|----------|-------|------------|--------------------|
| Infrastructure | 9 | 86h | Critical: 1, High: 8 |
| Authentication | 10 | 58h | Critical: 2, High: 4 |
| UI/UX | 18 | 130h | High: 12, Medium: 6 |
| Messaging | 10 | 88h | High: 8, Medium: 2 |
| Matching | 11 | 114h | High: 9, Medium: 2 |
| Profile | 9 | 80h | High: 6, Medium: 3 |
| Testing | 5 | 74h | High: 4, Medium: 1 |
| DevOps | 3 | 28h | High: 2, Medium: 1 |
| Project Mgmt | 2 | 14h | Medium: 2 |

---

## 🎯 Next Actions

1. **This Week**: Start `task:infra-005` (Smart Queue System) - critical for scalability
2. **Code Review**: Existing messaging implementation for optimization opportunities
3. **Design**: Finalize design system tokens and component specs
4. **Planning**: Schedule sprint planning for M2 tasks

---

## 📝 Notes

- **Queue System**: Use BullMQ with Redis for job processing. Critical for avoiding SQLite bottlenecks.
- **Google OAuth**: Official branding required - download from Google Identity branding guidelines.
- **Performance**: Target 60fps on all platforms. Use Compose Metrics for profiling.
- **Testing**: Start writing tests alongside features, not after. Aim for 80%+ coverage.
- **Monitoring**: Set up Sentry early to catch production issues before launch.

---

**Generated from**: `.code-hq/graph.jsonld`  
**Export to**: JIRA/Notion via `task:pm-001`  
**Updates**: Run `npx code-hq tasks` for live status
