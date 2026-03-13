# B-Side Project Roadmap

**Last Updated**: December 5, 2025  
**Status**: Active Development  
**Version**: 1.0.0-alpha

---

## 📊 Executive Summary

B-Side is a Kotlin Multiplatform dating/connection app focused on **values-based matching** through Proust-style questionnaires and an emotion graph system. The app runs on Android, iOS, Desktop (JVM), Web (JS/Wasm), with a Ktor backend and PocketBase database.

### Current State ✅
- **Build System**: Fully functional `gradle build` across all KMP targets
- **Infrastructure**: PocketBase hosted, Ktor server operational
- **Design System**: Figma-based design system extracted and applied
- **Authentication**: Login/signup with biometric support scaffolded
- **SDK**: Custom PocketBase Kotlin SDK (multiplatform, publishable)

### Key Gaps 🔄
- Navigation implementation (Decompose `RootComponent` is placeholder)
- Core matching/discovery screens not implemented
- Emotion graph UI not built
- Profile creation/editing flow incomplete
- Messaging system not implemented
- Push notifications not configured
- Production deployment pipeline pending

---

## 🗺️ Roadmap Phases

### Phase 1: Core Foundation ✅ (Completed)
**Timeline**: September - November 2025

| Task | Status | Notes |
|------|--------|-------|
| KMP project setup | ✅ Done | All 6 targets configured |
| Gradle build working | ✅ Done | `gradle build` succeeds |
| PocketBase schema design | ✅ Done | 12+ collections defined |
| Design system extraction | ✅ Done | Colors, typography, shapes |
| Ktor server scaffolding | ✅ Done | Routes, plugins, repos |
| PocketBase SDK creation | ✅ Done | Standalone multiplatform SDK |
| Biometric auth scaffolding | ✅ Done | Android/iOS secure storage |
| Dev scripts | ✅ Done | 10+ shell scripts |

---

### Phase 2: Navigation & Core Screens 🔧 (In Progress)
**Timeline**: December 2025 - January 2026  
**Priority**: HIGH

#### 2.1 Navigation Architecture
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Implement `RootComponent` with Decompose | HIGH | 8 |
| Define navigation graph (Auth → Onboarding → Home) | HIGH | 4 |
| Create child components (AuthComponent, HomeComponent, etc.) | HIGH | 8 |
| Wire up bottom navigation (Discover, Matches, Messages, Profile) | HIGH | 6 |
| Handle deep links | MEDIUM | 4 |

#### 2.2 Authentication Flow
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Complete login screen integration | HIGH | 4 |
| Complete signup flow (multi-step) | HIGH | 8 |
| Email verification handling | HIGH | 4 |
| Password reset flow | MEDIUM | 4 |
| Biometric enrollment UI | MEDIUM | 6 |
| Session persistence & auto-login | HIGH | 4 |

#### 2.3 Onboarding Flow
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Profile creation wizard | HIGH | 12 |
| Photo upload (profile pictures) | HIGH | 8 |
| Location permissions & selection | MEDIUM | 4 |
| Key values selection screen | HIGH | 8 |
| Seeking status selection | HIGH | 2 |
| Onboarding completion state | HIGH | 2 |

---

### Phase 3: Core Features 🎯 (Planned)
**Timeline**: January - February 2026  
**Priority**: HIGH

#### 3.1 Proust Questionnaire System
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Questionnaire screen UI (cards/swipe) | HIGH | 12 |
| Answer drafting & local state | HIGH | 6 |
| Answer submission to PocketBase | HIGH | 4 |
| Realtime updates (SSE/polling) | HIGH | 6 |
| Progress indicator & completion | MEDIUM | 4 |
| Review/edit previous answers | MEDIUM | 6 |

#### 3.2 Discover/Matching System
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Discover screen layout | HIGH | 8 |
| Profile card component | HIGH | 8 |
| Compatibility score display | HIGH | 4 |
| Swipe gestures (like/pass) | HIGH | 8 |
| Match algorithm integration | HIGH | 12 |
| Shared values highlight | MEDIUM | 4 |
| Filter preferences (seeking, distance) | MEDIUM | 8 |

