# Bside App - Complete Implementation Roadmap
## Status: October 19, 2025

## 📍 Current State

### ✅ Completed Infrastructure
- **Kotlin Multiplatform**: Shared code for Android/iOS/Desktop/Web
- **Compose Multiplatform**: UI framework across all platforms
- **Decompose Navigation**: Type-safe routing and state management
- **PocketBase Backend**: REST API + Realtime subscriptions
- **Database Schema**: Users, profiles, messages, conversations
- **Domain Models**: Complete data layer with validation
- **Material 3 Theme**: Brand colors (Pink/Purple/Orange) with dark mode
- **Core UI Components**: MessageBubble, ConversationListItem
- **Basic Auth Flow**: Login → Main screen navigation

### 🚧 In Progress
- **Bottom Navigation**: 4-tab layout (Messages/Discover/Profile/Settings)
- **Tab Screens**: Individual screen implementations
- **Navigation Wiring**: Connect all screens with proper state management

### ❌ Not Started
- **Branding & UX Polish**: Consistent visual identity
- **Matching Algorithms**: Proust questionnaire + affinity scoring
- **Backend Jobs**: Automated matching pipeline
- **Performance & Scale**: Monitoring, telemetry, optimization
- **API Resilience**: Retry logic, backoff, rate limiting

## 🎯 Implementation Phases

---

## PHASE 1: Core UI & Navigation (CURRENT - Week 1)

### Objectives
Complete the multiplatform UI with polished navigation and UX feel.

### Tasks
1. **Bottom Navigation Bar** ⏳
   - [ ] Create BottomNavBar component with Material 3 NavigationBar
   - [ ] 4 tabs: Messages, Discover, Profile, Settings
   - [ ] Icons and labels with active state
   - [ ] Integrate with MainScreen

2. **Messages Tab** ⏳
   - [ ] ConversationsListScreen - list all conversations
   - [ ] ChatDetailScreen - message thread with other user
   - [ ] Use existing MessageBubble and ConversationListItem
   - [ ] Pull-to-refresh, loading states
   - [ ] Real-time message updates (PocketBase subscriptions)

3. **Discover Tab** ⏳
   - [ ] UserGridScreen - browse potential matches
   - [ ] UserProfileScreen - view other user's profile
   - [ ] Filter/sort options
   - [ ] Lazy loading with pagination
   - [ ] Match score display (when matching is implemented)

4. **Profile Tab** ⏳
   - [ ] OwnProfileScreen - view own profile
   - [ ] EditProfileScreen - edit bio, photos, preferences
   - [ ] ProfilePhotoUpload component
   - [ ] Save/cancel with validation

5. **Settings Tab** ⏳
   - [ ] SettingsScreen - preferences, account, logout
   - [ ] Theme toggle (light/dark)
   - [ ] Notification settings
   - [ ] Account management
   - [ ] Logout with confirmation

### Files to Create/Update
```
composeApp/src/commonMain/kotlin/love/bside/app/
├── ui/
│   ├── components/
│   │   ├── BottomNavBar.kt          [CREATE]
│   │   ├── LoadingIndicator.kt      [CREATE]
│   │   ├── EmptyState.kt            [CREATE]
│   │   ├── ErrorState.kt            [CREATE]
│   │   └── ProfileCard.kt           [CREATE]
│   └── screens/
│       ├── messages/
│       │   ├── ConversationsListScreen.kt    [CREATE]
│       │   └── ChatDetailScreen.kt           [CREATE]
│       ├── discover/
│       │   ├── UserGridScreen.kt             [CREATE]
│       │   └── UserProfileScreen.kt          [CREATE]
│       ├── profile/
│       │   ├── OwnProfileScreen.kt           [CREATE]
│       │   └── EditProfileScreen.kt          [CREATE]
│       └── settings/
│           └── SettingsScreen.kt             [CREATE]
└── presentation/
    └── main/
        └── MainScreen.kt                     [UPDATE]
```

### Testing
- [ ] Desktop app launches and navigates
- [ ] All tabs accessible via bottom nav
- [ ] Smooth transitions between screens
- [ ] Loading states visible
- [ ] Error handling works

