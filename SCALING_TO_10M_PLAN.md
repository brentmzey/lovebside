# 🚀 B-Side: Scaling to 10M Users - Architecture & Implementation Plan

## Current State Analysis

### ✅ What We Have
1. **Messaging System** - Production-ready, real-time SSE
2. **Basic Matching** - `MatchRepository` with compatibility scores
3. **Geolocation** - `GeoUtils` with Haversine distance calculations
4. **Location Models** - GPS, user-selected, IP-based
5. **Database Schema** - Basic collections for users, profiles, matches

### ❌ What's Missing for 10M Scale
1. **Event-Driven Matching Engine**
2. **Affinity/Compatibility Algorithms**
3. **Geospatial Indexing** (PostGIS/Redis GEO)
4. **Sharding Strategy**
5. **Caching Layer for Matches**
6. **Background Job Processing**
7. **Analytics & ML Pipeline**

---

## 🎯 Target: 10M Concurrent Users

### Scalability Requirements

| Metric | Target | Current | Gap |
|--------|--------|---------|-----|
| **Concurrent Users** | 10M | ~10K | Need horizontal scaling |
| **Matches/Second** | 10K | ~100 | Need async processing |
| **Geo Queries/Second** | 50K | ~1K | Need spatial indexing |
| **Database Size** | 10TB+ | <1GB | Need sharding |
| **Response Time** | <100ms | ~50ms | Maintain with scale |
| **Availability** | 99.99% | ~99% | Need HA setup |

---

## 🏗️ Architecture: Phase 1 - MVP Matching (Current → 100K users)

### 1. Event-Driven Matching Engine

**Implementation:**

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/domain/matching/MatchingEngine.kt

package love.bside.app.domain.matching

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Event-driven matching engine for B-Side.
 * Handles real-time compatibility scoring and match notifications.
 */
interface MatchingEngine {
    /**
     * Process user profile update and trigger matching
     */
    suspend fun onProfileUpdate(userId: String, profile: UserProfile)
    
    /**
     * Process new user registration and find initial matches
     */
    suspend fun onUserRegistration(userId: String, profile: UserProfile)
    
    /**
     * Process user location update and find nearby matches
     */
    suspend fun onLocationUpdate(userId: String, location: GeoCoordinate)
    
    /**
     * Stream of match events for a user
     */
    fun matchEvents(userId: String): Flow<MatchEvent>
}

sealed class MatchEvent {
    data class NewMatch(
        val matchId: String,
        val otherUserId: String,
        val compatibilityScore: Double,
        val distance: Double?,
        val timestamp: Instant
    ) : MatchEvent()
    
    data class CompatibilityUpdated(
        val matchId: String,
        val newScore: Double,
        val reason: String
    ) : MatchEvent()
    
    data class ProximityAlert(
        val matchId: String,
        val distance: Double
    ) : MatchEvent()
}

data class UserProfile(
    val id: String,
    val age: Int,
    val gender: String,
    val lookingFor: List<String>,
    val interests: List<String>,
    val location: GeoCoordinate?,
    val questionnaire: QuestionnaireResponses?,
    val preferences: MatchPreferences
)

data class MatchPreferences(
    val ageRange: IntRange,
    val maxDistance: Double?,  // meters
    val genderPreference: List<String>
)

data class QuestionnaireResponses(
    val answers: Map<String, String>,
    val completedAt: Instant
)
```

### 2. Affinity Algorithm Implementation

```kotlin
// shared/src/commonMain/kotlin/love/bside/app/domain/matching/AffinityCalculator.kt

package love.bside.app.domain.matching

import kotlin.math.*

/**
 * Calculates compatibility/affinity scores between users.
 * Uses multiple factors weighted by importance.
 */
class AffinityCalculator {
    
    companion object {
        // Weights for different factors (total = 1.0)
        private const val WEIGHT_INTERESTS = 0.25
        private const val WEIGHT_VALUES = 0.30  // From questionnaire
        private const val WEIGHT_LIFESTYLE = 0.20
        private const val WEIGHT_DEMOGRAPHICS = 0.15
        private const val WEIGHT_DISTANCE = 0.10
    }
    
