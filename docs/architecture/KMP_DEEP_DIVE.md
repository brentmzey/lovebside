# Kotlin Multiplatform (KMP): Accellerating Small Teams

**"Write once, run everywhere" that actually works.**

This document explains how *B-Side* leverages **Kotlin Multiplatform (KMP)** to allow a small engineering team to deliver native quality apps across 4 platforms (Android, iOS, Desktop, Web) with the velocity of a team 4x its size.

---

## 1. Concepts & Terminology

Let's normalize the terms we use to avoid confusion.

| Term | Definition | Context |
| :--- | :--- | :--- |
| **KMP** | **Kotlin Multiplatform**. The technology that allows sharing Kotlin code across multiple platforms (JVM, Native, JS, Wasm). | *The Technology* |
| **KMM** | **Kotlin Multiplatform Mobile**. A subset of KMP specifically focused on sharing code between Android and iOS. *(Note: JetBrains is moving away from this term in favor of just KMP).* | *The Mobile Focus* |
| **CMP** | **Compose Multiplatform**. The UI framework by JetBrains (based on Google's Jetpack Compose) that renders UI across Android, iOS, Desktop, and Web. | *The Connective Tissue* |
| **Common** | Code in `commonMain` that uses only pure Kotlin libraries. Shared by ALL targets. | *The Holy Grail* |

### The "Supercharged" Pattern

In *B-Side*, we don't just share "business logic." We share **Application Behavior**.

- **Traditional KMM**: Share Data Models & Networking. Write UI 2x (SwiftUI + Jetpack Compose).
- **B-Side KMP**: Share **Everything** (ViewModels, Navigation, UI Components, Feature Screens). Write UI 1x (Generic Compose).

---

## 2. Architecture: The "Shared Core"

We treat the Platform (Android, iOS) as a thin "dumb" shell. The Application lives entirely in Shared Code.

```mermaid
graph TD
    subgraph "Platform Specific (The Shell)"
        android[Android Activity]
        ios[iOS ViewController]
        desktop[Desktop Window]
        web[Web Canvas]
    end

    subgraph "Shared KMP Core (The Brain)"
        cmp[Compose Multiplatform UI]
        vm[ViewModels & Presenters]
        repo[Repositories (Auth, Data)]
        api[API Client (Ktor)]
        db[Database (SQLDelight/PocketBase)]
    end

    android --> cmp
    ios --> cmp
    desktop --> cmp
    web --> cmp
```

### Why this Supercharges Small Teams

1. **Single Source of Truth**: When you fix a bug in the "Login Logic," you fix it for Android, iOS, Web, and Desktop simultaneously.
2. **Unified Type System**: We don't map JSON to Swift Structs to Kotlin Data Classes. We define a `User` type *once* in Kotlin, and it flows from the Database to the UI Button on every platform.
3. **Feature Parity**: You never have "Android is ahead of iOS." A new feature ships everywhere instantly.

---

## 3. The Tech Stack

- **Language**: Kotlin 2.0+
- **UI**: Compose Multiplatform (Material 3)
- **Networking**: Ktor Client
- **Async**: Coroutines & Flows
- **Dependency Injection**: Koin
- **Serialization**: Kotlinx.Serialization

---

## 4. How Development Works (The Workflow)

1. **Develop on Desktop (Fast Loop)**:
    - We build features primarily running the **Desktop Target**.
    - It compiles in seconds (JVM).
    - It supports Hot Reload (mostly).
    - No emulator overhead.

2. **Verify on Mobile (Triple Test)**:
    - Once the feature works on Desktop, we run `./scripts/run-android.sh` or `./scripts/run-ios.sh`.
    - 99% of the time, it just works.
    - We tweak specifics (Status bars, notched screens, touch targets) using "Expect/Actual" overrides only when necessary.

3. **Deploy**:
    - CI builds artifacts for all platforms from the same commit.

---

## 5. Related Documentation

- **[Cross Platform Type System](../reference/SHARED_TYPES_GUIDE.md)**: How data flows across boundaries.
- **[Design System](../reference/DESIGN_SYSTEM.md)**: The shared UI components.

---

## 6. Official Resources & Further Reading

To understand the "Standard" way versus our "Supercharged" way, review these official sources:

- **[Kotlin Multiplatform Official Docs](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)**: The baseline guide.
- **[Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)**: The UI framework.
- **"The Official Way" vs. B-Side**:
  - *Official*: Often suggests keeping ViewModels in `androidMain`/`iosMain` and sharing only UseCases.
  - *B-Side*: We promote ViewModels to `commonMain` to maximize code sharing (Logic + State + UI).
