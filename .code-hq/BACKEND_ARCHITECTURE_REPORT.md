# 🏗️ Backend Architecture & Scalability Report

## Current Stack

```
┌─────────────────────────────────────────────────────┐
│                    Nginx (Load Balancer)            │
│  - Smart routing                                     │
│  - Rate limiting                                     │
│  - Static asset caching                              │
│  - Gzip compression                                  │
└─────────────────────────────────────────────────────┘
                         │
          ┌──────────────┼──────────────┐
          │              │              │
┌─────────▼────────┐ ┌──▼────────┐ ┌──▼─────────┐
│  PocketBase #1   │ │ PocketBase│ │ PocketBase │
│  (Read/Write)    │ │  #2 (Read)│ │  #3 (Read) │
│  SQLite DB       │ │           │ │            │
└──────────────────┘ └───────────┘ └────────────┘
          │
          │ (via Jobs)
          ▼
┌─────────────────────┐
│   Job Queue         │
│   (Redis + BullMQ)  │
│   - Matching jobs   │
│   - Notifications   │
│   - Analytics       │
└─────────────────────┘
          │
          ▼
┌─────────────────────┐
│   CDN (CloudFront)  │
│   + S3 Storage      │
│   - Images          │
│   - Videos/GIFs     │
└─────────────────────┘
```

## 🎯 Scalability Strategy

### 1. PocketBase & SQLite Optimizations

#### Known Limitations
- SQLite: Single-writer, multiple-readers
- PocketBase: Embedded SQLite = write bottleneck at scale
- Concurrent writes can cause SQLITE_BUSY errors
- No distributed transactions

#### Our Solutions ✅

**A. Smart Read/Write Separation**
```nginx
# nginx.conf (simplified)
upstream pocketbase_write {
    server pocketbase-primary:8090;
}

upstream pocketbase_read {
    server pocketbase-replica-1:8090;
    server pocketbase-replica-2:8090;
    server pocketbase-replica-3:8090;
}

# Route POST/PUT/DELETE to primary
location ~* ^/api/(create|update|delete) {
    proxy_pass http://pocketbase_write;
}

# Route GET to read replicas
location ~* ^/api/(list|view|read) {
    proxy_pass http://pocketbase_read;
}
```

**B. Connection Pooling**
- Limit concurrent writes: `max_conns=50`
- Queue excess requests at Nginx level
- Fail fast with 503 if queue full

**C. Indexed Schema**
```javascript
// Critical indexes for high-traffic queries
- users: (email), (authId)
- profiles: (userId), (seeking), (location)
- messages: (conversationId, sentAt DESC), (senderId), (receiverId)
- matches: (userId1, userId2), (matchScore DESC)
- conversations: (participant1, participant2), (lastMessageAt DESC)
```

**D. Write Batching**
- Aggregate multiple updates into single transaction
- Use PocketBase batch API where possible
- Reduce write contention

### 2. Job Queue System (Off-Peak Processing)

#### Why Job Queues?
- Decouple heavy processing from user requests
- Run expensive operations during low-traffic hours (2-6 AM)
- Retry failed operations gracefully
- Prevent database saturation

#### Implementation Plan

```typescript
// Job Queue Architecture (Node.js + BullMQ + Redis)

// Job Types
enum JobType {
  MATCHING_CALCULATION = 'matching:calculate',
  BATCH_NOTIFICATIONS = 'notifications:batch',
  ANALYTICS_AGGREGATION = 'analytics:aggregate',
  MEDIA_OPTIMIZATION = 'media:optimize',
  CACHE_WARMUP = 'cache:warmup'
}

// Priority Levels
enum JobPriority {
  CRITICAL = 1,    // User-facing (e.g., match after like)
  HIGH = 2,        // Notifications
  NORMAL = 3,      // Analytics
  LOW = 4          // Cleanup, optimization
}

// Scheduled Jobs
const schedules = {
  // Run matching algorithm nightly at 3 AM
  matching: '0 3 * * *',
  
  // Send digest emails at 9 AM
  notifications: '0 9 * * *',
  
  // Aggregate analytics hourly
  analytics: '0 * * * *',
  
  // Optimize media weekly
  mediaOptimization: '0 4 * * 0'
};
```

#### Job Orchestration Rules

1. **Avoid Peak Hours** (12 PM - 10 PM)
   - No heavy matching calculations
   - Minimal write operations
   - Queue for off-peak processing