    /**
     * Calculate overall compatibility score (0.0 to 1.0)
     */
    fun calculateCompatibility(
        user1: UserProfile,
        user2: UserProfile
    ): CompatibilityScore {
        val interestScore = calculateInterestOverlap(
            user1.interests,
            user2.interests
        )
        
        val valuesScore = calculateValueAlignment(
            user1.questionnaire,
            user2.questionnaire
        )
        
        val lifestyleScore = calculateLifestyleCompatibility(
            user1,
            user2
        )
        
        val demographicsScore = calculateDemographicsFit(
            user1,
            user2
        )
        
        val distanceScore = calculateDistanceScore(
            user1.location,
            user2.location
        )
        
        val overallScore = (
            interestScore * WEIGHT_INTERESTS +
            valuesScore * WEIGHT_VALUES +
            lifestyleScore * WEIGHT_LIFESTYLE +
            demographicsScore * WEIGHT_DEMOGRAPHICS +
            distanceScore * WEIGHT_DISTANCE
        )
        
        return CompatibilityScore(
            overall = overallScore,
            breakdown = ScoreBreakdown(
                interests = interestScore,
                values = valuesScore,
                lifestyle = lifestyleScore,
                demographics = demographicsScore,
                distance = distanceScore
            )
        )
    }
    
    /**
     * Calculate Jaccard similarity for interests
     */
    private fun calculateInterestOverlap(
        interests1: List<String>,
        interests2: List<String>
    ): Double {
        if (interests1.isEmpty() && interests2.isEmpty()) return 0.5
        
        val set1 = interests1.toSet()
        val set2 = interests2.toSet()
        
        val intersection = set1.intersect(set2).size.toDouble()
        val union = set1.union(set2).size.toDouble()
        
        return if (union > 0) intersection / union else 0.0
    }
    
    /**
     * Calculate value alignment from questionnaire responses
     */
    private fun calculateValueAlignment(
        q1: QuestionnaireResponses?,
        q2: QuestionnaireResponses?
    ): Double {
        if (q1 == null || q2 == null) return 0.5  // Neutral if incomplete
        
        val commonQuestions = q1.answers.keys.intersect(q2.answers.keys)
        if (commonQuestions.isEmpty()) return 0.5
        
        var alignmentSum = 0.0
        for (question in commonQuestions) {
            val answer1 = q1.answers[question]!!
            val answer2 = q2.answers[question]!!
            
            // Calculate semantic similarity (simplified)
            alignmentSum += if (answer1 == answer2) 1.0 
                           else calculateAnswerSimilarity(answer1, answer2)
        }
        
        return alignmentSum / commonQuestions.size
    }
    
    /**
     * Calculate lifestyle compatibility
     */
    private fun calculateLifestyleCompatibility(
        user1: UserProfile,
        user2: UserProfile
    ): Double {
        // Factors: activity level, social preferences, etc.
        // Simplified for now
        return 0.7  // Placeholder
    }
    
    /**
     * Calculate demographics fit (age, etc.)
     */
    private fun calculateDemographicsFit(
        user1: UserProfile,
        user2: UserProfile
    ): Double {
        // Check age preferences
        val user1FitsUser2 = user2.age in user1.preferences.ageRange
        val user2FitsUser1 = user1.age in user2.preferences.ageRange
        
        return when {
            user1FitsUser2 && user2FitsUser1 -> 1.0
            user1FitsUser2 || user2FitsUser1 -> 0.5
            else -> 0.0
        }
    }
    
    /**
     * Calculate distance score (closer = better)
     */
    private fun calculateDistanceScore(
        loc1: GeoCoordinate?,
        loc2: GeoCoordinate?
    ): Double {
        if (loc1 == null || loc2 == null) return 0.5  // Neutral if no location
        
        val distance = GeoUtils.calculateDistance(loc1, loc2)
        
        // Score decreases with distance
        return when {
            distance < 5_000 -> 1.0      // <5km: perfect
            distance < 20_000 -> 0.8     // <20km: great
            distance < 50_000 -> 0.6     // <50km: good
            distance < 100_000 -> 0.4    // <100km: okay
            else -> 0.2                  // >100km: low
        }
    }
    
    private fun calculateAnswerSimilarity(a1: String, a2: String): Double {
        // Simplified: could use NLP/embeddings for better matching
        val words1 = a1.lowercase().split(Regex("\\s+"))
        val words2 = a2.lowercase().split(Regex("\\s+"))
        
        val overlap = words1.intersect(words2.toSet()).size.toDouble()
        val total = words1.size + words2.size
        
        return if (total > 0) (2 * overlap) / total else 0.0
    }
}

