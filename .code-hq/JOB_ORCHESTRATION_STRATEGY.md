# Intelligent Job Orchestration & Load Balancing Strategy

## The Problem

**Competing Database Operations:**
```
Peak Traffic (9am-11pm):
├─ User API requests (high priority)
├─ Real-time messaging (critical)
├─ Match algorithm jobs (heavy, can wait)
├─ Server-side events (medium priority)
└─ Cron jobs (background, flexible)

⚠️  Risk: Heavy matching jobs starve user-facing operations
⚠️  Risk: Cron jobs during peak hours cause slowdowns
⚠️  Risk: Event-driven writes conflict with batch operations
```

---

## Solution Architecture

### 1. Priority-Based Queue System

**Queue Tiers:**
```typescript
enum JobPriority {
  CRITICAL = 0,    // User auth, message send
  HIGH = 1,        // Profile updates, swipe actions
  MEDIUM = 2,      // Match scoring updates
  LOW = 3,         // Analytics, aggregations
  BACKGROUND = 4   // Match recalculation, cleanup
}

enum JobType {
  // CRITICAL (process immediately, bypass queue)
  USER_AUTH = 'user_auth',
  MESSAGE_SEND = 'message_send',
  
  // HIGH (process within 1s)
  PROFILE_UPDATE = 'profile_update',
  SWIPE_ACTION = 'swipe_action',
  READ_RECEIPT = 'read_receipt',
  
  // MEDIUM (process within 5s)
  MATCH_SCORE_UPDATE = 'match_score_update',
  TYPING_INDICATOR = 'typing_indicator',
  
  // LOW (process within 30s)
  VIEW_COUNTER = 'view_counter',
  ANALYTICS_EVENT = 'analytics_event',
  
  // BACKGROUND (process during off-peak, can take hours)
  FULL_MATCH_RECALC = 'full_match_recalc',
  DATA_CLEANUP = 'data_cleanup',
  REPORT_GENERATION = 'report_generation'
}
```

---

### 2. Time-Based Job Scheduling

**Dynamic Schedule Based on Load:**
```typescript
interface TimeWindow {
  name: string;
  hours: [number, number];  // Start, end hour (UTC)
  characteristics: string[];
  allowedJobTypes: JobType[];
  maxConcurrency: number;
}

const SCHEDULE: TimeWindow[] = [
  // Off-Peak: 2am-6am UTC (10pm-2am EST)
  {
    name: 'DEEP_NIGHT',
    hours: [2, 6],
    characteristics: ['very_low_traffic', 'heavy_jobs_ok'],
    allowedJobTypes: [
      JobType.FULL_MATCH_RECALC,    // Heavy: OK
      JobType.DATA_CLEANUP,          // Heavy: OK
      JobType.REPORT_GENERATION      // Heavy: OK
    ],
    maxConcurrency: 10  // Run many jobs in parallel
  },
  
  // Morning Ramp: 6am-9am UTC (2am-5am EST)
  {
    name: 'EARLY_MORNING',
    hours: [6, 9],
    characteristics: ['low_traffic', 'medium_jobs_ok'],
    allowedJobTypes: [
      JobType.MATCH_SCORE_UPDATE,    // Medium: OK
      JobType.ANALYTICS_EVENT        // Low: OK
    ],
    maxConcurrency: 5
  },
  
  // Peak Hours: 9am-11pm UTC (5am-7pm EST)
  {
    name: 'PEAK_HOURS',
    hours: [9, 23],
    characteristics: ['high_traffic', 'critical_only'],
    allowedJobTypes: [
      JobType.USER_AUTH,             // Critical: Always
      JobType.MESSAGE_SEND,          // Critical: Always
      JobType.PROFILE_UPDATE,        // High: OK
      JobType.SWIPE_ACTION           // High: OK
    ],
    maxConcurrency: 2  // Minimal background jobs
  },
  
  // Evening Wind-Down: 11pm-2am UTC (7pm-10pm EST)
  {
    name: 'EVENING',
    hours: [23, 2],
    characteristics: ['medium_traffic', 'medium_jobs_ok'],
    allowedJobTypes: [
      JobType.MATCH_SCORE_UPDATE,
      JobType.ANALYTICS_EVENT,
      JobType.VIEW_COUNTER
    ],
    maxConcurrency: 4
  }
];
```

---

### 3. Load-Aware Job Scheduler

