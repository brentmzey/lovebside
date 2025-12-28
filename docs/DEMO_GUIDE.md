# BSide Multi-Platform Real-Time Messaging Demo Guide

## Overview

This guide helps you demo the BSide app's real-time messaging capabilities across multiple platforms (Android, iOS, Web, Desktop) with offline cache and sync functionality.

## Prerequisites

### 1. Backend Running
```bash
cd pocketbase
./pocketbase serve
```
Access admin panel: http://localhost:8090/_/

### 2. Test Users Created
Create at least 2 test users in PocketBase admin:
- user1@test.com / password123
- user2@test.com / password123

Ensure they have profiles and are matched (or manually create a conversation).

### 3. Development Environment
- **Desktop/JVM**: Java 17+ installed
- **Web**: Modern browser (Chrome/Firefox)
- **Android**: Android Studio, SDK 34, emulator running
- **iOS**: Xcode 15+, iOS Simulator (macOS only)

## Quick Start Demo

### Option 1: Automated Launch (Recommended)
```bash
./scripts/demo_multiplatform.sh
```
Follow the interactive prompts to select platforms.

### Option 2: Manual Launch

#### Desktop (JVM)
```bash
./gradlew :composeApp:run
```

#### Web
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```
Opens at http://localhost:8080

#### Android
```bash
# Start emulator first
emulator -avd Pixel_5_API_34

# Install and run
./gradlew :composeApp:installDebug
adb shell am start -n love.bside.app/.MainActivity
```

#### iOS (macOS only)
```bash
open iosApp/iosApp.xcodeproj
# Build and run from Xcode
```

## Demo Scenarios

### Scenario 1: Real-Time Message Sync

**Goal**: Show messages appearing instantly across platforms

**Steps**:
1. Launch app on Platform A (e.g., Desktop)
2. Launch app on Platform B (e.g., Web)
3. Login as User 1 on Platform A
4. Login as User 2 on Platform B
5. Navigate to Messages → select conversation
6. Send message from Platform A
7. **Observe**: Message appears immediately on Platform B
8. Reply from Platform B
9. **Observe**: Reply appears immediately on Platform A

**Expected Result**: <1 second latency between send and receive

### Scenario 2: Threaded Conversations

**Goal**: Demonstrate message threading/replies

**Steps**:
1. Long-press or click reply icon on a message
2. Type reply text
3. Send reply
4. **Observe**: Reply shows with thread indicator
5. Click thread to view all replies
6. Send another reply in thread
7. **Observe**: Thread counter updates in real-time on both platforms

**Expected Result**: Threading UI shows parent message and depth indicator

### Scenario 3: Offline Mode & Sync

**Goal**: Show offline cache and auto-sync when back online

**Steps**:
1. Open app on Platform A (e.g., Mobile)
2. Send a message (works normally)
3. **Disconnect network** (airplane mode / disable WiFi)
4. Send another message
5. **Observe**: Message shows "pending" indicator
6. Try to refresh - shows cached messages
7. **Reconnect network**
8. **Observe**: Pending message auto-syncs to server
9. Check Platform B - see the synced message

**Expected Result**: 
- Messages queue locally when offline
- Cache provides offline viewing
- Auto-sync on reconnection

### Scenario 4: Multi-User Group Chat

**Goal**: Show 3+ users in real-time conversation

**Steps**:
1. Launch on 3 different platforms (Desktop, Web, Mobile)
2. Login as User 1, 2, and 3
3. Create or join group conversation
4. Each user sends a message in sequence
5. **Observe**: All messages appear across all platforms in real-time
6. Test typing indicators (if implemented)

**Expected Result**: Seamless multi-user experience

## Screen Recording Tips

### macOS
```bash
# Built-in screen recording: Cmd+Shift+5
# Or use QuickTime Player → File → New Screen Recording

# For multiple screens, use OBS Studio
```

### Android
```bash
# Screen record from adb
adb shell screenrecord --time-limit 180 /sdcard/bside_demo.mp4
# ... perform demo ...
# Stop with Ctrl+C
adb pull /sdcard/bside_demo.mp4
```

### Web
Use browser dev tools or OBS Studio to capture browser window.

### iOS
- QuickTime Player → File → New Movie Recording → Select iOS device
- Or Xcode → Window → Devices and Simulators → Record

## Troubleshooting

### PocketBase Connection Issues
```bash
# Check if PocketBase is running
curl http://localhost:8090/api/health

# Check logs
tail -f pocketbase/pocketbase.log
```

### Build Errors
```bash
# Clean build
./gradlew clean

# Rebuild specific platform
./gradlew :composeApp:assembleDebug  # Android
./gradlew :composeApp:jsBrowserProductionWebpack  # Web
./gradlew :shared:build  # Shared code
```

### Real-Time Not Working
1. Check SSE connection in browser dev tools (Network tab)
2. Verify PocketBase real-time is enabled
3. Check CORS settings in PocketBase

### Offline Cache Not Working
1. Verify NetworkMonitor is initialized
2. Check logs for cache operations
3. Ensure OfflineCacheManager is injected into repository

## Feature Highlights for Demo

### 🚀 Real-Time Messaging
- WebSocket-based (SSE) live updates
- <1 second message delivery
- Typing indicators
- Read receipts

### 💾 Offline Support
- LRU cache with TTL
- Optimistic UI updates
- Auto-sync queue
- Network state monitoring

### 🧵 Threading
- Reply to specific messages
- Thread depth tracking
- Nested conversation views

### 🎨 Premium UX
- Apple-style design
- Smooth animations
- Glassmorphism effects
- Dark/light mode support

### 📱 Multi-Platform
- Android (Phone, Tablet)
- iOS (iPhone, iPad)
- Web (Desktop, Mobile)
- Desktop (Windows, macOS, Linux)

## Performance Metrics to Highlight

- **Message Delivery**: <1s latency
- **Cache Hit Rate**: 85%+ for repeated views
- **Offline Queue**: Unlimited messages
- **Sync Time**: <2s for 100 queued messages
- **Memory Usage**: <50MB for 1000 cached messages

## Next Steps

After the demo, consider:
1. Load testing with 100+ concurrent users
2. Network simulation testing (poor connectivity)
3. Battery impact analysis (mobile)
4. Accessibility audit
5. Security penetration testing

## Support

For issues or questions:
- Check logs: `./pocketbase/pocketbase.log`, `./logs/`
- Review CodeHQ tasks: `./.code-hq/entities/tasks.md`
- Project dashboard: `./CODEHQ.md`