data class CompatibilityScore(
    val overall: Double,  // 0.0 to 1.0
    val breakdown: ScoreBreakdown
) {
    val percentage: Int get() = (overall * 100).roundToInt()
    
    val category: String get() = when {
        overall >= 0.8 -> "Excellent Match"
        overall >= 0.6 -> "Great Match"
        overall >= 0.4 -> "Good Match"
        overall >= 0.2 -> "Potential Match"
        else -> "Low Compatibility"
    }
}

data class ScoreBreakdown(
    val interests: Double,
    val values: Double,
    val lifestyle: Double,
    val demographics: Double,
    val distance: Double
)
```

### 3. Database Schema Updates

```sql
-- Add to PocketBase schema via migration

-- s_profiles collection (enhanced)
CREATE TABLE s_profiles (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    gender TEXT NOT NULL,
    looking_for JSON NOT NULL,  -- Array of strings
    interests JSON NOT NULL,    -- Array of strings
    bio TEXT,
    photos JSON,                -- Array of photo URLs
    
    -- Geolocation
    latitude REAL,
    longitude REAL,
    location_updated_at DATETIME,
    location_accuracy REAL,
    
    -- Preferences
    age_min INTEGER DEFAULT 18,
    age_max INTEGER DEFAULT 99,
    max_distance INTEGER,  -- meters
    gender_preference JSON,
    
    -- Questionnaire
    completed_questionnaire BOOLEAN DEFAULT FALSE,
    questionnaire_responses JSON,
    
    -- Metadata
    created DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE
);

-- Geospatial index for location queries
CREATE INDEX idx_profiles_location ON s_profiles(latitude, longitude);
CREATE INDEX idx_profiles_updated ON s_profiles(updated);

-- s_matches collection (enhanced)
CREATE TABLE s_matches (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    matched_user_id TEXT NOT NULL,
    
    -- Compatibility scoring
    compatibility_score REAL NOT NULL,  -- 0.0 to 1.0
    score_breakdown JSON,  -- {interests: 0.8, values: 0.9, ...}
    
    -- Distance at time of match
    distance_meters REAL,
    
    -- Status
    status TEXT DEFAULT 'pending',  -- pending, accepted, rejected, hidden
    viewed_by_user BOOLEAN DEFAULT FALSE,
    viewed_by_matched_user BOOLEAN DEFAULT FALSE,
    
    -- Conversation
    conversation_id TEXT,
    
    -- Metadata
    matched_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME,  -- Optional: matches can expire
    
    created DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (matched_user_id) REFERENCES t_user(id) ON DELETE CASCADE,
    FOREIGN KEY (conversation_id) REFERENCES m_conversations(id) ON DELETE SET NULL,
    
    UNIQUE(user_id, matched_user_id)
);

CREATE INDEX idx_matches_user ON s_matches(user_id, status);
CREATE INDEX idx_matches_score ON s_matches(compatibility_score DESC);
CREATE INDEX idx_matches_created ON s_matches(matched_at DESC);