**Monitor System Load in Real-Time:**
```typescript
interface SystemMetrics {
  // Database metrics
  sqliteWriteQueueDepth: number;      // Current pending writes
  sqliteWriteLatency: number;         // p95 write time (ms)
  activeConnections: number;          // Current DB connections
  
  // API metrics
  apiRequestRate: number;             // Requests/second
  apiErrorRate: number;               // % errors
  apiLatencyP95: number;              // p95 response time (ms)
  
  // Resource metrics
  cpuUsage: number;                   // % CPU
  memoryUsage: number;                // % Memory
  diskIOWait: number;                 // % I/O wait
  
  // Derived
  isHealthy: boolean;
  loadLevel: 'low' | 'medium' | 'high' | 'critical';
}

class LoadAwareScheduler {
  private metrics: SystemMetrics;
  
  async shouldRunJob(job: Job): Promise<boolean> {
    const metrics = await this.getCurrentMetrics();
    const timeWindow = this.getCurrentTimeWindow();
    
    // 1. Check time window permissions
    if (!timeWindow.allowedJobTypes.includes(job.type)) {
      return false;  // Job not allowed during this time
    }
    
    // 2. Critical jobs always run (bypass all checks)
    if (job.priority === JobPriority.CRITICAL) {
      return true;
    }
    
    // 3. Check system load thresholds
    const loadChecks = [
      // SQLite write queue depth
      metrics.sqliteWriteQueueDepth < 500,  // Pause if queue > 500
      
      // API health
      metrics.apiLatencyP95 < 500,          // Pause if API slow
      metrics.apiErrorRate < 0.01,          // Pause if errors > 1%
      
      // Resource availability
      metrics.cpuUsage < 80,                // Pause if CPU > 80%
      metrics.memoryUsage < 85,             // Pause if memory > 85%
      
      // Overall health
      metrics.isHealthy
    ];
    
    // 4. Adjust based on job priority
    if (job.priority === JobPriority.HIGH) {
      // High priority: Only check critical metrics
      return loadChecks[0] && loadChecks[1] && loadChecks[5];
    }
    
    if (job.priority === JobPriority.BACKGROUND) {
      // Background: ALL checks must pass
      return loadChecks.every(check => check);
    }
    
    // Medium/Low: Most checks must pass
    const passedChecks = loadChecks.filter(c => c).length;
    return passedChecks >= 4;
  }
  
  async getCurrentMetrics(): Promise<SystemMetrics> {
    // Query Prometheus/DataDog/custom metrics
    const [db, api, resources] = await Promise.all([
      this.getDBMetrics(),
      this.getAPIMetrics(),
      this.getResourceMetrics()
    ]);
    
    const isHealthy = 
      db.writeQueueDepth < 1000 &&
      api.errorRate < 0.02 &&
      resources.cpuUsage < 90;
    
    let loadLevel: SystemMetrics['loadLevel'] = 'low';
    if (api.requestRate > 5000) loadLevel = 'critical';
    else if (api.requestRate > 2000) loadLevel = 'high';
    else if (api.requestRate > 500) loadLevel = 'medium';
    
    return {
      ...db,
      ...api,
      ...resources,
      isHealthy,
      loadLevel
    };
  }
}
```

---

### 4. Intelligent Match Job Orchestration

