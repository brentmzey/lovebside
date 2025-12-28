# Tasks

## Phase 2: Navigation & Core Screens

### NAV-001: Implement RootComponent Navigation
- **Priority**: HIGH
- **Status**: In Progress
- **Estimated Hours**: 8
- **Milestone**: M1
- **Description**: Implement full Decompose navigation in RootComponent with child components for Auth, Onboarding, and Home flows.

### NAV-002: Define Navigation Graph
- **Priority**: HIGH
- **Status**: In Progress
- **Estimated Hours**: 4
- **Milestone**: M1
- **Description**: Create navigation graph defining Auth → Onboarding → Home flow with proper state management.

### NAV-003: Bottom Navigation Setup
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 6
- **Milestone**: M1
- **Description**: Wire up bottom navigation tabs: Discover, Matches, Messages, Profile.

### AUTH-001: Complete Login Integration
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 4
- **Milestone**: M1
- **Description**: Complete login screen integration with PocketBase authentication.

### AUTH-002: Complete Signup Flow
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 8
- **Milestone**: M1
- **Description**: Multi-step signup flow with email verification handling.

### AUTH-003: Session Persistence
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 4
- **Milestone**: M1
- **Description**: Implement session persistence and auto-login on app restart.

### ONBOARD-001: Profile Creation Wizard
- **Priority**: HIGH
- **Status**: In Progress
- **Estimated Hours**: 12
- **Milestone**: M1
- **Description**: Multi-step profile creation wizard (name, birthdate, bio, photo, seeking status).

## Updates: Premium UX & Intelligence

### UI-001: Premium Landing Screen
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 8
- **Milestone**: M2
- **Description**: "Apple-Style" Orbit animation, trigonometric avatar layout, precise colors.

### UI-002: Apple-Style Auth
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 6
- **Milestone**: M2
- **Description**: Glassmorphism, biometric integration, seamless transitions.

### BACKEND-001: Matching Engine
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 8
- **Milestone**: M3
- **Description**: Cron job (`cron_matching.ts`) implementing Jaccard+Proust+Location algorithm.

### BACKEND-002: Discovery Geolocation
- **Priority**: HIGH
- **Status**: To Do
- **Estimated Hours**: 6
- **Milestone**: M3
- **Description**: Client-side location fetching and backend filtering.

### MSG-001: Conversations List
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 6
- **Milestone**: M3
- **Description**: Conversations list screen showing active chats.

### MSG-002: Chat Screen
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 12
- **Milestone**: M3
- **Description**: Individual chat screen with message input and realtime updates.

### MSG-003: Push Notifications
- **Priority**: HIGH
- **Status**: Not Started
- **Estimated Hours**: 8
- **Milestone**: M3
- **Description**: Integrate push notifications for new messages.

### MSG-004: Offline Cache & Sync
- **Priority**: HIGH
- **Status**: In Progress
- **Estimated Hours**: 12
- **Milestone**: M3
- **Description**: Implement offline caching with distributed cache support (t_user_property) for airplane mode. Auto-sync when back online.
- **Progress**: 
  - ✅ Created OfflineCacheManager with LRU cache and TTL
  - ✅ Created platform-specific NetworkMonitor (Android, iOS, JVM, JS)
  - ✅ Integrated cache into PocketBaseMessagingRepository
  - ✅ Added pending operations queue for offline sends
  - ⏳ Need to fix iOS compilation and Profile Repository issues
  - ⏳ Need to test multi-platform demo

### MSG-005: Real-time Messaging Demo
- **Priority**: HIGH
- **Status**: To Do
- **Estimated Hours**: 4
- **Milestone**: M3
- **Description**: Create demo script to launch multiple platform targets and test real-time messaging between users.

## Phase 4: Platform Targets & Demo

### DEMO-001: Multi-Platform Build
- **Priority**: HIGH
- **Status**: In Progress
- **Estimated Hours**: 6
- **Milestone**: M4
- **Description**: Build and run app on all targets: Android, iOS, Web, Desktop.

### DEMO-002: Real-time Messaging Recording
- **Priority**: HIGH
- **Status**: To Do
- **Estimated Hours**: 2
- **Milestone**: M4
- **Description**: Record screen demos showing real-time messaging between 2+ users across different platforms.
- **Estimated Hours**: 12
- **Milestone**: M3
- **Description**: Push notification infrastructure for messages and matches.

### MSG-004: Messaging Polish
- **Priority**: HIGH
- **Status**: Done
- **Estimated Hours**: 8
- **Milestone**: M3
- **Description**: Threaded replies, read receipts, and premium styling for chat.
