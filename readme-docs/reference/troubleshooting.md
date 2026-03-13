---
title: "Troubleshooting"
excerpt: "Solutions to common issues and problems"
category: "reference"
slug: "troubleshooting"
order: 3
---

# Troubleshooting Guide

Common issues and their solutions when working with B-Side.

## Backend Issues

### Port Already in Use

**Error:**
```
Error starting userland proxy: listen tcp 0.0.0.0:8092: bind: address already in use
```

**Solution:**

[block:code]
{
  "codes": [
    {
      "code": "# Find process using the port\nlsof -i :8092\n\n# Kill the process\nkill $(lsof -t -i:8092)\n\n# Or stop all B-Side services first\njust stop",
      "language": "shell",
      "name": "macOS/Linux"
    },
    {
      "code": "# Find process using the port\nnetstat -ano | findstr :8092\n\n# Kill the process (replace PID)\ntaskkill /PID <PID> /F",
      "language": "shell",
      "name": "Windows"
    }
  ]
}
[/block]

### Docker Container Won't Start

**Error:**
```
Error response from daemon: driver failed programming external connectivity
```

**Solution:**

```bash
# Reset Docker
just stop
docker system prune -a

# Restart Docker Desktop
# Then try again
just backend
```

### PocketBase Migration Fails

**Error:**
```
failed to apply migration: UNIQUE constraint failed
```

**Solution:**

```bash
# Backup your data first!
docker exec bside-pocketbase ./pocketbase backup

# Reset database (⚠️ destroys data)
docker-compose down -v
just backend

# Or manually fix in admin UI
open http://localhost:8092/_/
```

## Build Issues

### Gradle Build Fails

**Error:**
```
Execution failed for task ':shared:compileKotlinJvm'
```

**Solution:**

```bash
# Clean and rebuild
./gradlew clean
./gradlew build --refresh-dependencies

# If still failing, clear Gradle cache
rm -rf ~/.gradle/caches/
./gradlew build
```

### Out of Memory Error

**Error:**
```
Expiring Daemon because JVM heap space is exhausted
```

**Solution:**

Add to `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### Kotlin Compiler Error

**Error:**
```
e: Expected ... has no actual declaration
```

**Solution:**

This means platform-specific implementation is missing. Check:

1. Does the `expect` declaration exist in `commonMain`?
2. Are there `actual` implementations in all platform source sets?
3. Are the signatures identical?

```kotlin
// commonMain
expect fun platformSpecific(): String

// androidMain, iosMain, jvmMain, jsMain - all need:
actual fun platformSpecific(): String = "..."
```

## Android Issues

### Emulator Can't Connect to Backend

**Error:**
```
java.net.ConnectException: Failed to connect to localhost:8092
```

**Solution:**

Android emulator needs special IP:

```kotlin
// Use 10.0.2.2 instead of localhost for emulator
val apiUrl = if (isEmulator) {
    "http://10.0.2.2:8092"
} else {
    "http://localhost:8092"
}
```

Or use ADB reverse:
```bash
adb reverse tcp:8092 tcp:8092
```

### Build Variant Not Found

**Error:**
```
Cannot find variant 'debug'
```

**Solution:**

```bash
# Sync Gradle
./gradlew :composeApp:assemble

# In Android Studio: File > Sync Project with Gradle Files
```

## iOS Issues

### CocoaPods Error

**Error:**
```
[!] Unable to find a specification for 'compose-ios'
```

**Solution:**

```bash
cd iosApp
pod install --repo-update

# If still failing
rm -rf Pods Podfile.lock
pod install
```

### Xcode Build Fails

**Error:**
```
Command PhaseScriptExecution failed with a nonzero exit code
```

**Solution:**

```bash
# Clean Xcode build
cd iosApp
xcodebuild clean

# Or in Xcode: Product > Clean Build Folder (Cmd+Shift+K)

# Rebuild framework
./gradlew :composeApp:embedAndSignAppleFrameworkForXcode
```

### Simulator Not Found

**Error:**
```
error: No devices are booted
```

**Solution:**

```bash
# List available simulators
xcrun simctl list devices

# Boot a simulator
xcrun simctl boot "iPhone 15 Pro"

# Or open Xcode and start simulator from UI
```

## Desktop Issues

### App Won't Launch

**Error:**
```
Error: Could not find or load main class
```

**Solution:**

```bash
# Clean and rebuild
./gradlew :composeApp:clean
./gradlew :composeApp:jvmRun
```

### Window Appears Blank

**Issue:** App launches but shows empty window

**Solution:**

Check Graphics drivers:
```bash
# macOS - may need to run with Rosetta on M1/M2
arch -x86_64 ./gradlew :composeApp:jvmRun