**Incremental vs Full Recalculation:**
```typescript
class MatchJobOrchestrator {
  private scheduler: LoadAwareScheduler;
  private redis: Redis;
  
  /**
   * Strategy:
   * - Incremental updates: Run during peak (fast, targeted)
   * - Full recalculation: Run during off-peak (slow, comprehensive)
   */
  
  async scheduleMatchUpdates(userIds: string[]) {
    const timeWindow = this.getCurrentTimeWindow();
    
    if (timeWindow.name === 'PEAK_HOURS') {
      // Peak: Only incremental updates for active users
      await this.scheduleIncrementalUpdates(userIds);
    } else {
      // Off-peak: Full recalculation allowed
      await this.scheduleFullRecalculation();
    }
  }
  
  /**
   * Incremental: Update matches for specific users only
   * Fast: ~10ms per user, run during peak hours
   */
  async scheduleIncrementalUpdates(userIds: string[]) {
    for (const userId of userIds) {
      await this.redis.zadd('match:incremental', {
        score: Date.now(),
        value: userId
      });
      
      // Add to medium-priority queue
      await this.queueJob({
        type: JobType.MATCH_SCORE_UPDATE,
        priority: JobPriority.MEDIUM,
        data: { userId, mode: 'incremental' },
        estimatedDuration: 10  // ms
      });
    }
  }
  
  /**
   * Full: Recalculate all matches for all users
   * Slow: ~5min total, only run during off-peak (2am-6am)
   */
  async scheduleFullRecalculation() {
    const timeWindow = this.getCurrentTimeWindow();
    
    // Only allow during DEEP_NIGHT
    if (timeWindow.name !== 'DEEP_NIGHT') {
      console.log('⏸️  Full recalc deferred to off-peak hours');
      return;
    }
    
    // Break into chunks of 100 users each
    const allUserIds = await this.getAllUserIds();
    const chunks = this.chunkArray(allUserIds, 100);
    
    for (const [index, chunk] of chunks.entries()) {
      await this.queueJob({
        type: JobType.FULL_MATCH_RECALC,
        priority: JobPriority.BACKGROUND,
        data: { 
          userIds: chunk, 
          chunkIndex: index,
          totalChunks: chunks.length 
        },
        estimatedDuration: 5000  // 5s per chunk
      });
    }
    
    console.log(`📊 Scheduled ${chunks.length} match recalc chunks`);
  }
  
  /**
   * Event-Driven: User profile updated
   * Trigger immediate match score update for their connections
   */
  async onProfileUpdated(userId: string) {
    // Get users who have this user in their potential matches
    const affectedUserIds = await this.getAffectedUsers(userId);
    
    // High priority: Update quickly (within 5s)
    await this.queueJob({
      type: JobType.MATCH_SCORE_UPDATE,
      priority: JobPriority.HIGH,
      data: { 
        userId, 
        affectedUsers: affectedUserIds,
        trigger: 'profile_updated' 
      },
      estimatedDuration: 50  // ms
    });
  }
  
  /**
   * Event-Driven: New user signed up
   * Calculate their initial matches
   */
  async onUserSignup(userId: string) {
    // High priority: New users get fast matches
    await this.queueJob({
      type: JobType.MATCH_SCORE_UPDATE,
      priority: JobPriority.HIGH,
      data: { 
        userId, 
        mode: 'initial',
        trigger: 'signup' 
      },
      estimatedDuration: 100  // ms
    });
  }
}
```

---

### 5. Cron Job Coordination

**Separate Cron Jobs from User Traffic:**
```typescript
interface CronConfig {
  name: string;
  schedule: string;  // Cron expression
  jobType: JobType;
  allowedWindows: string[];  // Time window names
  estimatedDuration: number;  // ms
  maxRetries: number;
}

const CRON_JOBS: CronConfig[] = [
  // Daily full match recalculation (off-peak only)
  {
    name: 'daily_match_recalc',
    schedule: '0 3 * * *',  // 3am UTC daily
    jobType: JobType.FULL_MATCH_RECALC,
    allowedWindows: ['DEEP_NIGHT'],
    estimatedDuration: 300000,  // 5 minutes
    maxRetries: 2
  },
  
  // Hourly incremental updates (off-peak preferred)
  {
    name: 'hourly_match_update',
    schedule: '0 * * * *',  // Every hour
    jobType: JobType.MATCH_SCORE_UPDATE,
    allowedWindows: ['DEEP_NIGHT', 'EARLY_MORNING', 'EVENING'],
    estimatedDuration: 60000,  // 1 minute
    maxRetries: 3
  },
  
  // Daily analytics aggregation
  {
    name: 'daily_analytics',
    schedule: '0 4 * * *',  // 4am UTC daily
    jobType: JobType.REPORT_GENERATION,
    allowedWindows: ['DEEP_NIGHT'],
    estimatedDuration: 120000,  // 2 minutes
    maxRetries: 1
  },
  
  // Cleanup old data (weekly, off-peak)
  {
    name: 'weekly_cleanup',
    schedule: '0 2 * * 0',  // 2am UTC Sunday
    jobType: JobType.DATA_CLEANUP,
    allowedWindows: ['DEEP_NIGHT'],
    estimatedDuration: 600000,  // 10 minutes
    maxRetries: 1
  }
];

class CronOrchestrator {
  async executeCronJob(config: CronConfig) {
    const timeWindow = this.getCurrentTimeWindow();
    
    // 1. Check if allowed during current time window
    if (!config.allowedWindows.includes(timeWindow.name)) {
      console.log(`⏸️  ${config.name} deferred (wrong time window)`);
      await this.scheduleLater(config);
      return;
    }
    
    // 2. Check system load
    const shouldRun = await this.scheduler.shouldRunJob({
      type: config.jobType,
      priority: JobPriority.BACKGROUND,
      estimatedDuration: config.estimatedDuration
    });
    
    if (!shouldRun) {
      console.log(`⏸️  ${config.name} deferred (high load)`);
      await this.scheduleLater(config);
      return;
    }
    
    // 3. Execute job
    console.log(`▶️  ${config.name} starting...`);
    const startTime = Date.now();
    
    try {
      await this.queueJob({
        type: config.jobType,
        priority: JobPriority.BACKGROUND,
        data: { cronName: config.name },
        estimatedDuration: config.estimatedDuration
      });
      
      const duration = Date.now() - startTime;
      console.log(`✅ ${config.name} completed in ${duration}ms`);
      
    } catch (error) {
      console.error(`❌ ${config.name} failed:`, error);
      
      if (config.maxRetries > 0) {
        await this.scheduleRetry(config, config.maxRetries - 1);
      }
    }
  }
  
  /**
   * Defer cron job to next allowed time window
   */
  async scheduleLater(config: CronConfig) {
    const nextWindow = this.getNextAllowedWindow(config.allowedWindows);
    const delayMs = this.getMillisecondsUntil(nextWindow.hours[0]);
    
    console.log(`⏰ ${config.name} rescheduled for ${nextWindow.name}`);
    
    await this.redis.zadd('cron:deferred', {
      score: Date.now() + delayMs,
      value: config.name
    });
  }
}
```

