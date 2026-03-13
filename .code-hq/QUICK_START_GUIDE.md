# 🚀 BSide Development Quick Start Guide

**Last Updated:** January 30, 2026

## 📖 Essential Reading

Start here to understand the project:

1. **[UI_IMPLEMENTATION_REPORT.md](./UI_IMPLEMENTATION_REPORT.md)** - UI/UX status, components, platform coverage
2. **[BACKEND_ARCHITECTURE_REPORT.md](./BACKEND_ARCHITECTURE_REPORT.md)** - Scalability strategy, load balancing, race conditions
3. **[graph.jsonld](./graph.jsonld)** - Complete task breakdown (4 epics, 20+ stories)
4. **[JIRA_EXPORT_FULL.csv](./JIRA_EXPORT_FULL.csv)** - Import into Notion/JIRA

## 🏗️ Project Structure

```
bside/
├── .code-hq/                    # Project knowledge graph
│   ├── graph.jsonld             # Tasks, epics, stories
│   ├── UI_IMPLEMENTATION_REPORT.md
│   ├── BACKEND_ARCHITECTURE_REPORT.md
│   └── ... (20+ docs)
│
├── composeApp/                  # Main KMP app
│   └── src/
│       ├── commonMain/          # Shared code
│       │   └── kotlin/love/bside/app/
│       │       ├── ui/
│       │       │   ├── screens/  # AuthScreen, DashboardScreen, etc.
│       │       │   ├── components/ # Reusable UI components
│       │       │   └── theme/    # Design system
│       │       ├── domain/       # Business logic
│       │       └── data/         # Repositories
│       ├── androidMain/         # Android-specific
│       ├── iosMain/             # iOS-specific
│       ├── jvmMain/             # Desktop-specific
│       └── jsMain/              # Web-specific
│
├── shared/                      # Shared KMP module
├── server/                      # PocketBase backend (Go)
├── nginx/                       # Load balancer config
└── workers/                     # Background jobs (TODO)
```

## 🎨 UI/UX Status (60% Complete)

### ✅ Completed
- Authentication (login/signup with biometrics)
- Dashboard (matches, insights, quick actions)
- Messaging (bubbles, typing indicators, reactions)
- Design system (colors, typography, shapes)
- Responsive layouts (mobile, tablet, desktop)
- Loading states and animations

### 🚧 In Progress
- Proust questionnaire flow
- Profile edit screen
- Discovery swipe cards

### 📋 TODO
- Photo upload to CDN
- Settings screen
- Onboarding flow

## 🏗️ Backend Status (20% Complete)

### ✅ Designed
- Nginx load balancing strategy
- Job queue architecture (Redis + BullMQ)
- Race condition prevention (optimistic locking)
- CDN integration (S3 + CloudFront)
- PocketBase schema optimization

### 🚧 In Progress
- PocketBase schema migrations
- API endpoints

### 📋 TODO
- Nginx configuration
- Job queue implementation
- CDN setup
- Matching algorithms

## 🧪 Testing Status (15% Complete)

### ✅ Exists
- AuthUiStateTest.kt

### 📋 TODO
- Unit tests for ViewModels
- Integration tests for APIs
- E2E tests for user flows
- Load tests (k6)
- Cross-platform tests

## 🚀 How to Run

### Prerequisites
```bash
# Install tools
brew install just    # Task runner
brew install bun     # For code-hq CLI
```

### Run the App
```bash
# Android
just android

# iOS (requires Mac + Xcode)
just ios

# Desktop
just desktop

# Web
just web
```

### Run Backend
```bash
# Start PocketBase
just pocketbase

# View admin UI
open http://127.0.0.1:8090/_/
```

### Run Tests
```bash
# All tests
./gradlew test

# Specific module
./gradlew :composeApp:test
```

## 📋 Priority Tasks (This Week)

### 1. PocketBase Schema ⭐⭐⭐
**Why:** Foundation for everything
**Tasks:**
- Create idempotent migrations
- Add indexes for performance
- Set up self-referencing relations
- Test migration rollback

**Files to create:**
```
server/pb_migrations/
├── 1704067200_create_users.js
├── 1704067201_create_profiles.js
├── 1704067202_create_messages.js
├── 1704067203_create_matches.js
└── 1704067204_create_proust_answers.js
```

### 2. Proust Questionnaire UI ⭐⭐
**Why:** Critical for matching
**Tasks:**
- Progress indicator component
- Question-by-question navigation
- Auto-save to backend
- Review screen
- Completion animation