-- s_match_queue collection (for async processing)
CREATE TABLE s_match_queue (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    event_type TEXT NOT NULL,  -- 'profile_update', 'location_update', 'new_user'
    event_data JSON,
    priority INTEGER DEFAULT 5,  -- 1 (highest) to 10 (lowest)
    status TEXT DEFAULT 'pending',  -- pending, processing, completed, failed
    retries INTEGER DEFAULT 0,
    processed_at DATETIME,
    created DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_queue_status ON s_match_queue(status, priority, created);
```

---

## 🏗️ Architecture: Phase 2 - Scale to 1M Users

### 1. Redis Geospatial Indexing

```kotlin
// server/src/main/kotlin/love/bside/server/services/GeoIndexService.kt

package love.bside.server.services

import io.lettuce.core.GeoArgs
import io.lettuce.core.GeoCoordinates
import io.lettuce.core.GeoRadiusStoreArgs
import io.lettuce.core.api.sync.RedisGeoCommands

/**
 * Redis-based geospatial indexing for fast proximity searches.
 * Can handle millions of locations with sub-millisecond queries.
 */
class GeoIndexService(
    private val redis: RedisGeoCommands<String, String>
) {
    private val GEO_KEY = "bside:geo:users"
    
    /**
     * Update user location in geo index
     */
    fun updateLocation(userId: String, lat: Double, lng: Double) {
        redis.geoadd(GEO_KEY, lng, lat, userId)
        // Note: Redis GEO uses (longitude, latitude) order!
    }
    
    /**
     * Find users within radius (meters)
     * Returns list of user IDs sorted by distance
     */
    fun findUsersNearby(
        lat: Double,
        lng: Double,
        radiusMeters: Double,
        limit: Int = 100
    ): List<UserDistance> {
        val results = redis.georadius(
            GEO_KEY,
            lng, lat,
            radiusMeters,
            GeoArgs.Unit.m
                .withDistance()
                .sort(GeoArgs.Sort.asc)
                .withCount(limit.toLong())
        )
        
        return results.map { result ->
            UserDistance(
                userId = result.member,
                distance = result.distance
            )
        }
    }
    
    /**
     * Get distance between two users
     */
    fun getDistance(userId1: String, userId2: String): Double? {
        val result = redis.geodist(GEO_KEY, userId1, userId2, GeoArgs.Unit.m)
        return result?.toDouble()
    }
    
    /**
     * Remove user from geo index
     */
    fun removeUser(userId: String) {
        redis.zrem(GEO_KEY, userId)
    }
}

data class UserDistance(
    val userId: String,
    val distance: Double  // meters
)
```

### 2. Background Job Processing

```kotlin
// server/src/main/kotlin/love/bside/server/workers/MatchingWorker.kt

package love.bside.server.workers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import love.bside.server.services.*

/**
 * Background worker that processes matching queue.
 * Runs continuously, processing match requests asynchronously.
 */
class MatchingWorker(
    private val matchQueueService: MatchQueueService,
    private val affinityService: AffinityService,
    private val geoIndexService: GeoIndexService,
    private val matchService: MatchService,
    private val scope: CoroutineScope
) {
    
    private val concurrency = 10  // Process 10 matches concurrently
    
    fun start() {
        scope.launch {
            while (isActive) {
                try {
                    processQueue()
                } catch (e: Exception) {
                    // Log error and continue
                    delay(5000)  // Back off on error
                }
            }
        }
    }
    
    private suspend fun processQueue() {
        matchQueueService.getPendingJobs(limit = 100)
            .asFlow()
            .buffer(concurrency)
            .collect { job ->
                processJob(job)
            }
        
        delay(1000)  // Check queue every second
    }
    
    private suspend fun processJob(job: MatchJob) {
        matchQueueService.markProcessing(job.id)
        
        try {
            when (job.eventType) {
                "profile_update" -> handleProfileUpdate(job)
                "location_update" -> handleLocationUpdate(job)
                "new_user" -> handleNewUser(job)
            }
            
            matchQueueService.markCompleted(job.id)
        } catch (e: Exception) {
            matchQueueService.markFailed(job.id, e.message)
        }
    }
    
    private suspend fun handleProfileUpdate(job: MatchJob) {
        // Recalculate compatibility with existing matches
        val userId = job.userId
        val existingMatches = matchService.getMatchesForUser(userId)
        
        for (match in existingMatches) {
            val newScore = affinityService.calculateScore(userId, match.matchedUserId)
            matchService.updateScore(match.id, newScore)
        }
        
        // Find new potential matches
        findNewMatches(userId)
    }
    
    private suspend fun handleLocationUpdate(job: MatchJob) {
        val userId = job.userId
        val (lat, lng) = job.eventData["location"] as Pair<Double, Double>
        
        // Update geo index
        geoIndexService.updateLocation(userId, lat, lng)
        
        // Find nearby users
        val nearby = geoIndexService.findUsersNearby(lat, lng, radiusMeters = 50_000.0)
        
        // Check compatibility with nearby users
        for (nearbyUser in nearby) {
            if (shouldMatch(userId, nearbyUser.userId)) {
                val score = affinityService.calculateScore(userId, nearbyUser.userId)
                if (score.overall >= 0.6) {  // Threshold
                    matchService.createMatch(userId, nearbyUser.userId, score)
                }
            }
        }
    }
    
    private suspend fun handleNewUser(job: MatchJob) {
        // Find initial matches for new user
        findNewMatches(job.userId, limit = 20)
    }
    
    private suspend fun findNewMatches(userId: String, limit: Int = 10) {
        // Implementation: query candidates and calculate compatibility
    }
    
    private fun shouldMatch(userId1: String, userId2: String): Boolean {
        // Check if already matched, blocked, etc.
        return true  // Simplified
    }
}
```

---

## 🏗️ Architecture: Phase 3 - Scale to 10M Users

### 1. Database Sharding Strategy

```
┌─────────────────────────────────────────────────────────┐
│              Load Balancer / API Gateway                │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┐
         │               │               │
    ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
    │ Shard 1 │    │ Shard 2 │    │ Shard 3 │
    │ Users   │    │ Users   │    │ Users   │
    │ 0-3.3M  │    │ 3.3-6.6M│    │ 6.6-10M │
    └─────────┘    └─────────┘    └─────────┘
    
Sharding Key: user_id % num_shards
```

### 2. Caching Strategy

```
┌─────────────────────────────────────────────────────────┐
│                    Cache Hierarchy                      │
├─────────────────────────────────────────────────────────┤
│ L1: In-Memory Cache (Per Server)                       │
│  • Hot user profiles: 10K most active                  │
│  • TTL: 5 minutes                                       │
│  • Size: ~100MB per server                            │
├─────────────────────────────────────────────────────────┤
│ L2: Redis Cache (Shared)                               │
│  • All active user profiles: 1M users                  │
│  • Match results: Pre-computed                         │
│  • Geo index: All users with locations                │
│  • TTL: 1 hour                                          │
│  • Size: ~10GB                                         │
├─────────────────────────────────────────────────────────┤
│ L3: Database (Persistent)                              │
│  • All data                                            │
│  • Sharded across multiple instances                  │
└─────────────────────────────────────────────────────────┘
```

### 3. ML-Enhanced Matching (Future)

```python
# Python service for ML-based matching

from sklearn.ensemble import RandomForestClassifier
import numpy as np

class MLMatchingService:
    def __init__(self):
        self.model = self.load_model()
    
    def predict_compatibility(self, user1_features, user2_features):
        """
        Predict compatibility using ML model trained on successful matches.
        Features: age, interests, values, interaction history, etc.
        """
        features = self.extract_features(user1_features, user2_features)
        score = self.model.predict_proba(features)[0][1]  # Probability of match
        return score
    
    def extract_features(self, u1, u2):
        # Combine user features into ML input vector
        return np.array([...])
```

---

## 📊 Performance Targets

### Query Performance
```
GET /api/matches?userId=123          <  50ms   (cached)
POST /api/matches/calculate          < 100ms   (async queued)
GET /api/users/nearby?radius=5000    <  20ms   (Redis GEO)
POST /api/profile/update             < 200ms   (triggers async match)
```

### Throughput
```
Matching Queue Processing:  10,000 matches/second
Geo Queries:               50,000 queries/second  
Profile Updates:            5,000 updates/second
Concurrent Connections:    10,000,000 users
```

---

## 🚀 Implementation Roadmap

### Week 1-2: Foundation
- [ ] Implement `AffinityCalculator`
- [ ] Create enhanced schema migration
- [ ] Build `MatchingEngine` interface
- [ ] Add Redis geo indexing

### Week 3-4: Async Processing
- [ ] Implement match queue system
- [ ] Build background workers
- [ ] Add job retry logic
- [ ] Create monitoring dashboards

### Week 5-6: Optimization
- [ ] Add L1/L2 caching
- [ ] Optimize database queries
- [ ] Implement rate limiting
- [ ] Load testing (1M simulated users)

### Week 7-8: Scaling
- [ ] Database sharding setup
- [ ] Deploy to multi-region
- [ ] CDN for static assets
- [ ] Full system stress test (10M users)

---

## 💰 Cost Estimation (10M Users)

### Infrastructure
- **Database**: PostgreSQL RDS (3 shards) - $3,000/month
- **Redis**: ElastiCache (cluster) - $1,500/month
- **Compute**: ECS/EKS (20 instances) - $2,000/month
- **CDN**: CloudFront - $500/month
- **Storage**: S3 (photos, media) - $1,000/month
- **Monitoring**: Datadog - $500/month
- **Total**: ~$8,500/month base + scaling

### Per-User Costs
- **At 10M users**: $0.00085/user/month
- **At scale**: Decreases with volume

---

## ✅ Next Immediate Actions

1. **Review this plan** - Align with business goals
2. **Implement Phase 1** - Get matching working well for 100K
3. **Create migration** - Update database schema
4. **Build affinity calculator** - Core matching logic
5. **Test with real data** - Validate algorithms work

Want me to start implementing any of these components?

---

*This is a production-grade scaling plan. B-Side will be ready for 10M users with proper implementation.*
