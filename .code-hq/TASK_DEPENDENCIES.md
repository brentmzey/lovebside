# B-Side Task Dependencies & Orchestration

**Last Updated**: January 30, 2026

This document outlines the optimal order of task execution based on dependencies, ensuring smooth development flow.

---

## 🔄 Critical Path (Must Complete First)

These tasks block the most other work and should be prioritized:

### 1. Infrastructure Foundation (Week 1-2)
```
task:infra-005 (Smart Queue System) [16h]
    ↓
task:infra-006 (Read/Write Separation) [12h]
    ↓
task:infra-008 (Event Streams) [14h]
```

**Why First**: Everything else depends on scalable backend. Without queue system, we hit SQLite write bottlenecks.

### 2. Core Navigation & Auth (Week 1-3)
```
task:nav-001 (RootComponent) [8h]
    ↓
task:nav-002 (Navigation Graph) [4h]
    ↓
task:auth-001 (Login) [4h] + task:auth-007 (Google OAuth Backend) [8h]
    ↓
task:auth-002 (Signup) [8h] + task:auth-008 (Google OAuth UI) [10h]
```

**Why First**: Users can't use the app without auth. All features require authenticated users.

### 3. Design System (Week 2-3)
```
task:ui-003 (Design Tokens) [8h]
    ↓
task:ui-004 (Component Library) [16h]
    ↓
All other UI tasks can use components
```

**Why First**: Prevents UI inconsistency and rework. All screens depend on design system.

---

## 🌊 Parallel Work Streams

Once critical path is complete, these can be worked in parallel:

### Stream A: Profile & Onboarding
```
task:onboard-001 (Profile Wizard) [12h]
task:profile-007 (Photo Upload) [12h]
task:profile-008 (Profile Prompts) [10h]
task:profile-009 (Relationship Goals) [8h]
```
**Team**: 1-2 developers  
**Duration**: 2-3 weeks

### Stream B: Proust Questionnaire
```
task:proust-001 (Questionnaire UI) [12h]
    ↓
task:proust-007 (Beautiful UX) [12h]
task:proust-008 (Auto-Save) [6h]
```
**Team**: 1 developer  
**Duration**: 2 weeks

### Stream C: Messaging Core
```
task:msg-001 (Conversations List) [6h]
task:msg-002 (Chat Screen) [12h]
    ↓
task:msg-007 (Rich Messages) [16h]
task:msg-008 (Delivery Status) [6h]
task:msg-009 (Typing Indicators) [4h]
task:msg-010 (Message Search) [8h]
```
**Team**: 2 developers  
**Duration**: 3-4 weeks

### Stream D: Matching & Discovery
```
task:match-001 (Discover Screen) [8h]
task:match-002 (Profile Card) [8h]
task:match-004 (Swipe Gestures) [8h]
    ↓
task:match-005 (Algorithm Integration) [12h]
task:match-008 (Affinity Score) [16h]
task:match-009 (Vibes Matching) [20h]
task:match-010 (Geographic) [10h]
```
**Team**: 1-2 developers (1 backend, 1 frontend)  
**Duration**: 4-5 weeks

---

## 📋 Dependency Chains

### Infrastructure Chain
```
task:infra-005 (Queue)
    ├─→ task:infra-006 (Read/Write)
    ├─→ task:infra-008 (Events)
    └─→ task:match-011 (Batch Jobs)
```

### Auth Chain
```
task:auth-001 (Login)
    ├─→ task:auth-002 (Signup)
    └─→ task:onboard-001 (Onboarding)
            └─→ task:profile-007, 008, 009 (Profile)

task:auth-007 (Google OAuth Backend)
    └─→ task:auth-008 (Google OAuth UI)
```

### UI Chain
```
task:ui-003 (Tokens)
    └─→ task:ui-004 (Components)
            ├─→ task:ui-005 (Animations)
            ├─→ task:proust-007 (Proust UX)
            ├─→ task:msg-007 (Rich Messages)
            └─→ task:match-002 (Profile Card)
```

### Messaging Chain
```
task:msg-002 (Chat Screen)
    ├─→ task:msg-007 (Rich Messages)
    ├─→ task:msg-008 (Delivery Status)
    ├─→ task:msg-009 (Typing)
    └─→ task:msg-010 (Search)
```

### Matching Chain
```
task:match-005 (Algorithm Integration)
    ├─→ task:match-008 (Affinity)
    ├─→ task:match-009 (Vibes)
    └─→ task:match-010 (Geographic)
            └─→ task:backend-002 (Geolocation)

task:infra-005 (Queue)
    └─→ task:match-011 (Batch Jobs)
```

### Testing Chain
```
task:test-001 (Backend Tests) - Can start anytime
task:test-002 (SDK Tests) - Parallel with feature dev
task:test-003 (Screenshot Tests) - After task:ui-004
task:test-004 (E2E Tests) - After core flows done
task:test-005 (Load Tests) - After task:infra-007
```

---

## 🎯 Sprint Planning Recommendations

### Sprint 1 (Weeks 1-2): Foundation
**Goal**: Infrastructure + Auth + Navigation

**Must Have**:
- task:infra-005 (Queue) [CRITICAL]
- task:nav-001 (Navigation)
- task:auth-001 (Login)
- task:auth-007 (OAuth Backend)

**Should Have**:
- task:infra-006 (Read/Write)
- task:ui-003 (Design Tokens)

**Est. Hours**: 60h

---

### Sprint 2 (Weeks 3-4): Core UX
**Goal**: Design System + Profile + Onboarding

**Must Have**:
- task:ui-004 (Components)
- task:auth-002 (Signup)
- task:onboard-001 (Onboarding)
- task:profile-007 (Photo Upload)