#### 3.3 Profile System
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Profile view screen (self) | HIGH | 8 |
| Profile view screen (others) | HIGH | 6 |
| Profile editing screen | HIGH | 8 |
| Photo gallery management | HIGH | 10 |
| Values/interests management | MEDIUM | 6 |
| Profile completeness indicator | LOW | 4 |

---

### Phase 4: Emotion Graph System 🧠 (Planned)
**Timeline**: February - March 2026  
**Priority**: MEDIUM-HIGH

#### 4.1 Graph Foundation
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Emotion terms list UI | HIGH | 6 |
| Create graph item (person/thing/memory) | HIGH | 8 |
| Create emotion edge (connection) | HIGH | 8 |
| Intensity slider/selector | MEDIUM | 4 |
| Custom verb/adverb modifiers | LOW | 6 |

#### 4.2 Graph Visualization
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Graph node visualization | MEDIUM | 12 |
| Edge rendering (relationships) | MEDIUM | 8 |
| Interactive graph exploration | MEDIUM | 10 |
| Shared graph overlays (matching) | LOW | 12 |

#### 4.3 Graph Insights
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Compatibility insights from graph | MEDIUM | 8 |
| "You both love..." highlights | MEDIUM | 4 |
| Graph-based conversation starters | LOW | 6 |

---

### Phase 5: Messaging System 💬 (Planned)
**Timeline**: March - April 2026  
**Priority**: HIGH

#### 5.1 Conversations
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Conversations list screen | HIGH | 6 |
| Individual chat screen | HIGH | 12 |
| Message input & sending | HIGH | 6 |
| Realtime message updates (SSE) | HIGH | 8 |
| Message read receipts | MEDIUM | 4 |
| Typing indicators | LOW | 4 |

#### 5.2 Rich Messaging
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Image sharing in chat | MEDIUM | 8 |
| Reactions/emoji responses | LOW | 6 |
| Icebreaker prompts integration | MEDIUM | 6 |
| "Ask about their answer" feature | LOW | 8 |

#### 5.3 Notifications
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Push notification infrastructure | HIGH | 12 |
| New message notifications | HIGH | 4 |
| New match notifications | HIGH | 4 |
| In-app notification badge system | MEDIUM | 4 |
| Notification preferences | MEDIUM | 4 |

---

### Phase 6: Polish & UX 🎨 (Planned)
**Timeline**: April - May 2026  
**Priority**: MEDIUM

#### 6.1 Animations & Transitions
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Screen transitions (shared element) | MEDIUM | 8 |
| Card swipe animations | HIGH | 6 |
| Loading states & skeletons | MEDIUM | 6 |
| Success/error feedback animations | LOW | 4 |
| Pull-to-refresh | MEDIUM | 2 |

#### 6.2 Accessibility
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Screen reader support | HIGH | 8 |
| Touch target sizing | MEDIUM | 4 |
| Color contrast verification | MEDIUM | 4 |
| Keyboard navigation (Web/Desktop) | MEDIUM | 6 |
| Dynamic text sizing | MEDIUM | 4 |

#### 6.3 Platform-Specific Polish
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| iOS-specific haptics | LOW | 4 |
| Android Material You theming | LOW | 6 |
| Desktop window management | LOW | 4 |
| Web responsive breakpoints | MEDIUM | 8 |

---

### Phase 7: Infrastructure & DevOps 🏗️ (Planned)
**Timeline**: May - June 2026  
**Priority**: HIGH (for launch)

#### 7.1 Build & Distribution
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Android app signing (release) | HIGH | 4 |
| iOS provisioning & certificates | HIGH | 6 |
| Desktop distribution (DMG, EXE, DEB) | MEDIUM | 8 |
| Web deployment (CDN/hosting) | HIGH | 4 |
| Server Docker production image | HIGH | 4 |

#### 7.2 CI/CD
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Re-enable GitHub Actions (selective) | HIGH | 4 |
| Automated test runs | HIGH | 6 |
| Artifact publishing pipeline | MEDIUM | 8 |
| Version management automation | MEDIUM | 4 |
| Release notes generation | LOW | 2 |

#### 7.3 Monitoring & Analytics
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Crash reporting (Sentry/Crashlytics) | HIGH | 6 |
| Analytics integration | MEDIUM | 8 |
| Server health monitoring | HIGH | 4 |
| Performance metrics | MEDIUM | 6 |
| User behavior tracking | LOW | 6 |

