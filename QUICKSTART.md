# ⚡ Quick Start Guide

## For Emulator/Simulator Development

### 1. Start Backend

```bash
cd bside
just backend
```

Wait for:
```
✅ Backend services are running!
  PocketBase:      http://localhost:8092
  Ktor API:        http://localhost:8081
```

### 2. Run Your App

**Android (Android Studio):**
```bash
# Open project
open -a "Android Studio" .

# Or use:
just android-studio

# Then click Run ▶️
# App automatically connects to http://10.0.2.2:8092
```

**iOS (Xcode):**
```bash
# Open Xcode project
open iosApp/iosApp.xcodeproj

# Or use:
just ios

# Then click Run ▶️
# App automatically connects to http://localhost:8092
```

**Desktop:**
```bash
just desktop-hot  # With hot reload
```

**Web:**
```bash
just web  # Opens browser at localhost:8080
```

### 3. Done! 🎉

You're now running:
- ✅ Backend (PocketBase + Ktor) in Docker
- ✅ Your app connecting to it

---

## Network Configuration (Already Set Up)

| Platform | Backend URL | Notes |
|----------|-------------|-------|
| **Android Emulator** | `http://10.0.2.2:8092` | Special IP for host |
| **iOS Simulator** | `http://localhost:8092` | Uses host network |
| **Desktop** | `http://localhost:8092` | Direct localhost |
| **Web** | `http://localhost:8092` | Direct localhost |

**No configuration needed** - it just works! ✅

---

## Common Commands

```bash
# Backend
just backend      # Start backend services
just stop         # Stop everything
just restart      # Restart backend

# Database
just migrate      # Run DB migrations
open http://localhost:8092/_/  # Admin UI

# Logs
docker logs -f bside-pocketbase
docker logs -f bside-server

# Tests
./gradlew check   # All tests
```

---

## Troubleshooting

**Backend won't start:**
```bash
just stop
just backend
```

**Emulator can't connect:**
```bash
# Android: Use adb reverse
adb reverse tcp:8092 tcp:8092

# iOS: Should just work
```

**Build errors:**
```bash
./gradlew clean build
```

---

## What's Running?

```
┌─────────────────────────────────────┐
│                                     │
│  Your Computer (localhost)          │
│                                     │
│  ┌────────────┐   ┌─────────────┐ │
│  │ PocketBase │   │ Ktor Server │ │
│  │   :8092    │   │    :8081    │ │
│  └────────────┘   └─────────────┘ │
│         ▲                ▲         │
│         │                │         │
│         │                │         │
│  ┌──────┴───────┬────────┴──────┐ │
│  │              │               │ │
│  │   Android    │     iOS       │ │
│  │  Emulator    │  Simulator    │ │
│  │              │               │ │
│  │ 10.0.2.2:8092│ localhost:8092│ │
│  └──────────────┴───────────────┘ │
│                                     │
└─────────────────────────────────────┘
```

---

## Next Steps

- 📖 Read [Local Development Guide](docs/LOCAL_DEVELOPMENT.md)
- 🔑 Login to Admin UI: http://localhost:8092/_/
  - Email: `tester_admin@bside.love`
  - Password: `password123`
- 🧪 Run tests: `./gradlew check`
- 📱 Make changes and see hot reload!

---

**That's it! Just `just backend` + Run in IDE = Coding!** 🚀