**Should Have**:
- task:auth-008 (OAuth UI)
- task:profile-008 (Prompts)

**Est. Hours**: 70h

---

### Sprint 3 (Weeks 5-6): Proust & Matching Foundation
**Goal**: Questionnaire + Discovery Screen

**Must Have**:
- task:proust-001 (Questionnaire)
- task:proust-007 (Beautiful UX)
- task:match-001 (Discover)
- task:match-002 (Profile Card)

**Should Have**:
- task:match-004 (Swipe)
- task:proust-008 (Auto-Save)

**Est. Hours**: 64h

---

### Sprint 4 (Weeks 7-8): Messaging Core
**Goal**: Real-time Chat

**Must Have**:
- task:msg-001 (Conversations)
- task:msg-002 (Chat Screen)
- task:msg-003 (Push Notifications)
- task:msg-004 (Offline)

**Should Have**:
- task:msg-007 (Rich Messages)
- task:msg-009 (Typing)

**Est. Hours**: 64h

---

### Sprint 5 (Weeks 9-10): Advanced Matching
**Goal**: Smart Matching Algorithms

**Must Have**:
- task:match-005 (Algorithm Integration)
- task:match-008 (Affinity)
- task:backend-002 (Geolocation)

**Should Have**:
- task:match-010 (Geographic)
- task:match-011 (Batch Jobs)

**Est. Hours**: 58h

---

### Sprint 6 (Weeks 11-12): Polish & Performance
**Goal**: UI Polish + Performance

**Must Have**:
- task:ui-006 (Platform Adaptations)
- task:ui-007 (Performance)
- task:infra-007 (Load Balancing)

**Should Have**:
- task:polish-001, 002, 003
- task:ui-005 (Animations)

**Est. Hours**: 62h

---

### Sprint 7 (Weeks 13-14): Testing & DevOps
**Goal**: Comprehensive Testing + Monitoring

**Must Have**:
- task:test-001 (Backend Tests)
- task:test-002 (SDK Tests)
- task:devops-001 (APM)
- task:devops-003 (CI/CD)

**Should Have**:
- task:test-003 (Screenshot)
- task:test-004 (E2E)
- task:devops-002 (Logging)

**Est. Hours**: 72h

---

### Sprint 8 (Weeks 15-16): Beta Prep
**Goal**: Launch Ready

**Must Have**:
- task:beta-001 (TestFlight)
- task:beta-002 (Feedback)
- task:test-005 (Load Testing)
- task:infra-001, 002 (Environments)

**Est. Hours**: 32h

---

## ⚠️ Bottleneck Risks

### Risk 1: Queue System Delays
**Impact**: Blocks infra, events, and batch jobs  
**Mitigation**: Start task:infra-005 ASAP, allocate senior dev

### Risk 2: Design System Not Ready
**Impact**: UI inconsistency, rework  
**Mitigation**: Complete task:ui-003 + ui-004 before other UI work

### Risk 3: PocketBase Scalability Issues
**Impact**: Messaging lag, slow matching  
**Mitigation**: Load test early (task:test-005), implement queue system

### Risk 4: OAuth Integration Complexity
**Impact**: Auth delays block everything  
**Mitigation**: Start task:auth-007 early, use test credentials

---

## 📊 Resource Allocation

### Optimal Team Structure (4 developers)

**Backend Dev 1** (Infrastructure Focus):
- Weeks 1-4: Infrastructure (queue, events, load balancing)
- Weeks 5-8: Matching algorithms backend
- Weeks 9-12: Performance optimization

**Backend Dev 2** (Features Focus):
- Weeks 1-4: Auth + OAuth
- Weeks 5-8: Messaging backend + push notifications
- Weeks 9-12: Testing + DevOps

**Frontend Dev 1** (Core Features):
- Weeks 1-4: Navigation + Design System
- Weeks 5-8: Profile + Onboarding
- Weeks 9-12: Messaging UI

**Frontend Dev 2** (Matching & Polish):
- Weeks 1-4: Proust UI
- Weeks 5-8: Discovery + Matching UI
- Weeks 9-12: Polish + Performance

---

## 🔍 Dependency Visualization

```
                    ┌─────────────┐
                    │ task:infra-005 │
                    │  (Queue)    │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌──────────┐   ┌───────────┐   ┌──────────┐
    │ infra-006│   │ infra-008 │   │match-011 │
    │(Read/Write)  │ (Events)  │   │(Batch)   │
    └──────────┘   └───────────┘   └──────────┘

    ┌──────────┐
    │ nav-001  │
    └────┬─────┘
         ▼
    ┌──────────┐   ┌──────────┐
    │ auth-001 │   │auth-007  │
    │ (Login)  │   │(OAuth BE)│
    └────┬─────┘   └────┬─────┘
         │              │
         ▼              ▼
    ┌──────────┐   ┌──────────┐
    │ auth-002 │   │auth-008  │
    │ (Signup) │   │(OAuth UI)│
    └────┬─────┘   └──────────┘
         ▼
    ┌──────────┐
    │onboard-001
    │ (Wizard) │
    └────┬─────┘
         │
    ┌────┴────┬────────┬──────────┐
    ▼         ▼        ▼          ▼
┌────────┐┌────────┐┌────────┐┌────────┐
│prof-007││prof-008││prof-009││proust-*│
│(Photos)││(Prompts)│(Goals) ││        │
└────────┘└────────┘└────────┘└────────┘
```

---

## 📝 Notes

- **Always check dependencies** before starting a task
- **Communicate blockers** immediately to avoid delays
- **Test as you go** - don't wait for testing sprint
- **Review code daily** to catch issues early
- **Update .code-hq** after completing each task

---

**Generated from**: `.code-hq/graph.jsonld`  
**Updates**: `npx code-hq tasks --status in-progress` to see active work