---

### Phase 8: Beta & Launch 🚀 (Planned)
**Timeline**: June - July 2026  
**Priority**: HIGH

#### 8.1 Beta Testing
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| TestFlight setup (iOS) | HIGH | 4 |
| Internal testing track (Android) | HIGH | 4 |
| Beta feedback collection | MEDIUM | 6 |
| Bug triage process | HIGH | Ongoing |

#### 8.2 Store Listings
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| App Store listing & screenshots | HIGH | 8 |
| Play Store listing & screenshots | HIGH | 8 |
| Privacy policy & terms | HIGH | 4 |
| App review preparation | HIGH | 4 |

#### 8.3 Launch
| Task | Priority | Estimated Hours |
|------|----------|-----------------|
| Production environment verification | HIGH | 4 |
| Load testing | HIGH | 8 |
| Launch day monitoring | HIGH | 8 |
| Post-launch hotfix process | HIGH | Ongoing |

---

## 📈 Milestone Summary

| Milestone | Target Date | Key Deliverables |
|-----------|-------------|------------------|
| **M1: Navigation Complete** | Jan 15, 2026 | Working app navigation, auth flow |
| **M2: Core MVP** | Feb 28, 2026 | Profiles, Proust, basic matching |
| **M3: Messaging Beta** | Apr 15, 2026 | Chat system, notifications |
| **M4: Polish Release** | May 31, 2026 | Animations, accessibility, platform polish |
| **M5: Beta Launch** | Jun 30, 2026 | TestFlight/Internal testing |
| **M6: Public Launch** | Jul 31, 2026 | App Store & Play Store |

---

## 🔧 Technical Debt & Maintenance

### Ongoing Tasks
| Task | Frequency | Notes |
|------|-----------|-------|
| Dependency updates | Monthly | Kotlin, Compose, Ktor versions |
| PocketBase SDK updates | As needed | Track upstream changes |
| Gradle deprecation fixes | Quarterly | Address warnings before major versions |
| Security patches | Immediate | Critical vulnerabilities |
| Code cleanup & refactoring | Ongoing | Tech debt reduction |

### Known Technical Debt
- [ ] Gradle `jsNpmAggregated` warnings (clean up before Gradle 10)
- [ ] Configuration cache optimization needed
- [ ] Build metrics/telemetry not captured
- [ ] Some placeholder components need real implementations

---

## 📋 Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| Nov 2025 | Disabled GitHub Actions | Cost optimization during active development |
| Nov 2025 | PocketBase as primary DB | Fast iteration, embedded auth, realtime |
| Nov 2025 | Decompose for navigation | Pure Kotlin, multiplatform compatible |
| Nov 2025 | Custom PocketBase SDK | No existing KMP SDK, needed realtime support |
| Oct 2025 | Figma-first design | Consistent brand across platforms |

---

## 🎯 Success Metrics

### Launch Goals
- **MAU Target**: 1,000 monthly active users within 3 months of launch
- **Retention**: 40% D7 retention rate
- **Matches**: Average 5 matches per active user per week
- **Engagement**: 15 minutes average daily session time

### Technical Goals
- **Build Time**: < 3 minutes for incremental builds
- **App Size**: < 50MB on Android, < 100MB on iOS
- **Crash Rate**: < 1% crash-free rate
- **API Latency**: < 200ms p95 response time

---

## 📞 Team & Resources

### Key Files Reference
- **Build**: `build.gradle.kts`, `gradle/libs.versions.toml`
- **Design**: `docs/DESIGN_SYSTEM.md`, `docs/FIGMA_IMPLEMENTATION.md`
- **Database**: `docs/POCKETBASE_SCHEMA.md`
- **Scripts**: `scripts/README.md`
- **SDK**: `pocketbase-kt-sdk/README.md`

### Quick Commands
```bash
# Development
./scripts/run-desktop.sh      # Fastest iteration
./scripts/start-all.sh        # Full stack

# Testing
./scripts/verify-targets.sh   # Quick build check
./scripts/test-full-stack.sh  # Integration tests

# Build
gradle build                  # Full build + tests
gradle assemble              # Compile only
```

---

*This roadmap is a living document. Update as priorities shift and progress is made.*
