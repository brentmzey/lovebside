# B-Side Scalability Architecture

## Current Limitations

### SQLite Write Bottlenecks
- **Single Writer**: SQLite allows only ONE writer at a time
- **Write Lock**: Other writes must wait, causing queuing delays
- **Concurrent Reads**: OK during writes with WAL mode, but writes still serialize
- **File-Based**: No network distribution of writes across nodes

### PocketBase Constraints
- **Single Instance Writes**: Even with multiple PocketBase instances, all write to same SQLite file
- **No Built-in Replication**: No native master-slave or multi-master setup
- **SSE Connection Limits**: Each instance has connection limits for real-time subscriptions

---

## Scalability Solution Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                          CLIENTS                                │
│  (Android, iOS, Web, Desktop)                                   │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                     CLOUDFLARE / AWS ROUTE 53                   │
│                    (DNS + DDoS Protection)                      │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AWS CloudFront (CDN)                         │
│  - Static Assets (JS, CSS, Images)                             │
│  - User Uploaded Media (Profile Photos, Messages)              │
│  - Edge Caching (Low Latency Globally)                         │
└────────────┬────────────────────────────────────────────────────┘
             │
             ▼
┌─────────────────────────────────────────────────────────────────┐
│              NGINX LOAD BALANCER (HA Proxy Alt)                │
│                                                                  │
│  Smart Routing:                                                 │
│  - /api/pb/auth/*      → PocketBase Pool (Read Heavy)          │
│  - /api/pb/files/*     → Direct S3 Upload (Bypass PB)          │
│  - /api/pb/realtime/*  → PocketBase (Sticky Sessions)          │
│  - /api/v1/match/*     → Redis Queue → Workers                 │
│  - /api/v1/heavy/*     → Async Jobs (Bull Queue)               │
│                                                                  │
│  Health Checks:                                                 │
│  - Active/Passive failover                                      │
│  - Circuit breaker pattern                                      │
│  - Connection pooling (keepalive 256)                           │
└────────────┬───────────────┬────────────────┬───────────────────┘
             │               │                │
     ┌───────┴────┐  ┌──────┴─────┐  ┌───────┴────┐
     ▼            ▼            ▼            ▼
┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐
│  PB-1   │ │  PB-2   │ │  PB-3   │ │  PB-4   │  ← Read Replicas
│ (Read)  │ │ (Read)  │ │ (Read)  │ │ (Write) │     (SQLite WAL)
└────┬────┘ └────┬────┘ └────┬────┘ └────┬────┘
     │           │            │           │
     └───────────┴────────────┴───────────┘
                      │
                      ▼
            ┌──────────────────┐
            │  SQLite (WAL)    │  ← Primary Database
            │  - Fast Reads    │
            │  - Write Queue   │
            └──────────────────┘
                      │
                      ▼
            ┌──────────────────┐
            │  Redis Cluster   │  ← Caching + Queue
            │  - Session Store │
            │  - Write Queue   │
            │  - Cache Layer   │
            │  - Pub/Sub       │
            └──────────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
   ┌─────────────┐        ┌─────────────┐
   │ Bull Workers│        │ Match Jobs  │  ← Background Workers
   │ - Matching  │        │ - Notifs    │
   │ - Analytics │        │ - Email     │
   └─────────────┘        └─────────────┘
                      │
                      ▼
            ┌──────────────────┐
            │    AWS S3        │  ← Media Storage
            │ - Profile Photos │
            │ - Message Media  │
            │ - Backups        │
            └──────────────────┘
                      │
                      ▼
            ┌──────────────────┐
            │  CloudFront CDN  │  ← Global Distribution
            │ - Edge Locations │
            │ - Low Latency    │
            └──────────────────┘
```

---

## Component Breakdown

### 1. Nginx Load Balancer (Enhanced)

**Features:**
- Round-robin across 4 PocketBase instances
- Sticky sessions for SSE connections
- Health checks every 10s
- Circuit breaker (3 failures = 30s timeout)
- Connection pooling (keepalive 256)
- Rate limiting per endpoint

**Configuration:**
```nginx
upstream pocketbase_read {
    least_conn;  # Route to least busy instance
    
    server pb1:8090 max_fails=3 fail_timeout=30s weight=1;
    server pb2:8090 max_fails=3 fail_timeout=30s weight=1;
    server pb3:8090 max_fails=3 fail_timeout=30s weight=1;
    server pb4:8090 max_fails=3 fail_timeout=30s weight=1;
    
    keepalive 256;  # Connection pooling
}

upstream pocketbase_write {
    # Single writer for SQLite consistency
    server pb-master:8090 max_fails=3 fail_timeout=30s;
    keepalive 64;
}

upstream redis_queue {
    server redis-1:6379 max_fails=2 fail_timeout=10s;
    server redis-2:6379 backup;  # Failover
}
```

---

### 2. Redis Queue System (BullMQ)

**Purpose:**
- Decouple write operations from API requests
- Batch writes to reduce lock contention
- Retry failed writes with exponential backoff
- Prioritize critical operations

**Use Cases:**
```
HIGH PRIORITY (Process immediately):
- User authentication
- Message sending
- Profile updates

MEDIUM PRIORITY (Batch every 5s):
- Match scoring updates
- Read receipts
- Typing indicators

LOW PRIORITY (Batch every 30s):
- Analytics events
- View counters
- Last seen timestamps
```

**Implementation:**
```typescript
// Queue Configuration
const matchingQueue = new Queue('matching', {
  connection: redis,
  defaultJobOptions: {
    attempts: 3,
    backoff: {
      type: 'exponential',
      delay: 2000
    },
    removeOnComplete: 1000,
    removeOnFail: 5000
  }
});

// Write Batching
const writeBatcher = {
  batch: [],
  timer: null,
  
  add(operation) {
    this.batch.push(operation);
    
    if (this.batch.length >= 100) {
      this.flush();  // Flush if batch full
    } else if (!this.timer) {
      this.timer = setTimeout(() => this.flush(), 5000);  // Flush every 5s
    }
  },
  
  async flush() {
    const ops = this.batch.splice(0);
    clearTimeout(this.timer);
    this.timer = null;
    
    if (ops.length === 0) return;
    
    // Execute batch write in transaction
    await db.transaction(async (tx) => {
      for (const op of ops) {
        await tx.execute(op);
      }
    });
  }
};
```

---

### 3. SQLite WAL Mode Optimization

**Configuration:**
```sql
-- Enable WAL mode (Write-Ahead Logging)
PRAGMA journal_mode = WAL;

-- Optimize for concurrent reads
PRAGMA synchronous = NORMAL;  -- Instead of FULL
PRAGMA cache_size = -64000;   -- 64MB cache
PRAGMA temp_store = MEMORY;   -- Temp tables in RAM
PRAGMA mmap_size = 268435456; -- 256MB memory-mapped I/O

-- Auto-checkpoint every 1000 pages
PRAGMA wal_autocheckpoint = 1000;

-- Busy timeout for write contention
PRAGMA busy_timeout = 5000;   -- Wait 5s for locks
```

**Benefits:**
- Readers don't block writers
- Writers don't block readers
- ~10x improvement in concurrent reads
- Checkpoints happen in background

---

### 4. CDN + S3 Media Architecture

**Flow:**
```
User Upload → Nginx → Pre-signed S3 URL → Direct Upload → S3
                                              ↓
                                        CloudFront Invalidation
                                              ↓
                                        Update PocketBase Record
                                        (Store CDN URL, not file)
```

**Benefits:**
- **Bypass PocketBase**: No SQLite writes during uploads
- **Direct to S3**: Nginx generates pre-signed URL, client uploads directly
- **CDN Edge Caching**: Media served from 200+ global locations
- **Cost Effective**: S3 cheaper than server bandwidth

**Implementation:**
```typescript
// Generate Pre-Signed Upload URL
async function getUploadUrl(userId: string, fileType: string) {
  const key = `users/${userId}/${Date.now()}-${uuidv4()}.${fileType}`;
  
  const command = new PutObjectCommand({
    Bucket: process.env.AWS_S3_BUCKET,
    Key: key,
    ContentType: `image/${fileType}`,
    ACL: 'public-read'
  });
  
  const uploadUrl = await getSignedUrl(s3Client, command, { expiresIn: 3600 });
  
  const cdnUrl = `${process.env.CDN_BASE_URL}/${key}`;
  
  return { uploadUrl, cdnUrl, key };
}

// Client uploads directly to S3, then updates PocketBase
async function uploadProfilePhoto(file: File) {
  // 1. Get pre-signed URL
  const { uploadUrl, cdnUrl } = await api.get('/api/v1/media/upload-url', {
    fileType: file.type
  });
  
  // 2. Upload directly to S3
  await fetch(uploadUrl, {
    method: 'PUT',
    body: file,
    headers: { 'Content-Type': file.type }
  });
  
  // 3. Update PocketBase record with CDN URL
  await pb.collection('s_profiles').update(userId, {
    avatar_url: cdnUrl  // CDN URL, not local file
  });
}
```

---

### 5. Read/Write Separation

**Strategy:**
```
READS (95% of traffic):
→ Load balanced across PB-1, PB-2, PB-3, PB-4
→ SQLite WAL allows concurrent reads
→ Redis cache for hot data (profiles, matches)

WRITES (5% of traffic):
→ All writes go to Redis Queue
→ Queue worker writes to PB-4 (master)
→ Batched when possible
→ Critical writes get priority

REALTIME (SSE):
→ Sticky sessions to same PocketBase instance
→ Pub/Sub via Redis for cross-instance events
```

---

### 6. Caching Strategy

**Redis Cache Layers:**

```typescript
// L1: Hot user data (profiles viewed in last 5 min)
const profileCache = new RedisCache('profiles', {
  ttl: 300,  // 5 minutes
  maxSize: 10000  // 10k profiles
});

// L2: Match scores (recomputed every hour)
const matchCache = new RedisCache('matches', {
  ttl: 3600,  // 1 hour
  maxSize: 50000  // 50k match pairs
});

// L3: Static data (interests, questions)
const staticCache = new RedisCache('static', {
  ttl: 86400,  // 24 hours
  maxSize: 1000
});

// Cache-aside pattern
async function getProfile(userId: string) {
  // Try cache first
  let profile = await profileCache.get(userId);
  
  if (!profile) {
    // Cache miss - fetch from DB
    profile = await pb.collection('s_profiles').getOne(userId);
    await profileCache.set(userId, profile);
  }
  
  return profile;
}

// Write-through pattern
async function updateProfile(userId: string, data: any) {
  // Add to write queue
  await writeQueue.add({ type: 'update_profile', userId, data });
  
  // Invalidate cache immediately
  await profileCache.del(userId);
  
  // Update local copy for optimistic UI
  return data;
}
```

---

## Scaling Numbers

### Current Capacity (Single PocketBase)
- **Reads**: ~1,000 req/s
- **Writes**: ~50 req/s (limited by SQLite)
- **SSE Connections**: ~5,000 concurrent
- **Storage**: Unlimited (until disk full)

### With Proposed Architecture
- **Reads**: ~10,000 req/s (4 instances × 2,500)
- **Writes**: ~500 req/s (batched via queue)
- **SSE Connections**: ~20,000 concurrent (4 × 5,000)
- **Storage**: Unlimited (S3 + CloudFront)

### Cost Estimates (1M Monthly Active Users)
```
AWS CloudFront:  $150/month (1TB transfer)
AWS S3:          $50/month (500GB storage)
Redis Cloud:     $200/month (10GB cache)
PocketBase x4:   $400/month (4 × $100 VPS)
Workers x2:      $200/month (2 × $100 VPS)
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Total:           ~$1,000/month
```

---

## Monitoring & Alerts

**Key Metrics:**
- SQLite write queue depth (alert if > 1000)
- Redis queue lag (alert if > 30s)
- PocketBase response time (p95 < 500ms)
- CDN cache hit rate (target > 90%)
- Error rate (< 0.5%)

**Tools:**
- Prometheus + Grafana for metrics
- Sentry for error tracking
- DataDog APM for distributed tracing

---

## Disaster Recovery

**Backup Strategy:**
```
SQLite DB:       Hourly snapshots to S3
Redis Cache:     No backup needed (ephemeral)
S3 Media:        Versioning enabled + lifecycle rules
Configuration:   Git + Terraform
```

**Recovery Time:**
- **RTO** (Recovery Time Objective): < 15 minutes
- **RPO** (Recovery Point Objective): < 1 hour

---

## Next Steps

1. ✅ **Implement Redis Queue** (task:infra-005)
2. ✅ **Configure Multi-Instance PocketBase** (task:infra-006)
3. ✅ **Setup S3 + CloudFront CDN** (task:infra-010 - NEW)
4. ✅ **Enhanced Nginx Config** (task:infra-007)
5. ✅ **Monitoring Dashboard** (task:devops-001)

---

**Document Version**: 2.0  
**Last Updated**: January 30, 2026  
**Status**: Architecture Approved ✅