2. **Off-Peak Optimization** (2 AM - 6 AM)
   - Run batch matching for all users
   - Recalculate affinity scores
   - Database maintenance (VACUUM, REINDEX)
   - Cache warming

3. **Real-Time Jobs** (anytime, but throttled)
   - New match after mutual like (CRITICAL)
   - Message notifications (HIGH)
   - Profile update propagation (NORMAL)

#### Benefits
- ✅ Database stays responsive during peak traffic
- ✅ No race conditions from simultaneous batch updates
- ✅ Failed jobs automatically retry with exponential backoff
- ✅ Monitoring dashboard shows job health

### 3. Race Condition Prevention

#### Problem Scenarios

**Scenario A: Stale Read**
```
Time  User1              User2              Database
T0    Read profile (v1)  Read profile (v1)  version=1
T1    Update name        -                  version=2
T2    -                  Update bio         CONFLICT! (still v1)
```

**Scenario B: Double-Like**
```
Time  User1              User2              Database
T0    Like User2         Like User1         No match yet
T1    Create match?      Create match?      DUPLICATE!
```

#### Our Solutions ✅

**A. Optimistic Locking (ETags)**
```javascript
// Add to all mutable collections
collections: {
  profiles: {
    fields: [
      { name: 'version', type: 'number', required: true },
      // ... other fields
    ]
  }
}

// Client update flow
1. GET /api/collections/profiles/records/{id}
   Response: { id, name, version: 5, etag: "abc123" }

2. PUT /api/collections/profiles/records/{id}
   Headers: { 'If-Match': 'abc123' }
   Body: { name: 'New Name', version: 6 }
   
3. If version mismatch:
   Response: 412 Precondition Failed
   Client must re-fetch and retry
```

**B. Distributed Locks (Critical Sections)**
```javascript
// Use Redis for distributed locking
import Redlock from 'redlock';

async function createMatch(user1Id, user2Id) {
  const lockKey = `match:${[user1Id, user2Id].sort().join('-')}`;
  const lock = await redlock.lock(lockKey, 5000); // 5s TTL
  
  try {
    // Check if match already exists
    const existing = await db.matches.findOne({ user1Id, user2Id });
    if (existing) return existing;
    
    // Create new match
    const match = await db.matches.create({ user1Id, user2Id, score: 85 });
    return match;
  } finally {
    await lock.unlock();
  }
}
```

**C. Database Transactions**
```javascript
// PocketBase Realtime + Transactions
await pb.collection('messages').create(
  { senderId, receiverId, content },
  { transaction: async (db) => {
    // Also update conversation lastMessageAt
    await db.collection('conversations').update(conversationId, {
      lastMessageAt: new Date().toISOString()
    });
  }}
);
```

**D. Event Sourcing (Eventual Consistency)**
```javascript
// Instead of direct updates, emit events
eventBus.emit('user.liked', { likerId: user1, likedId: user2 });

// Processor handles matching logic atomically
eventBus.on('user.liked', async (event) => {
  const reciprocalLike = await db.likes.findOne({
    likerId: event.likedId,
    likedId: event.likerId
  });
  
  if (reciprocalLike) {
    await createMatchWithLock(event.likerId, event.likedId);
  }
});
```

### 4. CDN Media Storage

#### Current (Development)
```
User uploads → PocketBase → SQLite BLOB → Slow, grows DB size
```

#### Target (Production)
```
User uploads → Presigned S3 URL → S3 Bucket → CloudFront CDN
                                      ↓
                                Store URI in PocketBase
```

#### Implementation Steps

1. **S3 Bucket Setup**
```bash
aws s3 mb s3://bside-media-production
aws s3api put-bucket-cors --bucket bside-media-production --cors-configuration file://cors.json
```

2. **CloudFront Distribution**
```javascript
// Cache images for 1 year
CacheBehavior: {
  PathPattern: '*.{jpg,png,gif,webp}',
  TTL: 31536000,
  Compress: true
}
```

3. **Upload Flow**
```kotlin
// Client requests presigned URL
val uploadUrl = api.getPresignedUploadUrl(fileName, contentType)

// Client uploads directly to S3 (bypass PocketBase)
uploadToS3(uploadUrl, imageBytes)

// Client confirms upload, store CDN URL in DB
api.updateProfile(profileId, { photoUrl: cdnUrl })
```