**Files to update:**
```
composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/proust/
├── QuestionnaireScreen.kt
├── QuestionnaireViewModel.kt
└── QuestionnaireController.kt
```

### 3. Discovery Feed ⭐⭐
**Why:** Core user experience
**Tasks:**
- Swipeable card component
- Like/pass gestures
- Match modal
- Empty state

**Files to create:**
```
composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/discover/
├── DiscoverScreen.kt
├── DiscoverViewModel.kt
└── components/
    ├── SwipeableCard.kt
    └── MatchModal.kt
```

## 🏗️ Architecture Decisions

### UI Framework
**Choice:** Compose Multiplatform
**Why:** Single codebase for Android, iOS, Desktop, Web
**Trade-off:** Some platform-specific code needed

### Backend
**Choice:** PocketBase + SQLite
**Why:** Rapid development, built-in auth, realtime
**Trade-off:** SQLite has write limits (mitigated with Nginx + job queue)
**Plan:** Migrate to PostgreSQL at 50k+ users

### Job Queue
**Choice:** Redis + BullMQ
**Why:** Proven, reliable, great monitoring
**Trade-off:** Adds infrastructure complexity

### CDN
**Choice:** AWS S3 + CloudFront
**Why:** Cost-effective, global, unlimited scale
**Trade-off:** Added complexity vs. storing in DB

## 📊 Performance Targets

| Metric | Target | Current |
|--------|--------|---------|
| API response (p95) | < 200ms | ~500ms* |
| Page load time | < 2s | ~3s* |
| Database write latency | < 100ms | ~300ms* |
| CDN cache hit rate | > 90% | N/A |
| Concurrent users | 1000+ | ~100* |

*Estimated, not yet load tested

## 🐛 Known Issues

1. **iOS biometric auth** - Not tested on device
2. **Web image upload** - Needs file picker implementation
3. **SQLite write contention** - Mitigated by job queue plan
4. **Stale read on profile updates** - Needs optimistic locking

## 🆘 Getting Help

### Documentation
- See `.code-hq/` for all project docs
- Check `BACKEND_QUICKSTART.md` for backend setup
- Read `MESSAGING_UI_QUICKSTART.md` for messaging features

### Key Files
- **Design System:** `shared/src/commonMain/kotlin/love/bside/app/ui/design/`
- **API Client:** `shared/src/commonMain/kotlin/love/bside/app/data/`
- **Navigation:** `composeApp/src/commonMain/kotlin/love/bside/app/ui/navigation/`

### Common Commands
```bash
# Clean build
./gradlew clean build

# Run specific screen demo
just demo-auth
just demo-dashboard
just demo-messaging

# View code-hq board
bun run code-hq show --view kanban

# Validate graph
bun run code-hq validate
```

## 🎯 Definition of Done

For each task/story:
- [ ] Code implemented
- [ ] Unit tests pass (>80% coverage)
- [ ] Integration tests pass
- [ ] Tested on Android + 1 other platform
- [ ] No console errors/warnings
- [ ] Code reviewed
- [ ] Documentation updated
- [ ] Merged to main

## 🚦 Project Health

| Category | Status |
|----------|--------|
| Planning | 🟢 Excellent |
| UI/UX | 🟢 Strong |
| Backend API | 🟡 Partial |
| Scalability | 🟡 Designed |
| Testing | 🟡 Minimal |
| Documentation | 🟢 Excellent |

**Overall:** 🟢 Healthy and ready for focused development

## 📅 Timeline

```
Week 1-2:   ✅ Planning + UI foundation
Week 3-4:   🎯 YOU ARE HERE - Schema + Questionnaire + Discovery
Week 5-6:   Backend scalability (Nginx, Queue, CDN)
Week 7-8:   Matching algorithms
Week 9-10:  Testing + Polish
Week 11-12: Beta + Production prep
```

## 🎉 Quick Wins

Want to see progress fast? Start here:

1. **Run the app** - `just android` and explore existing screens
2. **Fix a small bug** - Check GitHub issues
3. **Add a unit test** - Improve test coverage
4. **Polish an animation** - Tweak existing screens
5. **Write a migration** - Set up database schema

---

**Remember:** All tasks are tracked in `.code-hq/graph.jsonld`. Export to Notion/JIRA for team collaboration!

**Questions?** Check the documentation or ask the team!
