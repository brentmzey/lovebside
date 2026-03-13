# 🎯 B-Side Startup Flowchart

## Visual Guide: What Starts When

```
┌─────────────────────────────────────────────────────────────┐
│                     JUST START                               │
│              (One Command to Rule Them All)                  │
└──────────────────┬──────────────────────────────────────────┘
                   │
                   ├─────────────┐
                   │             │
                   ▼             ▼
         ┌──────────────┐  ┌──────────────┐
         │   BACKEND    │  │   FRONTENDS  │
         └──────┬───────┘  └──────┬───────┘
                │                  │
                │                  ├── Web (port 8080)
                │                  ├── Desktop (native)
                │                  └── Android (if studio open)
                │
                ├── PocketBase (port 8092)
                ├── Ktor Server (port 8081)
                └── Nginx (port 8082)
```

---

## 🏗️ Detailed Startup Architecture

```
┌───────────────────────────────────────────────────────────────┐
│                         BACKEND STACK                          │
└───────────────────────────────────────────────────────────────┘

   ┌─────────────────────────────────────────────────────────┐
   │ docker-compose up --build                               │
   └──────────────────┬──────────────────────────────────────┘
                      │
       ┌──────────────┼──────────────┐
       │              │               │
       ▼              ▼               ▼
   ┌────────┐    ┌────────┐      ┌────────┐
   │PocketB.│    │ Ktor   │      │ Nginx  │
   │:8092   │◄───│ Server │◄─────│ :8082  │
   │        │    │ :8081  │      │        │
   └────────┘    └────────┘      └────────┘
       │
       │ SQLite DB
       ▼
   ┌──────────────┐
   │ pb_data/     │
   │ - data.db    │
   │ - logs/      │
   │ - storage/   │
   └──────────────┘

┌───────────────────────────────────────────────────────────────┐
│                         FRONTEND STACK                         │
└───────────────────────────────────────────────────────────────┘

   ┌─────────────────────────────────────────────────────────┐
   │ Gradle Multiplatform                                    │
   └──────────────────┬──────────────────────────────────────┘
                      │
       ┌──────────────┼──────────────┬──────────────┐
       │              │               │              │
       ▼              ▼               ▼              ▼
   ┌────────┐    ┌────────┐      ┌────────┐    ┌────────┐
   │  Web   │    │Desktop │      │Android │    │  iOS   │
   │  JS    │    │  JVM   │      │ APK    │    │ Swift  │
   │ :8080  │    │ Native │      │Emulator│    │Simulator
   └────────┘    └────────┘      └────────┘    └────────┘
       │              │               │              │
       └──────────────┴───────────────┴──────────────┘
                      │
                      ▼
              ┌───────────────┐
              │  Shared Core  │
              │  (:shared)    │
              │               │
              │ - Models      │
              │ - ViewModels  │
              │ - Repositories│
              │ - Domain      │
              └───────────────┘
```

---

## 🎬 Startup Sequence

### Phase 1: Backend Initialization (Docker)

```
1. docker-compose up
   ↓
2. Build Kotlin server JAR
   ↓
3. Start PocketBase container
   ├── Initialize SQLite DB
   ├── Apply migrations
   ├── Start HTTP server :8092
   └── Health check ready ✓
   ↓
4. Start Ktor Server container
   ├── Load config
   ├── Connect to PocketBase
   ├── Start HTTP server :8081
   └── Health check ready ✓
   ↓
5. Start Nginx container
   ├── Load config
   ├── Proxy rules active
   └── Ready on :8082 ✓
```

### Phase 2: Frontend Initialization

#### Web (JS)
```
1. ./gradlew :composeApp:jsBrowserDevelopmentRun
   ↓
2. Compile Kotlin → JS
   ↓
3. Start Webpack dev server
   ↓
4. Open browser → localhost:8080
   ↓
5. Hot reload active ✓
```

#### Desktop (JVM)
```
1. ./gradlew :composeApp:jvmRun
   ↓
2. Compile Kotlin → JVM bytecode
   ↓
3. Start JVM process
   ↓
4. Open native window ✓
```

#### Android
```
1. Open Android Studio
   ↓
2. Gradle sync
   ├── Download dependencies
   ├── Build :shared module
   ├── Build :composeApp
   └── Sync complete ✓
   ↓
3. Start emulator (if not running)
   ↓
4. Build APK
   ↓
5. Install APK → emulator
   ↓
6. Launch app ✓
```

#### iOS
```
1. Open Xcode
   ↓
2. Load iosApp.xcodeproj
   ↓
3. Resolve Swift Package Manager deps
   ↓
4. Build Kotlin framework
   ↓
5. Compile Swift code
   ↓
6. Start simulator
   ↓
7. Install app → simulator
   ↓
8. Launch app ✓
```