# Linux - install OpenGL
sudo apt-get install libgl1-mesa-dev
```

## Web Issues

### Webpack Build Fails

**Error:**
```
Module not found: Error: Can't resolve 'kotlin'
```

**Solution:**

```bash
# Clear npm/yarn cache
rm -rf kotlin-js-store node_modules
npm clean-install

# Rebuild
./gradlew :composeApp:jsBrowserDevelopmentWebpack
```

### CORS Error in Browser

**Error:**
```
Access to fetch blocked by CORS policy
```

**Solution:**

For development, backend should allow CORS. Check `server/src/main/kotlin/Application.kt`:

```kotlin
install(CORS) {
    anyHost() // Development only!
    allowCredentials = true
    allowHeader(HttpHeaders.ContentType)
}
```

For production, configure proper origins.

## Network Issues

### SSE Connection Fails

**Error:**
```
EventSource failed: network error
```

**Solution:**

1. Check backend is running: `curl http://localhost:8092/api/health`
2. Check firewall settings
3. Verify authentication token is valid
4. Check browser console for detailed error

### API Requests Timeout

**Error:**
```
java.net.SocketTimeoutException: Read timed out
```

**Solution:**

Increase timeout in HTTP client:

```kotlin
val client = HttpClient {
    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 10_000
    }
}
```

## Database Issues

### Data Not Syncing

**Issue:** Changes don't appear in real-time

**Solution:**

1. Check SSE connection is active
2. Verify user has permissions
3. Check PocketBase logs: `docker logs -f bside-pocketbase`
4. Test with admin panel: http://localhost:8092/_/

### Query Performance Slow

**Issue:** List queries take too long

**Solution:**

Add indexes in PocketBase admin:

1. Open http://localhost:8092/_/
2. Go to Collections
3. Add indexes on frequently queried fields
4. Common indexes: `created`, `updated`, `author`, `conversation`

## Cache Issues

### Stale Data Displayed

**Issue:** Old data shown after update

**Solution:**

Clear cache:

```kotlin
// In app code
cacheManager.clear()

// Or restart app
just stop
just backend
```

### Redis Connection Error

**Error:**
```
Error connecting to Redis: Connection refused
```

**Solution:**

```bash
# Check Redis is running
docker ps | grep redis

# Restart Redis
docker restart bside-redis

# Check logs
docker logs bside-redis
```

## Performance Issues

### App Feels Sluggish

**Solution:**

1. **Enable ProGuard/R8** (Release builds)
2. **Reduce image sizes**
3. **Implement pagination**
4. **Use lazy loading**
5. **Profile with tools:**

```bash
# Android
./gradlew :composeApp:assembleRelease --profile

# Desktop
./gradlew :composeApp:jvmRun -Dcompose.desktop.verbose=true
```

### Memory Leak Suspected

**Solution:**

Use platform profilers:
- **Android**: Android Studio Profiler
- **iOS**: Xcode Instruments
- **Desktop**: VisualVM, JProfiler

Common causes:
- Unclosed EventSource connections
- Retained ViewModel references
- Large image caches

## Testing Issues

### Tests Fail on CI

**Error:**
```
Tests failed: expected <5> but was <4>
```

**Solution:**

1. Check test isolation - tests might share state
2. Use `runTest` for coroutine tests
3. Mock time-sensitive operations
4. Clear database between tests

```kotlin
@BeforeTest
fun setup() {
    // Reset state
    repository.clear()
}
```

## Getting More Help

### Check Logs

```bash
# Backend logs
docker logs -f bside-pocketbase
docker logs -f bside-server

# App logs
./gradlew :composeApp:jvmRun --info
```

### Enable Debug Logging

```kotlin
// In shared/src/commonMain/kotlin/App.kt
Logger.setLevel(LogLevel.DEBUG)
```

### Common Log Locations

| Platform | Log Location |
|----------|-------------|
| **Android** | Logcat in Android Studio |
| **iOS** | Xcode Console |
| **Desktop** | Terminal output |
| **Web** | Browser DevTools Console |

### Report a Bug

If you can't solve the issue:

1. Collect information:
   - Error message
   - Stack trace
   - Steps to reproduce
   - Environment (OS, version, etc.)

2. Search existing issues: [GitHub Issues](https://github.com/your-org/bside/issues)

3. Create new issue with template

---

> 💡 Quick Fixes
> 
> 90% of issues are resolved by:
> 1. `just stop && just backend`
> 2. `./gradlew clean build`
> 3. Clear caches and restart IDE

> 📘 Still Stuck?
> 
> Join our [Discussions](https://github.com/your-org/bside/discussions) or [Discord](https://discord.gg/bside) for community help.
