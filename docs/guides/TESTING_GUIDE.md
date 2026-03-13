# BSide Application Testing & Verification Guide

## Overview
This guide provides comprehensive instructions for testing the BSide application's real-time messaging features, including reactions, read receipts, typing status, online status, and media handling with PocketBase backend.

## Prerequisites

### Backend Setup
1. **PocketBase Instance**
   - Local: `./pocketbase/pocketbase serve` (port 8090)
   - Production: PocketHost deployment
   
2. **Database Schema**
   - Collections: `m_conversations`, `m_participants`, `m_messages`, `m_reactions`, `m_read_receipts`, `m_typing_status`
   - Indexes configured for performance
   - Real-time subscriptions enabled

### Development Environment
```bash
# Install dependencies
./gradlew build

# Verify tests pass
./gradlew :pocketbase-kt-sdk:check :composeApp:check :shared:check
```

## Testing Scope

### 1. Real-Time Messaging Core
**Features to Test:**
- ✅ Message send/receive
- ✅ Message threading (replies)
- ✅ Message deletion
- ✅ Read receipts
- 🔄 Reactions (implementation in progress)
- 🔄 Typing indicators
- 🔄 Online status

**Test Script:** `scripts/demo-realtime.sh`
```bash
# Start PocketBase backend
cd pocketbase && ./pocketbase serve

# In another terminal, run demo script
./scripts/demo-realtime.sh
```

### 2. Message Reactions Testing

#### Unit Tests
Location: `composeApp/src/commonTest/kotlin/love/bside/app/presentation/ChatViewModelTest.kt`

```kotlin
@Test
fun `toggleReaction calls repository`() = runTest {
    // Test adding reaction
    viewModel.toggleReaction(messageId, "👍")
    assertTrue(fakeRepository.addReactionCalled.contains(messageId to "👍"))
    
    // Test removing reaction
    viewModel.toggleReaction(messageId, "👍")
    assertTrue(fakeRepository.removeReactionCalled.contains(messageId to "👍"))
}
```

Run tests:
```bash
./gradlew :composeApp:allTests
```

#### Integration Testing
1. Start two client sessions (different users)
2. Send a message from User A
3. User B adds reaction 👍
4. Verify User A sees reaction in real-time
5. User B removes reaction
6. Verify removal propagates to User A

### 3. Platform-Specific UI Testing

#### Android
```bash
# Build and install debug APK
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk

# Launch app
adb shell am start -n love.bside.app/.MainActivity

# Capture screenshots
adb shell screencap -p /sdcard/screenshots/chat_screen.png
adb pull /sdcard/screenshots/chat_screen.png docs/screenshots/android/

# Record video
adb shell screenrecord /sdcard/demo.mp4
# ... perform actions ...
# Ctrl+C to stop
adb pull /sdcard/demo.mp4 docs/videos/android/
```

#### iOS
```bash
# Open Xcode project
open iosApp/iosApp.xcworkspace

# Build and run on simulator
xcodebuild -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  build

# Capture screenshot
xcrun simctl io booted screenshot docs/screenshots/ios/chat_screen.png

# Record video
xcrun simctl io booted recordVideo docs/videos/ios/demo.mov
# ... perform actions, then Ctrl+C to stop
```

#### Desktop (JVM)
```bash
# Run desktop app
./gradlew :composeApp:runDistributable

# Use system screenshot tools
# macOS: Cmd+Shift+4
# Linux: gnome-screenshot or scrot
# Windows: Snipping Tool or Win+Shift+S
```

#### Web (JS/Wasm)
```bash
# Build and serve web app
./gradlew :composeApp:jsBrowserDevelopmentRun

# Open in browser: http://localhost:8080

# Use browser DevTools for screenshots/recordings
# Or use Playwright for automated testing:
```