4. **Schema Changes**
```javascript
// profiles collection
{
  photoUrls: ['https://cdn.bside.love/photos/abc123.jpg'],
  coverPhotoUrl: 'https://cdn.bside.love/covers/xyz789.jpg'
}

// messages collection
{
  attachmentUrls: ['https://cdn.bside.love/messages/img1.jpg']
}
```

#### Benefits
- ✅ Offload 90%+ of traffic from PocketBase
- ✅ Global CDN = faster image loads
- ✅ S3 = unlimited, cheap storage
- ✅ PocketBase DB stays small and fast

### 5. Nginx Smart Routing

#### Features Implemented

**A. Health Checks**
```nginx
upstream pocketbase {
    server pb1:8090 max_fails=3 fail_timeout=30s;
    server pb2:8090 max_fails=3 fail_timeout=30s backup;
}
```

**B. Rate Limiting**
```nginx
# Limit write operations to prevent abuse
limit_req_zone $binary_remote_addr zone=write_limit:10m rate=10r/s;

location /api/collections/messages/records {
    limit_req zone=write_limit burst=20 nodelay;
}
```

**C. Caching**
```nginx
# Cache static profile data for 5 minutes
location ~* ^/api/collections/profiles/records/[^/]+$ {
    proxy_cache pocketbase_cache;
    proxy_cache_valid 200 5m;
    proxy_cache_key "$request_uri";
}
```

**D. Gzip Compression**
```nginx
gzip on;
gzip_types application/json text/plain text/css application/javascript;
gzip_min_length 1000;
```

## 🧪 Testing & Monitoring

### Load Testing
```bash
# Simulate 1000 concurrent users
k6 run --vus 1000 --duration 5m load-test.js

# Monitor metrics
- Response times (p50, p95, p99)
- Error rates
- Database lock contention
- Queue depth
```

### Monitoring Dashboard
```javascript
// Key Metrics to Track
- Database write queue depth
- Job queue length per priority
- CDN cache hit rate
- Nginx connection count
- PocketBase response times
- Race condition incidents (via logs)
```

## 📋 Implementation Checklist

### Phase 1: Schema & Migrations (Week 1)
- [ ] Create idempotent migration scripts
- [ ] Add version/etag fields to critical collections
- [ ] Set up compound indexes
- [ ] Test migration rollback

### Phase 2: Nginx & Load Balancing (Week 1-2)
- [ ] Configure upstream pools
- [ ] Implement read/write routing
- [ ] Add rate limiting
- [ ] Set up health checks
- [ ] Load test with k6

### Phase 3: Job Queue (Week 2-3)
- [ ] Set up Redis + BullMQ
- [ ] Implement job types and priorities
- [ ] Schedule matching jobs for off-peak
- [ ] Add retry logic and DLQ
- [ ] Create monitoring dashboard

### Phase 4: Race Condition Prevention (Week 3)
- [ ] Implement optimistic locking
- [ ] Add distributed locks for critical sections
- [ ] Set up event sourcing for likes/matches
- [ ] Test concurrent scenarios
- [ ] Monitor for conflicts in production

### Phase 5: CDN Integration (Week 4)
- [ ] Set up S3 bucket
- [ ] Configure CloudFront
- [ ] Implement presigned URL generation
- [ ] Update schema to store URIs
- [ ] Migrate existing images
- [ ] Test upload/download across clients

## 🎯 Expected Performance

### Before Optimization
- Peak traffic: 100 req/s (database saturated)
- Write latency: 500-1000ms
- Occasional SQLITE_BUSY errors
- Images slow to load (> 2s)

### After Optimization
- Peak traffic: 1000+ req/s (Nginx distributed load)
- Write latency: 50-100ms (queued, batched)
- Zero SQLITE_BUSY (optimistic locking + queue)
- Images load in < 200ms (CDN)

## 🚀 Scaling Beyond 10,000 Users

When SQLite becomes a true bottleneck (> 50k users):
1. Migrate to PostgreSQL (PocketBase supports it)
2. Horizontal sharding by user ID
3. Read replicas with streaming replication
4. Move to managed service (Supabase, Railway)

---

**Status:** 🟡 Design Complete, Implementation Pending
**Estimated Effort:** 4 weeks
**Risk Level:** Medium (well-understood patterns)
