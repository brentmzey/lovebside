# Quick Start: Testing BSide Real-Time Features

## 🚀 Fast Track (5 minutes)

### 1. Start Backend
```bash
cd pocketbase && ./pocketbase serve
# Admin UI: http://localhost:8090/_/
```

### 2. Run Tests
```bash
# All tests
./gradlew :composeApp:allTests :shared:allTests :pocketbase-kt-sdk:check

# Or just the new reaction tests
./gradlew :composeApp:jvmTest --tests "*ChatViewModelTest"
```

### 3. Launch App
```bash
# Choose your platform:

# Desktop
./gradlew :composeApp:run

# Android (requires device/emulator)
./gradlew :composeApp:installDebug

# iOS (requires Xcode + Simulator)
open iosApp/iosApp.xcworkspace
# Then: Cmd+R to build and run

# Web
./gradlew :composeApp:jsBrowserDevelopmentRun
# Opens at: http://localhost:8080
```

## ✅ What to Test

### Core Flow
1. **Login** with two different accounts (use two devices/browsers)
2. **Start conversation** between the users
3. **Send messages** - verify they appear in real-time
4. **Add reaction** 👍 to a message (click/tap on message)
5. **Verify** other user sees reaction appear instantly
6. **Remove reaction** - verify removal propagates
7. **Check typing indicator** (type but don't send)
8. **Monitor online status** (disconnect one user)

### Verify in PocketBase Admin
1. Go to http://localhost:8090/_/
2. Collections > `m_messages`
3. See new messages appear in real-time
4. Collections > `m_reactions` (once implemented)
5. Check timestamps and relations

## 📸 Capture Evidence

### Screenshots
```bash
# Android
adb shell screencap -p /sdcard/screenshot.png
adb pull /sdcard/screenshot.png ./docs/screenshots/android/

# iOS Simulator
xcrun simctl io booted screenshot ./docs/screenshots/ios/chat.png

# Desktop/Web
# Use system screenshot tool (Cmd+Shift+4 on macOS)
```

### Video Recording
```bash
# Android
adb shell screenrecord /sdcard/demo.mp4
# ... perform test ...
# Ctrl+C, then: adb pull /sdcard/demo.mp4 ./docs/videos/android/

# iOS Simulator
xcrun simctl io booted recordVideo ./docs/videos/ios/demo.mov
# ... perform test ...
# Ctrl+C to stop

# Web (using browser DevTools)
# Chrome: F12 > Performance > Record
```

## 🐛 Quick Troubleshooting

### Tests Failing?
```bash
./gradlew clean
./gradlew :composeApp:jvmTest --info
```

### PocketBase Issues?
```bash
# Check if running
curl http://localhost:8090/api/health

# View logs
tail -f pocketbase.log

# Restart
pkill pocketbase && cd pocketbase && ./pocketbase serve
```

### Real-Time Not Working?
1. Check WebSocket connection in browser DevTools (Network tab)
2. Verify firewall allows port 8090
3. Check PocketBase logs for errors
4. Confirm user is authenticated

### Build Errors?
```bash
# Clean everything
./gradlew clean
rm -rf build/ */build/

# Rebuild
./gradlew build
```

## 📋 Test Checklist (Print This!)

- [ ] ✅ Backend started (PocketBase on :8090)
- [ ] ✅ Tests pass (`./gradlew check`)
- [ ] ✅ App launches on at least one platform
- [ ] ✅ Login with User A
- [ ] ✅ Login with User B (different device/browser)
- [ ] ✅ Send message A→B (appears in <500ms)
- [ ] ✅ Send message B→A (appears in <500ms)
- [ ] ✅ Add reaction from User B (appears for User A)
- [ ] ✅ Remove reaction (removal appears for User A)
- [ ] ✅ Typing indicator shows when typing
- [ ] ✅ Read receipt updates when message viewed
- [ ] ✅ Screenshot captured
- [ ] ✅ Video recorded (optional)
- [ ] ✅ PocketBase admin shows data correctly

## 🎯 Current Status

### ✅ Working
- Message send/receive (real-time)
- Threading/replies
- Read receipts
- Basic UI rendering

### 🔄 In Progress (Stubs Ready)
- **Reactions** - UI/ViewModel done, backend pending
- Typing indicators
- Online status

### 📅 Next Up
1. Create `m_reactions` collection in PocketBase
2. Implement PocketBase reaction logic
3. Add reaction UI components
4. Full integration testing

## 🔗 More Details

- Full Testing Guide: `docs/TESTING_GUIDE.md`
- Recent Changes: `docs/RECENT_CHANGES.md`
- Main README: `README.md`

---

**Last Updated:** 2026-01-24  
**Estimated Time:** 5-10 minutes  
**Difficulty:** ⭐⭐☆☆☆ (Easy)
