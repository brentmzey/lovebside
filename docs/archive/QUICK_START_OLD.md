# Quick Start Guide 🚀

**Get developing in 5 minutes!**

---

## Option 1: Simple Start (Recommended for First Time)

### Step 1: Start Backend
```bash
./start-all.sh
```

You should see:
```
✨ All B-Side services are running successfully! ✨
�� PocketBase: http://127.0.0.1:8090
🌐 Internal API: http://localhost:8080
```

### Step 2: Run Your Platform

**Android:**
```bash
./debug-platform.sh android
```

**iOS:**
```bash
./debug-platform.sh ios
# or
open iosApp/iosApp.xcodeproj
```

**Desktop:**
```bash
./debug-platform.sh desktop
```

**Web:**
```bash
./debug-platform.sh web
# Then open http://localhost:8080 in browser
```

### Step 3: Monitor Logs (Optional)
In a new terminal:
```bash
./watch-logs.sh
```

---

## Option 2: Development Mode (For Active Development)

This mode auto-reloads when you make changes!

### Backend Development
```bash
./dev-loop.sh backend
```
- Server restarts automatically when you save files
- Perfect for API development

### Frontend Development
```bash
# Pick your platform
./dev-loop.sh frontend android
./dev-loop.sh frontend ios
./dev-loop.sh frontend desktop
./dev-loop.sh frontend web
```
- Hot reload on file save (web/desktop)
- Quick rebuild (Android/iOS)

### Full-Stack Development
```bash
# Backend + Frontend together
./dev-loop.sh fullstack android
./dev-loop.sh fullstack desktop
```
- Unified log output
- Both backend and frontend running
- See all changes in real-time

---

## Your First Code Change

### 1. Backend Change Example

**Edit:** `server/src/main/kotlin/love/bside/server/routes/api/v1/UserRoutes.kt`

Add a new endpoint:
```kotlin
get("/user/test") {
    call.respond(HttpStatusCode.OK, mapOf("message" to "Hello from backend!"))
}
```

**Test immediately:**
```bash
curl http://localhost:8080/api/v1/user/test
```

If using `./dev-loop.sh backend`, server auto-restarts!

### 2. Frontend Change Example

**Edit:** `composeApp/src/commonMain/kotlin/love/bside/app/ui/screens/HomeScreen.kt`

Add some UI:
```kotlin
@Composable
fun HomeScreen() {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Welcome to B-Side!",
            style = MaterialTheme.typography.headlineLarge
        )
        
        Button(onClick = { /* TODO */ }) {
            Text("Get Started")
        }
    }
}
```

**See changes:**
- Web/Desktop: Save file, app reloads automatically
- Android: Run `./gradlew :composeApp:installDebug`
- iOS: Click Run in Xcode

---

## Typical Development Session

### Morning Routine
```bash
# 1. Start backend
./dev-loop.sh backend

# 2. In another terminal, run frontend
./dev-loop.sh frontend android

# 3. In another terminal, watch logs (optional)
./watch-logs.sh
```

### Make Changes
```bash
# Edit files in your IDE
# - Backend changes: Server auto-restarts
# - Frontend changes: Rebuild app
```

### Test Changes
```bash
# Test backend
curl http://localhost:8080/api/v1/endpoint

# Test frontend
# Use the app, check logs

# Run automated tests
./gradlew test
```

### End of Day
```bash
# Ctrl+C in all terminals
# or
./stop-all.sh
```

---

## Common Tasks

### Check if Services are Running
```bash
curl http://localhost:8080/health        # Server
curl http://127.0.0.1:8090/api/health   # PocketBase
```

### View Logs
```bash
# All logs in split view
./watch-logs.sh

# Individual logs
tail -f logs/server.log
tail -f logs/pocketbase.log

# Android
adb logcat -s "B-Side:*"
```

### Clean Build
```bash
./gradlew clean build
```

### Reset Database
```bash
cd pocketbase
mv pb_data pb_data.backup
./pocketbase serve
# Access admin UI: http://127.0.0.1:8090/_/
```

---

## Troubleshooting

### "Port already in use"
```bash
./stop-all.sh
# If that doesn't work:
lsof -ti:8080 | xargs kill -9
lsof -ti:8090 | xargs kill -9
```

### "Build failed"
```bash
./gradlew clean
./gradlew --stop
./gradlew build
```

### "Can't connect to server"
```bash
# Check if running
curl http://localhost:8080/health

# Check logs
tail -f logs/server.log

# Restart
./stop-all.sh
./start-all.sh
```

### "Android device not found"
```bash
# List devices
adb devices

# Restart adb
adb kill-server
adb start-server

# Start emulator
emulator -list-avds
emulator -avd Pixel_5_API_33
```

---

## Next Steps

1. **Read the docs:**
   - [DEVELOPMENT_GUIDE.md](DEVELOPMENT_GUIDE.md) - Full development guide
   - [ITERATIVE_DEVELOPMENT.md](ITERATIVE_DEVELOPMENT.md) - Development workflows
   - [CHEAT_SHEET.md](CHEAT_SHEET.md) - Quick command reference

2. **Explore the code:**
   - `server/` - Backend API
   - `shared/` - Shared business logic
   - `composeApp/` - UI code
   - `pocketbase-kt-sdk/` - Database SDK

3. **Try making changes:**
   - Add a new API endpoint
   - Create a new UI screen
   - Update the database schema

4. **Test on all platforms:**
   ```bash
   ./dev-loop.sh frontend android
   ./dev-loop.sh frontend ios
   ./dev-loop.sh frontend desktop
   ./dev-loop.sh frontend web
   ```

---

## Quick Reference

| I want to... | Command |
|--------------|---------|
| Start developing | `./dev-loop.sh backend` then `./dev-loop.sh frontend android` |
| Just run the app | `./start-all.sh` then `./debug-platform.sh android` |
| See logs | `./watch-logs.sh` |
| Test my code | `./gradlew test` |
| Build for production | See [QUICK_VERIFICATION.md](QUICK_VERIFICATION.md) |
| Fix a bug | See [ITERATIVE_DEVELOPMENT.md](ITERATIVE_DEVELOPMENT.md) |
| Add a feature | See [ITERATIVE_DEVELOPMENT.md](ITERATIVE_DEVELOPMENT.md) |

---

**Happy coding! 🎉**

For detailed help, see [README_DOCS.md](README_DOCS.md) for the full documentation index.
