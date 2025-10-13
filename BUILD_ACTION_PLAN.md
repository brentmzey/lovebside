# Build & Integration Action Plan

## Current Status

### ✅ Completed
1. Enterprise-level error handling infrastructure (Result type, AppException)
2. Multi-platform logging system
3. Configuration management with feature flags
4. Network resilience with automatic retry
5. Input validation framework
6. Caching infrastructure
7. Professional UI components (Loading, Error, Empty states, Forms)
8. Complete Material Design 3 theme
9. PocketBase schema documentation
10. Updated AuthRepository with validation and retry
11. Updated ProfileRepository with caching
12. Enhanced LoginScreen with validation
13. Fixed LoginScreenComponent with proper state management
14. Updated ViewModels (Auth, Profile) to use new Result type

### ⚠️ In Progress
1. Fixing remaining compilation errors
2. Adding decompose-essenty dependencies
3. Updating all repositories to use new patterns

### ❌ Remaining Work

## Phase 1: Fix Compilation (Priority: CRITICAL)

### 1.1 Fix Decompose Imports
- [x] Add essenty-lifecycle-coroutines dependency
- [ ] Verify all decompose imports use correct package paths
- [ ] Fix MainScreen childStack.value references
- [ ] Test navigation between screens

### 1.2 Update Remaining ViewModels
- [ ] QuestionnaireViewModel - convert to use Result type
- [ ] ValuesViewModel - convert to use Result type
- [ ] MatchViewModel - convert to use Result type
- [ ] Add proper error handling to all ViewModels

### 1.3 Update All Repositories
Files to update:
- [ ] PocketBaseQuestionnaireRepository.kt
- [ ] PocketBaseValuesRepository.kt
- [ ] PocketBaseMatchRepository.kt

For each:
- Add retryable wrapper
- Add caching where appropriate
- Add logging
- Use Result type
- Add validation

### 1.4 Fix Component Issues
- [ ] Fix ProfileScreenComponent
- [ ] Fix QuestionnaireScreenComponent
- [ ] Fix ValuesScreenComponent
- [ ] Fix MatchScreenComponent
- [ ] Ensure all use coroutineScope() correctly

## Phase 2: Complete UI Layer (Priority: HIGH)

### 2.1 Update All Screens
Apply new patterns to:
- [ ] ProfileScreen.kt - Use LoadingView, ErrorView, proper state handling
- [ ] QuestionnaireScreen.kt - Add validation, error handling
- [ ] ValuesScreen.kt - Add validation, error handling
- [ ] MatchesScreen.kt - Add empty state, error handling
- [ ] PromptsScreen.kt - Add error handling

### 2.2 Screen Components
For each screen component:
- Use sealed class for UI state
- Implement onSuccess, onError, onLoading handlers
- Add clearError() method
- Use coroutineScope() properly

### 2.3 Navigation
- [ ] Fix RootComponent with proper routing
- [ ] Ensure proper navigation flow
- [ ] Add back navigation support
- [ ] Handle deep links (future)

## Phase 3: Complete Data Layer (Priority: HIGH)

### 3.1 Data Models
Ensure proper mapping:
- [ ] Verify all DTOs have toDomain() mappers
- [ ] Ensure all domain models are properly defined
- [ ] Add validation at model level where needed

### 3.2 API Integration
- [ ] Test all PocketBase endpoints
- [ ] Verify authentication flow
- [ ] Test CRUD operations on profiles
- [ ] Test values operations
- [ ] Test matches operations
- [ ] Test questionnaire operations

### 3.3 Error Mapping
- [ ] Map all PocketBase error codes to AppException types
- [ ] Handle 401/403 properly (session expired)
- [ ] Handle 404 (resource not found)
- [ ] Handle 429 (rate limiting)
- [ ] Handle 500-level errors

## Phase 4: Platform Builds (Priority: HIGH)

### 4.1 Android
```bash
./gradlew :shared:assembleDebug
./gradlew :composeApp:assembleDebug
```
- [ ] Fix any Android-specific compilation issues
- [ ] Test on Android emulator
- [ ] Verify logging works (Logcat)
- [ ] Test authentication flow
- [ ] Test all screens

