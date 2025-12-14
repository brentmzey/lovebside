# B-Side Project Overview

**Type**: Kotlin Multiplatform Dating/Connection App  
**Status**: Active Development (Alpha)  
**Last Updated**: December 5, 2025

## Quick Links

- [Project Roadmap](./docs/PROJECT_ROADMAP.md) - Comprehensive development roadmap
- [Build Status](./docs/BUILD_STATUS.md) - Platform compilation status
- [Gradle Roadmap](./docs/GRADLE_BUILD_ROADMAP.md) - Build system progress
- [Design System](./docs/DESIGN_SYSTEM.md) - UI/UX guidelines
- [PocketBase Schema](./docs/POCKETBASE_SCHEMA.md) - Database structure
- [Scripts Guide](./scripts/README.md) - Development scripts

## Current Focus

### Phase 2: Navigation & Core Screens (In Progress)
- Implementing Decompose navigation (`RootComponent`)
- Completing authentication flow
- Building onboarding/profile creation

### Immediate Priorities
1. **Navigation**: Wire up `RootComponent` with actual child components
2. **Auth Flow**: Complete login/signup integration with PocketBase
3. **Onboarding**: Profile creation wizard with photo upload
4. **Proust Tab**: Questionnaire UI (already scaffolded)

## Project Stats

| Metric | Value |
|--------|-------|
| Platforms | 6 (Android, iOS, Desktop, Web JS, Wasm, Server) |
| Build Status | ✅ `gradle build` passing |
| Collections | 12+ PocketBase collections |
| SDK | Custom PocketBase KMP SDK |

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