---

## PHASE 2: Branding & UX Polish (Week 2)

### Objectives
Create a cohesive, beautiful, professional visual identity and user experience.

### Brand Identity

#### Logo Design
- [ ] Create vector logo (SVG) with bside branding
- [ ] App icon for all platforms (1024x1024, various sizes)
- [ ] Splash screen design
- [ ] Favicon for web

#### Color System Enhancement
Current colors (Pink/Purple/Orange) are good foundation, but need:
- [ ] Semantic color mapping (success, warning, error, info)
- [ ] Gradient definitions for premium features
- [ ] Surface elevation colors (cards, overlays, modals)
- [ ] Accessibility audit (WCAG AAA compliance)
- [ ] High contrast mode support

#### Typography System
- [ ] Define font families (primary/secondary)
- [ ] Font weights for hierarchy (Regular/Medium/SemiBold/Bold)
- [ ] Line heights and letter spacing
- [ ] Responsive text sizes
- [ ] Import custom fonts (Google Fonts or custom)

#### Visual Components
- [ ] Illustration set for empty states
- [ ] Icon set (custom or Material Symbols)
- [ ] Loading animations (Lottie or custom)
- [ ] Success/error animations
- [ ] Profile placeholder images

### UX Enhancements

#### Micro-interactions
- [ ] Button press states with scale/haptic
- [ ] Swipe gestures (delete conversation, etc.)
- [ ] Pull-to-refresh with physics
- [ ] Smooth scroll to top
- [ ] Long-press menus

#### Navigation Patterns
- [ ] Consistent back navigation across all screens
- [ ] Breadcrumb trails for deep navigation
- [ ] Modal bottom sheets for actions
- [ ] Full-screen overlays for important flows
- [ ] Deep linking support

#### Feedback Mechanisms
- [ ] Toast notifications for actions
- [ ] Snackbars with undo actions
- [ ] Progress indicators (determinate/indeterminate)
- [ ] Success confirmations
- [ ] Error recovery suggestions

#### Content States
- [ ] Skeleton screens during loading
- [ ] Empty states with illustrations and CTAs
- [ ] Error states with retry actions
- [ ] Offline mode indicators
- [ ] Sync status indicators

### Files to Create/Update
```
composeApp/src/commonMain/
├── resources/
│   ├── images/
│   │   ├── logo.svg
│   │   ├── icon.png (various sizes)
│   │   └── illustrations/
│   ├── fonts/
│   │   └── (custom fonts if any)
│   └── animations/
│       └── (Lottie JSON files)
└── kotlin/love/bside/app/ui/
    ├── theme/
    │   ├── Color.kt              [UPDATE - semantic colors]
    │   ├── Type.kt               [UPDATE - font system]
    │   ├── Theme.kt              [UPDATE - complete theme]
    │   └── Icons.kt              [CREATE - custom icons]
    ├── animations/
    │   ├── Transitions.kt        [CREATE]
    │   └── SpringSpecs.kt        [CREATE]
    └── components/
        ├── BsideButton.kt        [CREATE - branded button]
        ├── BsideCard.kt          [CREATE - branded card]
        ├── BsideTextField.kt     [CREATE - branded input]
        └── BsideTopBar.kt        [CREATE - branded app bar]
```

### Design System Documentation
- [ ] Create design tokens JSON
- [ ] Document component variants
- [ ] Create Figma/design file (optional)
- [ ] Style guide PDF

---

## PHASE 3: Backend Matching System (Week 3-4)

### Objectives
Implement the Proust questionnaire, affinity scoring algorithms, and automated matching.

### Database Schema Extensions