```javascript
// playwright-test.js
const { chromium } = require('playwright');

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('http://localhost:8080');
  
  // Navigate to chat
  await page.click('text=Messages');
  await page.waitForSelector('[data-testid="chat-screen"]');
  
  // Screenshot
  await page.screenshot({ path: 'docs/screenshots/web/chat_screen.png' });
  
  // Video recording
  const context = await browser.newContext({
    recordVideo: { dir: 'docs/videos/web/' }
  });
  
  await browser.close();
})();
```

### 4. Backend/Database Verification

#### PocketBase Admin UI
1. Access: `http://localhost:8090/_/`
2. Login with admin credentials
3. Navigate to Collections > m_messages
4. Verify:
   - Messages are created correctly
   - Timestamps are accurate
   - Relations (conversation_id, sender_id) are valid
   - Real-time updates appear instantly

#### Database Inspection
```bash
# Export data for verification
./pocketbase/pocketbase export --output ./db_export.zip

# SQL queries (if using SQLite directly)
sqlite3 ./pocketbase/pb_data/data.db

.schema m_messages
SELECT * FROM m_messages ORDER BY created DESC LIMIT 10;
SELECT * FROM m_reactions WHERE message_id = 'xxx';
```

#### Real-Time Subscription Testing
```kotlin
// Test real-time message subscription
val flow = repository.subscribeToConversation(conversationId)
flow.collect { message ->
    println("Received real-time message: ${message.content}")
    // Verify message appears within 500ms of send
}
```

### 5. Performance Testing

#### Message Throughput
```bash
# Send 100 messages rapidly
./scripts/performance_test_messages.sh

# Expected: <100ms latency per message
# Expected: No dropped messages
# Expected: UI remains responsive
```

#### Concurrent Users
```bash
# Simulate 10 concurrent users
./scripts/load_test_concurrent_users.sh

# Monitor PocketBase logs for 429 rate limit errors
tail -f pocketbase.log | grep "429"
```

#### Network Conditions
```bash
# Simulate slow network (iOS Simulator)
xcrun simctl status_bar booted override \
  --dataNetwork 3g \
  --wifiBars 2

# Test offline mode
# 1. Disconnect network
# 2. Send messages (should queue)
# 3. Reconnect network
# 4. Verify messages sync
```

### 6. Security Testing

#### Authentication
- ✅ Verify JWT tokens expire correctly
- ✅ Test refresh token flow
- ✅ Validate unauthorized access is blocked

#### Authorization
- ✅ Users can only see conversations they're in
- ✅ Users can only delete their own messages
- ✅ Admin-only endpoints are protected

#### Data Validation
- ✅ XSS prevention in message content
- ✅ File upload size limits enforced
- ✅ Media type validation

### 7. Visual Regression Testing

#### Setup
```bash
# Install dependencies
npm install --save-dev @playwright/test

# Run visual regression tests
npx playwright test --project=chromium
```

#### Capture Baselines
```bash
# Generate baseline screenshots for all platforms
./scripts/capture_visual_baselines.sh

# Baselines saved to: docs/screenshots/baselines/
```

#### Compare Changes
```bash
# After code changes, capture new screenshots
./scripts/capture_visual_regression.sh

# Compare with baselines
./scripts/compare_screenshots.sh

# Review diffs in: docs/screenshots/diffs/
```

## Test Checklist

### Core Messaging
- [ ] Send text message
- [ ] Send message with media (image, video, GIF)
- [ ] Reply to message (threading)
- [ ] Delete message
- [ ] Edit message (if supported)
- [ ] Mark as read
- [ ] View read receipts

### Real-Time Features
- [ ] Receive message instantly (<500ms)
- [ ] See typing indicator when other user types
- [ ] See online status when other user connects/disconnects
- [ ] Reactions update in real-time
- [ ] Read receipts update in real-time

### UI/UX
- [ ] Messages scroll smoothly
- [ ] Images/videos load and display correctly
- [ ] Emoji picker works
- [ ] Timestamp displays correctly
- [ ] User avatars load
- [ ] Unread badge updates