### 4.2 iOS
```bash
./gradlew :shared:linkDebugFrameworkIosArm64
```
- [ ] Fix any iOS-specific compilation issues
- [ ] Test on iOS simulator
- [ ] Verify logging works (Console)
- [ ] Test authentication flow
- [ ] Test all screens

### 4.3 Desktop/JVM
```bash
./gradlew :composeApp:run
```
- [ ] Fix any JVM-specific compilation issues
- [ ] Test desktop app
- [ ] Verify debug menu works
- [ ] Test all features

### 4.4 Web (JS)
```bash
./gradlew :composeApp:jsBrowserDevelopmentRun
```
- [ ] Fix any JS-specific compilation issues
- [ ] Test in browser
- [ ] Verify console logging
- [ ] Test all features

### 4.5 Web (Wasm)
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```
- [ ] Verify ManualDI works correctly
- [ ] Test in modern browsers
- [ ] Test all features

## Phase 5: PocketBase Setup (Priority: CRITICAL)

### 5.1 Create Collections
Follow POCKETBASE_SCHEMA.md:
- [ ] Create `users` collection (auth)
- [ ] Create `s_profiles` collection
- [ ] Create `s_key_values` collection
- [ ] Create `s_user_values` collection
- [ ] Create `s_prompts` collection
- [ ] Create `s_user_answers` collection
- [ ] Create `s_matches` collection

### 5.2 Configure Permissions
For each collection:
- [ ] Set List rules
- [ ] Set View rules
- [ ] Set Create rules
- [ ] Set Update rules
- [ ] Set Delete rules
- [ ] Test permissions with test users

### 5.3 Seed Data
- [ ] Add key values (personality, values, interests, lifestyle)
- [ ] Add prompts (Proust questionnaire)
- [ ] Create test users
- [ ] Create test profiles
- [ ] Create test matches

### 5.4 Test API
- [ ] Test user registration
- [ ] Test login/logout
- [ ] Test profile CRUD
- [ ] Test values CRUD
- [ ] Test answers CRUD
- [ ] Test matches retrieval

## Phase 6: Integration Testing (Priority: HIGH)

### 6.1 End-to-End Flows
- [ ] User Registration Flow
  - Sign up
  - Create profile
  - Add values
  - Answer questionnaire
- [ ] Login Flow
  - Login with credentials
  - View profile
  - Navigate between screens
  - Logout
- [ ] Matching Flow
  - View matches
  - Check compatibility scores
  - Update match status

### 6.2 Error Scenarios
- [ ] Invalid credentials
- [ ] Network timeout
- [ ] Server error (500)
- [ ] Invalid input validation
- [ ] Session expiration
- [ ] Resource not found

### 6.3 Performance Testing
- [ ] Cache hit rates
- [ ] API response times
- [ ] Memory usage
- [ ] Battery impact (mobile)

## Phase 7: Polish & UX (Priority: MEDIUM)

### 7.1 Animations
- [ ] Screen transitions
- [ ] Loading animations
- [ ] Success animations
- [ ] Error shake animations

### 7.2 Accessibility
- [ ] Screen reader support
- [ ] Keyboard navigation
- [ ] High contrast support
- [ ] Font scaling

### 7.3 Offline Support
- [ ] Cache user data
- [ ] Queue operations
- [ ] Sync when online
- [ ] Show offline indicator

## Phase 8: Monitoring & Analytics (Priority: LOW)

### 8.1 Analytics
- [ ] User events tracking
- [ ] Screen views
- [ ] Error tracking
- [ ] Performance metrics

### 8.2 Crash Reporting
- [ ] Integrate crash reporting
- [ ] Test crash reporting
- [ ] Monitor crash rates

### 8.3 Logging
- [ ] Production logging strategy
- [ ] Log aggregation
- [ ] Alert on critical errors

## Quick Start Commands

### Build All Platforms
```bash
# Clean build
./gradlew clean

# Build shared module
./gradlew :shared:build

# Build Android
./gradlew :composeApp:assembleDebug

# Run Desktop
./gradlew :composeApp:run

# Run Web (JS)
./gradlew :composeApp:jsBrowserDevelopmentRun