#### New Collections
```sql
-- Questionnaire responses
CREATE TABLE proust_answers (
    id VARCHAR PRIMARY KEY,
    user_id VARCHAR REFERENCES users(id),
    question_id VARCHAR,
    answer_text TEXT,
    answer_numeric INTEGER,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Match results
CREATE TABLE matches (
    id VARCHAR PRIMARY KEY,
    user_a_id VARCHAR REFERENCES users(id),
    user_b_id VARCHAR REFERENCES users(id),
    affinity_score FLOAT,
    match_type VARCHAR, -- 'friendship', 'romantic', 'both'
    shared_values JSONB,
    compatibility_breakdown JSONB,
    created_at TIMESTAMP,
    viewed_by_a BOOLEAN DEFAULT FALSE,
    viewed_by_b BOOLEAN DEFAULT FALSE,
    status VARCHAR -- 'pending', 'mutual_interest', 'dismissed'
);

-- Match calculations metadata
CREATE TABLE match_runs (
    id VARCHAR PRIMARY KEY,
    run_at TIMESTAMP,
    users_processed INTEGER,
    matches_created INTEGER,
    avg_processing_time_ms INTEGER,
    status VARCHAR -- 'running', 'completed', 'failed'
);
```

### Proust Questionnaire System

#### Questions Database
- [ ] Define 30-50 Proust-style questions
- [ ] Categorize by dimension (values, preferences, personality)
- [ ] Multiple choice + open-ended formats
- [ ] Weighted importance per question

#### UI Components
- [ ] Questionnaire onboarding flow
- [ ] Progress indicator (X/50 questions)
- [ ] Question card with smooth transitions
- [ ] Skip/save for later option
- [ ] Review answers before submit

### Matching Algorithm

#### Affinity Score Calculation
```kotlin
// Core algorithm pseudocode
fun calculateAffinityScore(userA: User, userB: User): MatchScore {
    // 1. Proust answer similarity (40% weight)
    val proustScore = compareProustAnswers(userA, userB)
    
    // 2. Shared values/interests (30% weight)
    val valuesScore = compareValues(userA, userB)
    
    // 3. Demographic compatibility (20% weight)
    val demoScore = compareDemographics(userA, userB)
    
    // 4. Behavioral signals (10% weight)
    val behaviorScore = compareBehavior(userA, userB)
    
    return MatchScore(
        overall = weighted_average(proustScore, valuesScore, demoScore, behaviorScore),
        breakdown = mapOf(
            "intellectual" to proustScore,
            "values" to valuesScore,
            "lifestyle" to demoScore,
            "chemistry" to behaviorScore
        ),
        matchType = determineMatchType(userA, userB) // friendship/romantic/both
    )
}
```

#### Matching Dimensions
1. **Intellectual Compatibility**
   - Philosophical alignment
   - Cultural interests
   - Conversation topics
   
2. **Values Alignment**
   - Life priorities
   - Ethical stances
   - Long-term goals
   
3. **Lifestyle Compatibility**
   - Social preferences
   - Activity levels
   - Routine alignment
   
4. **Emotional Chemistry**
   - Communication style
   - Emotional expression
   - Conflict resolution

### Backend Job Implementation

#### Job Scheduler
- [ ] Implement cron job system (use Kotlin coroutines + scheduler)
- [ ] Run daily at off-peak hours (3 AM)
- [ ] Process users in batches (100 at a time)
- [ ] Graceful shutdown on errors

#### Matching Pipeline
```kotlin
// server/src/main/kotlin/jobs/MatchingJob.kt
suspend fun runMatchingJob() {
    val runId = createMatchRun()
    
    try {
        val users = getUsersNeedingMatches()
        val batches = users.chunked(100)
        
        batches.forEachIndexed { index, batch ->
            logger.info("Processing batch ${index + 1}/${batches.size}")
            
            batch.forEach { user ->
                val potentialMatches = findPotentialMatches(user)
                val scores = potentialMatches.map { other ->
                    calculateAffinityScore(user, other)
                }
                
                // Save top matches (score > 70%)
                scores.filter { it.overall > 0.7 }
                    .take(20)
                    .forEach { saveMatch(user, it) }
            }
            
            delay(1000) // Rate limiting between batches
        }
        
        completeMatchRun(runId, success = true)
    } catch (e: Exception) {
        completeMatchRun(runId, success = false, error = e)
        throw e
    }
}
```