### Edge Cases
- [ ] Long messages (>1000 chars)
- [ ] Rapid message sending
- [ ] Network disconnect/reconnect
- [ ] App backgrounding/foregrounding
- [ ] Multiple devices same user
- [ ] Empty states (no messages, no conversations)

### Performance
- [ ] App launches in <3 seconds
- [ ] Messages render in <16ms (60fps)
- [ ] Memory usage stays <200MB
- [ ] Network usage reasonable (<1MB per 100 messages)

## Automated Testing

### CI/CD Integration
```yaml
# .github/workflows/test.yml
name: Test All Platforms

on: [push, pull_request]

jobs:
  test-android:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run Android Tests
        run: ./gradlew :composeApp:testDebugUnitTest
        
  test-ios:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run iOS Tests
        run: ./gradlew :composeApp:iosSimulatorArm64Test
        
  test-jvm:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run JVM Tests
        run: ./gradlew :composeApp:jvmTest
        
  test-js:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - name: Run JS Tests
        run: ./gradlew :composeApp:jsTest
```

### Continuous Monitoring
```bash
# Set up monitoring for production
# - Error tracking: Sentry
# - Performance monitoring: Firebase Performance
# - Real-time analytics: Mixpanel/Amplitude
```

## Documentation & Artifacts

### Screenshots
Save to: `docs/screenshots/{platform}/{feature}/`
Example:
- `docs/screenshots/android/chat/message_bubble.png`
- `docs/screenshots/ios/chat/reactions.png`
- `docs/screenshots/web/chat/typing_indicator.png`

### Videos
Save to: `docs/videos/{platform}/{feature}/`
Example:
- `docs/videos/android/realtime_messaging.mp4`
- `docs/videos/ios/reactions_flow.mov`

### Test Reports
Generated at: `build/reports/tests/`
- `pocketbase-kt-sdk/build/reports/tests/test/index.html`
- `composeApp/build/reports/tests/allTests/index.html`

## Troubleshooting

### Common Issues

**Issue: Real-time updates not working**
- Check PocketBase server is running
- Verify WebSocket connection in browser DevTools
- Check firewall/proxy settings

**Issue: Tests failing**
- Clean build: `./gradlew clean`
- Verify PocketBase test instance: `./scripts/start_test_pocketbase.sh`
- Check test logs: `build/reports/tests/test/index.html`

**Issue: Performance degradation**
- Profile with Android Studio Profiler
- Check for memory leaks
- Verify database indexes exist
- Monitor PocketBase logs for slow queries

## Next Steps

### Remaining Implementation
1. **Reactions Collection** - Create `m_reactions` in PocketBase
2. **Typing Indicators** - Implement ephemeral state tracking
3. **Online Status** - Add presence detection
4. **Media Handling** - Optimize image/video uploads
5. **Push Notifications** - Integrate FCM/APNS

### Schema Migrations
```sql
-- Create reactions table
CREATE TABLE m_reactions (
  id TEXT PRIMARY KEY,
  message_id TEXT NOT NULL,
  user_id TEXT NOT NULL,
  reaction TEXT NOT NULL,
  created TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (message_id) REFERENCES m_messages(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_reactions_message ON m_reactions(message_id);
CREATE INDEX idx_reactions_user ON m_reactions(user_id);
```

### Performance Optimization
- Add composite indexes for common queries
- Implement pagination for message lists
- Cache frequently accessed data
- Optimize image loading with thumbnails

## Resources

- PocketBase Docs: https://pocketbase.io/docs/
- Compose Multiplatform: https://www.jetbrains.com/lp/compose-multiplatform/
- Kotlin Coroutines Testing: https://kotlinlang.org/docs/coroutines-testing.html
- Playwright: https://playwright.dev/

---

**Last Updated:** 2026-01-24  
**Version:** 1.0.0  
**Maintainer:** BSide Team