# Run Web (Wasm)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Fix Current Build
```bash
# 1. Fix remaining compilation errors
./gradlew :shared:compileKotlinJvm --no-daemon 2>&1 | grep "^e:"

# 2. After fixes, test build
./gradlew :shared:jvmJar

# 3. Build full app
./gradlew :composeApp:build
```

### Test PocketBase Connection
```bash
# Test registration
curl -X POST https://bside.pockethost.io/api/collections/users/records \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Test1234!","passwordConfirm":"Test1234!"}'

# Test login
curl -X POST https://bside.pockethost.io/api/collections/users/auth-with-password \
  -H "Content-Type: application/json" \
  -d '{"identity":"test@example.com","password":"Test1234!"}'
```

## Dependencies to Add

```kotlin
// In shared/build.gradle.kts commonMain
implementation(libs.essenty.lifecycle)
implementation(libs.essenty.lifecycle.coroutines)

// Already added but verify:
implementation(libs.ktor.client.auth)
implementation(libs.ktor.client.logging)
implementation(compose.runtime)
implementation(compose.foundation)
implementation(compose.material3)
implementation(compose.ui)
```

## Files Needing Immediate Attention

### Critical (Build Blockers)
1. `/shared/src/commonMain/kotlin/love/bside/app/presentation/main/MainScreen.kt` - Fix childStack references
2. `/shared/src/commonMain/kotlin/love/bside/app/presentation/QuestionnaireViewModel.kt` - Update to Result type
3. `/shared/src/commonMain/kotlin/love/bside/app/presentation/ValuesViewModel.kt` - Update to Result type
4. `/shared/src/commonMain/kotlin/love/bside/app/presentation/MatchViewModel.kt` - Update to Result type
5. `/shared/src/commonMain/kotlin/love/bside/app/data/repository/PocketBaseQuestionnaireRepository.kt` - Add retry, logging
6. `/shared/src/commonMain/kotlin/love/bside/app/data/repository/PocketBaseValuesRepository.kt` - Add retry, logging
7. `/shared/src/commonMain/kotlin/love/bside/app/data/repository/PocketBaseMatchRepository.kt` - Add retry, logging

### High Priority (Core Features)
1. All Screen Components - Update state management
2. All Screens - Use new UI components
3. Domain Repository interfaces - Add Result types
4. Use Cases - Add logging

### Medium Priority (Enhancement)
1. Add more UI components as needed
2. Add more validators
3. Add analytics hooks
4. Add offline support

## Success Criteria

### Phase 1 Complete When:
- ✅ All platforms compile without errors
- ✅ Can run desktop app
- ✅ Can run web app
- ✅ Can build Android APK

### Phase 2 Complete When:
- ✅ All screens render correctly
- ✅ Navigation works between screens
- ✅ Error states display properly
- ✅ Loading states work

### Phase 3 Complete When:
- ✅ Can authenticate with PocketBase
- ✅ Can create/read/update profiles
- ✅ Can manage values
- ✅ Can answer questionnaire
- ✅ Can view matches

### Production Ready When:
- ✅ All tests passing
- ✅ Error handling comprehensive
- ✅ Performance acceptable
- ✅ Security audit passed
- ✅ Analytics integrated
- ✅ Crash reporting working
- ✅ Documentation complete

## Estimated Timeline

- Phase 1 (Fix Compilation): 2-4 hours
- Phase 2 (UI Layer): 4-6 hours
- Phase 3 (Data Layer): 3-4 hours
- Phase 4 (Platform Builds): 2-3 hours
- Phase 5 (PocketBase Setup): 2-3 hours
- Phase 6 (Integration Testing): 4-6 hours
- Phase 7 (Polish): 4-8 hours
- Phase 8 (Monitoring): 2-4 hours

**Total: 23-38 hours** (3-5 days with dedicated developer)

## Next Immediate Steps

1. Run: `./gradlew :shared:compileKotlinJvm --no-daemon` to see current errors
2. Fix each compilation error one by one
3. Update remaining ViewModels
4. Update remaining Repositories
5. Test build on at least one platform
6. Set up PocketBase collections
7. Test end-to-end authentication flow

---

**Last Updated**: January 2025
**Status**: Phase 1 In Progress
**Next Milestone**: Clean Build Across All Platforms