#### Optimizations
- [ ] Index database columns used in matching
- [ ] Cache user profiles during batch processing
- [ ] Parallel processing within safe limits
- [ ] Incremental matching (only new/changed users)
- [ ] Match expiration (refresh every 30 days)

### API Endpoints

#### New Routes
```kotlin
// Get matches for current user
GET /api/matches?type={friendship|romantic|both}&limit=20

// Get match details
GET /api/matches/:matchId

// Mark match as viewed
POST /api/matches/:matchId/view

// Express interest in match
POST /api/matches/:matchId/interest

// Dismiss match
POST /api/matches/:matchId/dismiss

// Get Proust questionnaire
GET /api/questionnaire

// Submit Proust answers
POST /api/questionnaire/answers
```

### Testing
- [ ] Unit tests for scoring algorithm
- [ ] Integration tests for matching pipeline
- [ ] Performance tests with 10k+ users
- [ ] Verify match quality manually

---

## PHASE 4: Performance, Scale & Observability (Week 5-6)

### Objectives
Ensure the system is performant, scalable, resilient, and observable.

### Performance Optimization

#### Frontend Performance
- [ ] **Code Splitting**: Lazy load screens/features
- [ ] **Image Optimization**: WebP format, lazy loading, placeholder blur
- [ ] **Bundle Size**: Tree shaking, minification
- [ ] **Caching Strategy**: 
  - Cache user profiles (5 min TTL)
  - Cache conversations list (1 min TTL)
  - Cache matches (30 min TTL)
- [ ] **Optimistic Updates**: Update UI before API response
- [ ] **Pagination**: Infinite scroll with virtual scrolling
- [ ] **Network Optimization**: Request batching, GraphQL (optional)

#### Backend Performance
- [ ] **Database Indexing**:
  ```sql
  CREATE INDEX idx_messages_conversation ON messages(conversation_id, sent_at DESC);
  CREATE INDEX idx_matches_user_score ON matches(user_a_id, affinity_score DESC);
  CREATE INDEX idx_users_active ON users(is_active, last_active_at);
  ```
- [ ] **Query Optimization**: 
  - Use `SELECT` specific columns, avoid `SELECT *`
  - Limit/offset pagination with cursor-based alternative
  - Join optimization for complex queries
- [ ] **Connection Pooling**: Configure PocketBase connection limits
- [ ] **Caching Layer**: Redis for hot data (consider for future)
- [ ] **CDN**: Static assets served from CDN

#### API Resilience

##### Retry Logic
```kotlin
// shared/src/commonMain/kotlin/network/RetryPolicy.kt
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 1000,
    maxDelay: Long = 10000,
    factor: Double = 2.0,
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (!shouldRetry(e)) throw e
            
            logger.warn("Attempt ${attempt + 1} failed, retrying in ${currentDelay}ms")
            delay(currentDelay)
            currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
        }
    }
    return block() // Last attempt
}

fun shouldRetry(e: Exception): Boolean {
    return when (e) {
        is NetworkException -> true
        is TimeoutException -> true
        is ServerException -> e.statusCode >= 500
        else -> false
    }
}
```

##### Circuit Breaker
```kotlin
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeout: Long = 60000
) {
    private var failureCount = 0
    private var state = State.CLOSED
    private var lastFailureTime = 0L
    
    suspend fun <T> execute(block: suspend () -> T): T {
        when (state) {
            State.OPEN -> {
                if (System.currentTimeMillis() - lastFailureTime > resetTimeout) {
                    state = State.HALF_OPEN
                } else {
                    throw CircuitBreakerException("Circuit is OPEN")
                }
            }
            State.HALF_OPEN -> { /* Try one request */ }
            State.CLOSED -> { /* Normal operation */ }
        }
        
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
}
```

##### Rate Limiting
- [ ] Client-side: Max 10 requests/second per API
- [ ] Server-side: 100 requests/minute per user (PocketBase middleware)
- [ ] Exponential backoff on 429 responses
- [ ] Queue requests during rate limit

##### Load Balancing
- [ ] Multiple PocketBase instances behind load balancer (future)
- [ ] Health check endpoints
- [ ] Graceful degradation (serve cached data if backend down)