---

### 6. Write Coordination Strategy

**Prevent Write Conflicts:**
```typescript
class WriteCoordinator {
  private writeLock: Map<string, Promise<void>> = new Map();
  
  /**
   * Coordinate writes to same entity
   * Ensures: User profile writes don't conflict with match job writes
   */
  async coordinatedWrite<T>(
    entityType: string,
    entityId: string,
    operation: () => Promise<T>,
    priority: JobPriority
  ): Promise<T> {
    const lockKey = `${entityType}:${entityId}`;
    
    // Wait for any existing write to complete
    const existingLock = this.writeLock.get(lockKey);
    if (existingLock) {
      await existingLock;
    }
    
    // Create new lock
    const lockPromise = this.executeWrite(operation, priority);
    this.writeLock.set(lockKey, lockPromise);
    
    try {
      const result = await lockPromise;
      return result;
    } finally {
      this.writeLock.delete(lockKey);
    }
  }
  
  private async executeWrite<T>(
    operation: () => Promise<T>,
    priority: JobPriority
  ): Promise<T> {
    // Critical writes: Execute immediately
    if (priority === JobPriority.CRITICAL || priority === JobPriority.HIGH) {
      return await operation();
    }
    
    // Background writes: Check load first
    const shouldRun = await this.scheduler.shouldRunJob({
      type: JobType.MATCH_SCORE_UPDATE,
      priority,
      estimatedDuration: 10
    });
    
    if (!shouldRun) {
      // Queue for later
      throw new Error('Write deferred due to high load');
    }
    
    return await operation();
  }
}

// Usage:
const coordinator = new WriteCoordinator();

// User-facing write (high priority, never queued)
await coordinator.coordinatedWrite(
  'profile',
  userId,
  async () => {
    await pb.collection('s_profiles').update(userId, { bio: 'Updated' });
  },
  JobPriority.HIGH
);

// Background job write (low priority, may be queued)
await coordinator.coordinatedWrite(
  'match',
  `${userId1}-${userId2}`,
  async () => {
    await pb.collection('m_matches').update(matchId, { score: 85 });
  },
  JobPriority.LOW
);
```

---

### 7. Circuit Breaker Pattern

**Automatically Pause Jobs During High Load:**
```typescript
class CircuitBreaker {
  private state: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED';
  private failureCount = 0;
  private lastFailureTime = 0;
  private readonly FAILURE_THRESHOLD = 5;
  private readonly TIMEOUT = 60000;  // 1 minute
  
  async execute<T>(
    operation: () => Promise<T>,
    priority: JobPriority
  ): Promise<T> {
    // Critical jobs bypass circuit breaker
    if (priority === JobPriority.CRITICAL) {
      return await operation();
    }
    
    if (this.state === 'OPEN') {
      // Circuit open: Check if timeout elapsed
      if (Date.now() - this.lastFailureTime > this.TIMEOUT) {
        this.state = 'HALF_OPEN';
      } else {
        throw new Error('Circuit breaker OPEN - high system load');
      }
    }
    
    try {
      const result = await operation();
      
      if (this.state === 'HALF_OPEN') {
        // Success after HALF_OPEN: Close circuit
        this.state = 'CLOSED';
        this.failureCount = 0;
        console.log('✅ Circuit breaker CLOSED - system recovered');
      }
      
      return result;
      
    } catch (error) {
      this.failureCount++;
      this.lastFailureTime = Date.now();
      
      if (this.failureCount >= this.FAILURE_THRESHOLD) {
        this.state = 'OPEN';
        console.log('🔴 Circuit breaker OPEN - pausing background jobs');
      }
      
      throw error;
    }
  }
}
```