---

## 🔄 Process Flow Diagram

```
USER COMMAND
    │
    ▼
┌─────────────────┐
│  just start     │
└────────┬────────┘
         │
         ├──────► Check Prerequisites
         │         ├─ Docker running?
         │         ├─ Java installed?
         │         └─ Node.js installed?
         │
         ├──────► Start Backend
         │         ├─ Build server JAR
         │         ├─ docker-compose up -d
         │         └─ Wait for health checks
         │
         ├──────► Start Web
         │         └─ jsBrowserDevelopmentRun
         │
         ├──────► Start Desktop
         │         └─ jvmRun
         │
         └──────► Report Status
                   ├─ ✅ Backend ready
                   ├─ ✅ Web running
                   ├─ ✅ Desktop running
                   └─ 🔗 URLs displayed
```

---

## 🎯 Decision Tree: Which Mode?

```
                    START
                      │
          ┌───────────┴───────────┐
          │                       │
    Working on            Working on
    Backend?              Frontend?
          │                       │
          ▼                       ▼
    ┌──────────┐          ┌──────────┐
    │ Backend  │          │ Frontend │
    │   Mode   │          │   Mode   │
    └─────┬────┘          └────┬─────┘
          │                    │
    ┌─────┴─────┐        ┌─────┴─────┐
    │           │        │           │
    ▼           ▼        ▼           ▼
  Docker     Local    Docker     Android
  Full       Binary   Backend    Studio
  Stack      Mode     Only       +Emulator
    │           │        │           │
    ▼           ▼        ▼           ▼
 docker-    ./scripts/ docker-   Open AS
 compose    setup_dev  compose   Select
 up         _env.sh    up -d     device
            +server    +just     Run ▶️
                       web
```

---

## 📍 Port Map

| Port | Service | Purpose | Access |
|------|---------|---------|--------|
| 8090 | PocketBase (local) | Local dev DB | http://localhost:8090/_/ |
| 8092 | PocketBase (Docker) | Dockerized DB | http://localhost:8092/_/ |
| 8080 | Web Dev Server | Hot reload web | http://localhost:8080 |
| 8081 | Ktor Server | Backend API | http://localhost:8081 |
| 8082 | Nginx | Reverse proxy | http://localhost:8082 |

---

## 🧩 Dependency Graph

```
                  ┌──────────────┐
                  │   Backend    │
                  │  (Required)  │
                  └───────┬──────┘
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
    ┌─────────┐     ┌─────────┐     ┌─────────┐
    │   Web   │     │ Desktop │     │ Mobile  │
    │(optional)│     │(optional)│     │(optional)│
    └─────────┘     └─────────┘     └─────────┘
          │               │               │
          └───────────────┴───────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │  :shared     │
                  │  (Core)      │
                  └──────────────┘
```

**Key Insight:** Backend must run first. All frontends are independent.

---

## 🚦 Health Check Sequence

```
1. PocketBase
   curl http://localhost:8092/api/health
   Expected: {"code": 200}
   
2. Ktor Server
   curl http://localhost:8081/health  
   Expected: {"status": "UP"}
   
3. Web Dev Server
   curl http://localhost:8080
   Expected: HTML content
   
4. Desktop
   Process running: jvmRun
   Window visible: ✓
```

---

## 🔍 Common Startup Issues

```
Issue: Port already in use
├─ Symptom: "Address already in use :8090"
├─ Cause: PocketBase still running
└─ Fix: ./scripts/stop-all.sh

Issue: Gradle daemon died
├─ Symptom: "Could not connect to daemon"
├─ Cause: Out of memory
└─ Fix: ./gradlew --stop && ./gradlew clean

Issue: Docker won't start
├─ Symptom: "Cannot connect to Docker daemon"
├─ Cause: Docker Desktop not running
└─ Fix: Start Docker Desktop app

Issue: Android Studio stuck
├─ Symptom: "Gradle sync in progress..."
├─ Cause: Cache corruption
└─ Fix: Invalidate Caches & Restart
```

---

## 📈 Performance Notes

| Stage | Time (Cold) | Time (Warm) |
|-------|-------------|-------------|
| Backend startup | 30-60s | 5-10s |
| Web first compile | 60-90s | 10-15s |
| Desktop first run | 45-60s | 5-10s |
| Android first build | 3-5min | 30-60s |
| iOS first build | 2-4min | 30-45s |

**Cold:** First run after restart, no cache
**Warm:** Subsequent runs with Gradle cache

---

**Pro Tips:**
- Keep Docker running in background
- Use `just start` for everything at once
- Enable Gradle daemon (default)
- Preload emulators in background
- Use hot reload modes for dev

---

Return to: [QUICK_START_BACKEND.md](./QUICK_START_BACKEND.md)