### Monitoring & Telemetry

#### Metrics to Track
```kotlin
// Core Metrics
- API response times (p50, p95, p99)
- Error rates (4xx, 5xx) by endpoint
- Active users (DAU, MAU)
- Message delivery rate
- Match calculation time
- Database query performance
- Memory usage
- CPU usage

// Business Metrics
- New user signups
- Questionnaire completion rate
- Matches created per day
- Messages sent per day
- Match acceptance rate
- User retention (D1, D7, D30)
```

#### Logging Strategy
```kotlin
// Structured logging
logger.info(
    "api_request",
    mapOf(
        "endpoint" to "/api/matches",
        "method" to "GET",
        "user_id" to userId,
        "duration_ms" to duration,
        "status_code" to 200
    )
)

// Log levels
- ERROR: System failures, exceptions
- WARN: Degraded performance, retries
- INFO: Business events (signup, match, message)
- DEBUG: Detailed flow (dev only)
```

#### Error Tracking
- [ ] Capture and log all exceptions
- [ ] Include stack traces and context
- [ ] Group similar errors
- [ ] Alert on error rate spikes
- [ ] User impact tracking

#### Simple Monitoring Setup (Phase 1)
- [ ] **Application Logs**: 
  - Write to file with rotation (100MB max, keep 10 files)
  - Timestamp, level, message, context
  - Parse with grep/awk for now
  
- [ ] **Health Check Endpoint**:
  ```kotlin
  GET /health
  {
    "status": "healthy",
    "database": "connected",
    "uptime_seconds": 12345,
    "version": "1.0.0"
  }
  ```
  
- [ ] **Metrics Endpoint** (Prometheus format):
  ```kotlin
  GET /metrics
  # HELP api_requests_total Total API requests
  # TYPE api_requests_total counter
  api_requests_total{endpoint="/api/matches",method="GET"} 12345
  ```

- [ ] **Simple Dashboard**:
  - Script to parse logs and show stats
  - Cron job to check health every 5 minutes
  - Email alerts on critical errors

#### Advanced Monitoring (Phase 2 - Future)
- [ ] Grafana + Prometheus for dashboards
- [ ] Distributed tracing (Jaeger/Zipkin)
- [ ] APM tool (Sentry, DataDog, New Relic)
- [ ] Real-time alerting (PagerDuty, Slack)

### Scalability Planning

#### Database Scaling
- [ ] **Vertical**: Increase PocketBase server resources
- [ ] **Read Replicas**: Separate read/write traffic
- [ ] **Partitioning**: Shard by user ID (future)
- [ ] **Archiving**: Move old messages to cold storage

#### Application Scaling
- [ ] **Horizontal**: Multiple app instances (K8s/Docker)
- [ ] **Stateless Design**: No session storage in app (use tokens)
- [ ] **CDN**: Cache static assets globally
- [ ] **Edge Functions**: Move simple logic to edge (Cloudflare Workers)

#### Cost Optimization
- [ ] Monitor cloud costs monthly
- [ ] Right-size instances based on metrics
- [ ] Use reserved/spot instances
- [ ] Archive unused data

### Files to Create/Update
```
server/
├── src/main/kotlin/
│   ├── monitoring/
│   │   ├── MetricsCollector.kt       [CREATE]
│   │   ├── HealthCheck.kt            [CREATE]
│   │   └── Logger.kt                 [CREATE]
│   ├── jobs/
│   │   ├── MatchingJob.kt            [CREATE]
│   │   └── JobScheduler.kt           [CREATE]
│   └── middleware/
│       ├── RateLimiter.kt            [CREATE]
│       └── CircuitBreaker.kt         [CREATE]
└── scripts/
    ├── health-check.sh               [CREATE]
    ├── log-analyzer.sh               [CREATE]
    └── deploy.sh                     [CREATE]

shared/src/commonMain/kotlin/
└── network/
    ├── RetryPolicy.kt                [CREATE]
    ├── NetworkMonitor.kt             [CREATE]
    └── CacheManager.kt               [CREATE]
```