---

## Implementation Summary

### Queue Configuration

```typescript
// BullMQ Queue Setup
const queues = {
  critical: new Queue('critical', {
    redis,
    defaultJobOptions: {
      attempts: 3,
      backoff: { type: 'exponential', delay: 1000 },
      priority: 1  // Highest
    }
  }),
  
  high: new Queue('high', {
    redis,
    defaultJobOptions: {
      attempts: 3,
      backoff: { type: 'exponential', delay: 2000 },
      priority: 2
    }
  }),
  
  medium: new Queue('medium', {
    redis,
    defaultJobOptions: {
      attempts: 2,
      backoff: { type: 'fixed', delay: 5000 },
      priority: 3,
      removeOnComplete: 1000
    }
  }),
  
  low: new Queue('low', {
    redis,
    defaultJobOptions: {
      attempts: 2,
      backoff: { type: 'fixed', delay: 10000 },
      priority: 4,
      removeOnComplete: 100
    }
  }),
  
  background: new Queue('background', {
    redis,
    defaultJobOptions: {
      attempts: 1,
      backoff: { type: 'fixed', delay: 300000 },  // 5 min
      priority: 5,  // Lowest
      removeOnComplete: 10
    }
  })
};

// Workers (different concurrency per priority)
const workers = {
  critical: new Worker('critical', processor, { concurrency: 50 }),
  high: new Worker('high', processor, { concurrency: 20 }),
  medium: new Worker('medium', processor, { concurrency: 10 }),
  low: new Worker('low', processor, { concurrency: 5 }),
  background: new Worker('background', processor, { concurrency: 2 })
};
```

---

## Monitoring Dashboard

```
┌─────────────────────────────────────────────────────┐
│  🚦 Job Orchestration Dashboard                    │
├─────────────────────────────────────────────────────┤
│                                                     │
│  Time Window: PEAK_HOURS (9am-11pm UTC)            │
│  Load Level: MEDIUM                                 │
│  System Health: ✅ HEALTHY                          │
│                                                     │
│  ┌──────────────────────────────────────┐          │
│  │ Queue Depths                         │          │
│  ├──────────────────────────────────────┤          │
│  │ Critical:    5 jobs  ▓▓░░░░░░░░      │          │
│  │ High:       23 jobs  ▓▓▓▓░░░░░░      │          │
│  │ Medium:    156 jobs  ▓▓▓▓▓▓▓░░░      │          │
│  │ Low:       892 jobs  ▓▓▓▓▓▓▓▓▓▓ FULL │          │
│  │ Background: PAUSED   ░░░░░░░░░░      │          │
│  └──────────────────────────────────────┘          │
│                                                     │
│  ┌──────────────────────────────────────┐          │
│  │ System Metrics                       │          │
│  ├──────────────────────────────────────┤          │
│  │ SQLite Write Queue:  234 (⚠️ High)   │          │
│  │ API Request Rate:    2,341/s         │          │
│  │ API Latency (p95):   387ms           │          │
│  │ CPU Usage:           67%             │          │
│  │ Memory Usage:        72%             │          │
│  └──────────────────────────────────────┘          │
│                                                     │
│  ⏸️  Background jobs paused (high load)            │
│  ⏰ Full match recalc scheduled for 3am UTC        │
│  ✅ Critical/High jobs processing normally         │
│                                                     │
└─────────────────────────────────────────────────────┘
```

---

## Benefits Summary

✅ **No Write Conflicts**
- Coordinated writes prevent race conditions
- Priority-based execution
- Entity-level locking

✅ **Load-Aware Scheduling**
- Automatically pause heavy jobs during peak
- Real-time system metrics
- Circuit breaker for safety

✅ **Time-Based Optimization**
- Heavy jobs only during off-peak (2am-6am)
- Incremental updates during peak
- Smart cron scheduling

✅ **Event-Driven + Batch**
- Fast event-driven updates (< 5s)
- Efficient batch processing (off-peak)
- Best of both worlds

✅ **Observable & Debuggable**
- Detailed job metrics
- Queue depth monitoring
- Load-based alerts

---

**Document Version**: 1.0  
**Last Updated**: January 30, 2026  
**Status**: Architecture Approved ✅