---

## 🔧 Technical Debt & Improvements

### Code Quality
- [ ] Add KDoc comments to public APIs
- [ ] Improve test coverage (target: 80%+)
- [ ] Set up pre-commit hooks (ktlint, detekt)
- [ ] Document architecture decisions (ADRs)

### Security
- [ ] Input validation on all endpoints
- [ ] SQL injection prevention (parameterized queries)
- [ ] XSS protection in UI
- [ ] Rate limiting on auth endpoints
- [ ] HTTPS enforcement
- [ ] Secure token storage (Keychain/Keystore)

### Accessibility
- [ ] Screen reader support
- [ ] Keyboard navigation
- [ ] Color contrast ratios (WCAG AA)
- [ ] Font scaling support
- [ ] Touch target sizes (min 44x44dp)

---

## 📊 Success Metrics

### Technical KPIs
- [ ] API response time p95 < 200ms
- [ ] Error rate < 0.1%
- [ ] App crash rate < 0.5%
- [ ] Uptime > 99.5%
- [ ] Match calculation < 100ms per pair

### Business KPIs
- [ ] User retention D7 > 40%
- [ ] Questionnaire completion > 70%
- [ ] Match acceptance rate > 20%
- [ ] Messages sent per active user > 5/day
- [ ] User satisfaction (NPS) > 50

---

## 📝 Documentation Needs

- [ ] **README.md**: Project overview, setup, commands
- [ ] **API.md**: All endpoints with examples
- [ ] **ARCHITECTURE.md**: System design, data flow
- [ ] **DEPLOYMENT.md**: How to deploy, environments
- [ ] **MONITORING.md**: Observability guide
- [ ] **MATCHING_ALGORITHM.md**: How matching works
- [ ] **CONTRIBUTING.md**: For future contributors
- [ ] **CHANGELOG.md**: Version history

---

## 🚀 Deployment Pipeline

### Environments
- **Local**: Development with hot reload
- **Staging**: Test environment (isolated DB)
- **Production**: Live environment

### CI/CD
- [ ] GitHub Actions for build/test
- [ ] Automated tests on PR
- [ ] Lint and format checks
- [ ] Build artifacts for all platforms
- [ ] Deploy to staging on merge to main
- [ ] Manual approval for production deploy

### Release Process
1. Version bump (semantic versioning)
2. Update CHANGELOG
3. Tag release in Git
4. Build platform artifacts (APK, IPA, DMG, etc.)
5. Deploy backend first
6. Deploy frontend apps
7. Monitor for issues
8. Rollback procedure if needed

---

## 🎯 IMMEDIATE NEXT STEPS (This Session)

### 1. Create Bottom Navigation (30 min)
- [x] BottomNavBar.kt component
- [ ] Wire into MainScreen
- [ ] Test navigation

### 2. Create Tab Screens (60 min)
- [ ] ConversationsListScreen
- [ ] UserGridScreen
- [ ] ProfileScreen
- [ ] SettingsScreen

### 3. Test End-to-End (15 min)
- [ ] Build and run desktop app
- [ ] Navigate through all tabs
- [ ] Verify theme consistency

### 4. Polish UX (30 min)
- [ ] Add loading states
- [ ] Add empty states
- [ ] Smooth transitions

**Total Time Estimate**: 2-3 hours

---

## 📋 Session Notes

**Date**: October 19, 2025
**Focus**: Phase 1 - Core UI & Navigation
**Safe Commands**: Use `ls` instead of `find`, avoid pipes with `head/tail`
**Build Command**: `./gradlew :composeApp:compileKotlinJvm --no-daemon --quiet`
**Run Command**: `./gradlew :composeApp:run --no-daemon`

**Key Files**:
- `TYPEERROR_WORKAROUND.md` - How to avoid CLI crashes
- `CURRENT_UI_STATUS.md` - Current implementation status
- `CONTINUE_UI_BUILD.md` - Next steps for UI
- `THIS FILE` - Complete roadmap and technical vision

**If Session Crashes**: Read these files to resume work exactly where we left off.
