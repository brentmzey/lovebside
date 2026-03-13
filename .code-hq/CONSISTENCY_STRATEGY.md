# Distributed Consistency & Race Condition Prevention

## The Problem

**Race Conditions in Distributed Systems:**

```
Scenario 1: Profile Update Race
─────────────────────────────────
Time    Client A              Client B              Database
────────────────────────────────────────────────────────────
T1      Read profile (v1)     Read profile (v1)     version=1
T2      Update bio            Update avatar         version=1
T3      Write (v1→v2) ✅                            version=2
T4                            Write (v1→v2) ❌      CONFLICT!
                              (Lost update!)

Result: Client B's update overwrites Client A's changes


Scenario 2: Match Score Race
─────────────────────────────
Time    User Event           Batch Job             Database
────────────────────────────────────────────────────────────
T1      Profile updated      Start full recalc     score=75
T2      Trigger recalc       Read old data         score=75
T3      New score=85         Calculate score=80    score=75
T4      Write score=85 ✅                          score=85
T5                           Write score=80 ❌     score=80
                             (Stale data!)

Result: Newer score (85) gets overwritten by older batch job (80)


Scenario 3: Double Swipe
─────────────────────────
Time    Request 1            Request 2             Database
────────────────────────────────────────────────────────────
T1      Swipe right on U2    Swipe right on U2     swipes=0
T2      Check: no match      Check: no match       swipes=0
T3      Create match         Create match          swipes=0
T4      Save ✅                                     match_id=1
T5                           Save ✅               match_id=2
                             (Duplicate!)

Result: Two match records for same pair!


Scenario 4: Message Ordering
─────────────────────────────
Time    Server 1 (PB-1)      Server 2 (PB-2)       Clients
────────────────────────────────────────────────────────────
T1      Receive msg A        Receive msg B         -
T2      Write msg A          Write msg B           -
T3      Broadcast A          Broadcast B           -
T4      Client gets A ✅     Client gets B ✅      B, A ❌
                                                   (Wrong order!)

Result: Messages appear out of order on client
```

---

## Solution Architecture

### 1. Distributed Locking (Redis)

**Optimistic Locking with Versioning:**
```typescript
interface VersionedEntity {
  id: string;
  data: any;
  version: number;  // Incremented on every update
  updatedAt: string;
}

class OptimisticLock {
  /**
   * Optimistic locking: Check version before update
   * 
   * Benefits:
   * - No blocking (high throughput)
   * - Detects conflicts automatically
   * - Fails fast on stale data
   */
  async updateWithVersion<T>(
    collection: string,
    id: string,
    expectedVersion: number,
    updates: Partial<T>
  ): Promise<Result<T>> {
    // Read current record
    const current = await pb.collection(collection).getOne(id);
    
    // Check version
    if (current.version !== expectedVersion) {
      return {
        error: 'VERSION_CONFLICT',
        message: `Expected v${expectedVersion}, got v${current.version}`,
        currentVersion: current.version,
        currentData: current
      };
    }
    
    // Update with incremented version
    try {
      const updated = await pb.collection(collection).update(id, {
        ...updates,
        version: expectedVersion + 1,
        updatedAt: new Date().toISOString()
      });
      
      return { success: true, data: updated };
    } catch (e) {
      // Another concurrent update won
      return { error: 'WRITE_CONFLICT', message: e.message };
    }
  }
}

// Usage:
const profile = await pb.collection('s_profiles').getOne(userId);
const result = await optimisticLock.updateWithVersion(
  's_profiles',
  userId,
  profile.version,  // Expected version
  { bio: 'Updated bio' }
);

if (result.error === 'VERSION_CONFLICT') {
  // Conflict! Retry with latest data
  console.log('Conflict detected, retrying...');
  await retryWithLatest(userId);
}
```

**Pessimistic Locking with Redis:**
```typescript
class PessimisticLock {
  private redis: Redis;
  
  /**
   * Pessimistic locking: Acquire lock before update
   * 
   * Use cases:
   * - Critical operations (payments, swipes)
   * - High contention resources
   * - Need guaranteed exclusive access
   */
  async withLock<T>(
    lockKey: string,
    operation: () => Promise<T>,
    ttl: number = 5000  // Lock expires after 5s
  ): Promise<T> {
    const lockValue = uuidv4();
    const maxRetries = 10;
    let retries = 0;
    
    // Try to acquire lock
    while (retries < maxRetries) {
      const acquired = await this.redis.set(
        `lock:${lockKey}`,
        lockValue,
        'PX', ttl,  // Expire after ttl ms
        'NX'        // Only if not exists
      );
      
      if (acquired === 'OK') {
        try {
          // Lock acquired! Execute operation
          const result = await operation();
          return result;
        } finally {
          // Release lock (only if we still own it)
          await this.releaseLock(lockKey, lockValue);
        }
      }
      
      // Lock not acquired, wait and retry
      retries++;
      await this.sleep(50 + Math.random() * 100);  // Exponential backoff
    }
    
    throw new Error(`Failed to acquire lock: ${lockKey}`);
  }
  
  private async releaseLock(lockKey: string, lockValue: string) {
    // Lua script: Only delete if value matches (atomic)
    const script = `
      if redis.call("get", KEYS[1]) == ARGV[1] then
        return redis.call("del", KEYS[1])
      else
        return 0
      end
    `;
    
    await this.redis.eval(script, 1, `lock:${lockKey}`, lockValue);
  }
}

// Usage:
await pessimisticLock.withLock(
  `user:${userId}:swipe`,
  async () => {
    // Check if already swiped
    const existing = await checkExistingMatch(userId, targetId);
    if (existing) return;
    
    // Create match
    await createMatch(userId, targetId);
  }
);
```

---

### 2. Sequencing & Ordering

**Message Sequencing:**
```typescript
/**
 * Ensure messages are ordered correctly across distributed servers
 * 
 * Strategy:
 * - Server-side sequence numbers
 * - Client-side ordering buffer
 * - Gap detection & repair
 */

interface Message {
  id: string;
  conversationId: string;
  senderId: string;
  content: string;
  sequenceNumber: number;  // Monotonically increasing
  timestamp: string;
}

class MessageSequencer {
  /**
   * Assign sequence number when message is persisted
   */
  async sendMessage(
    conversationId: string,
    senderId: string,
    content: string
  ): Promise<Message> {
    // Acquire lock on conversation
    return await pessimisticLock.withLock(
      `conversation:${conversationId}:send`,
      async () => {
        // Get next sequence number
        const lastSeq = await this.redis.get(
          `conversation:${conversationId}:seq`
        );
        const nextSeq = (parseInt(lastSeq || '0') + 1);
        
        // Create message with sequence
        const message = await pb.collection('mg_messages').create({
          conversationId,
          senderId,
          content,
          sequenceNumber: nextSeq,
          timestamp: new Date().toISOString()
        });
        
        // Update sequence counter
        await this.redis.set(
          `conversation:${conversationId}:seq`,
          nextSeq
        );
        
        // Broadcast to all participants
        await this.broadcastMessage(conversationId, message);
        
        return message;
      }
    );
  }
  
  /**
   * Client-side ordering buffer
   * Handle out-of-order delivery
   */
  class MessageBuffer {
    private buffer: Map<number, Message> = new Map();
    private nextExpected: number = 1;
    
    /**
     * Add message to buffer, emit if in order
     */
    addMessage(message: Message): Message[] {
      const { sequenceNumber } = message;
      
      // Already processed?
      if (sequenceNumber < this.nextExpected) {
        return [];  // Duplicate
      }
      
      // Add to buffer
      this.buffer.set(sequenceNumber, message);
      
      // Emit all consecutive messages
      const ready: Message[] = [];
      while (this.buffer.has(this.nextExpected)) {
        const msg = this.buffer.get(this.nextExpected)!;
        ready.push(msg);
        this.buffer.delete(this.nextExpected);
        this.nextExpected++;
      }
      
      return ready;
    }
    
    /**
     * Detect gaps and request missing messages
     */
    detectGaps(): number[] {
      const gaps: number[] = [];
      const bufferedSeqs = Array.from(this.buffer.keys()).sort();
      
      if (bufferedSeqs.length === 0) return gaps;
      
      const maxBuffered = Math.max(...bufferedSeqs);
      for (let seq = this.nextExpected; seq < maxBuffered; seq++) {
        if (!this.buffer.has(seq)) {
          gaps.push(seq);
        }
      }
      
      return gaps;
    }
  }
}
```

---

### 3. Read-After-Write Consistency

**Problem: Reading stale data immediately after write**

```typescript
/**
 * Ensure client reads their own writes
 * 
 * Strategies:
 * 1. Session pinning (sticky routing)
 * 2. Write-through cache
 * 3. Read-your-writes guarantee
 */

class ConsistentReader {
  private writeTimestamps: Map<string, number> = new Map();
  
  /**
   * Track when we last wrote to each entity
   */
  async write(collection: string, id: string, data: any) {
    const result = await pb.collection(collection).update(id, data);
    
    // Remember write timestamp
    this.writeTimestamps.set(
      `${collection}:${id}`,
      Date.now()
    );
    
    return result;
  }
  
  /**
   * Ensure we read data at least as new as our write
   */
  async read(collection: string, id: string): Promise<any> {
    const writeTime = this.writeTimestamps.get(`${collection}:${id}`);
    
    if (!writeTime) {
      // No recent write, read from any replica
      return await pb.collection(collection).getOne(id);
    }
    
    // Recent write: ensure consistency
    const timeSinceWrite = Date.now() - writeTime;
    
    if (timeSinceWrite < 1000) {
      // < 1s since write: Read from master
      return await this.readFromMaster(collection, id);
    } else {
      // > 1s: Replicas should be caught up
      return await pb.collection(collection).getOne(id);
    }
  }
  
  private async readFromMaster(collection: string, id: string) {
    // Force read from master instance
    const masterClient = new PocketBase('http://pocketbase-master:8090');
    masterClient.authStore.loadFromCookie(pb.authStore.exportToCookie());
    return await masterClient.collection(collection).getOne(id);
  }
}
```

---

### 4. Idempotency Tokens

**Problem: Duplicate requests (network retries, double-clicks)**

```typescript
/**
 * Ensure operations are idempotent using tokens
 * 
 * Use cases:
 * - Swipe actions (prevent double swipes)
 * - Message sends (prevent duplicates)
 * - Match creations (prevent double matches)
 */

interface IdempotentRequest {
  token: string;         // Client-generated UUID
  operation: string;     // Operation type
  params: any;           // Operation parameters
  expiresAt: number;     // Token expiration (24h)
}

class IdempotencyGuard {
  private redis: Redis;
  
  /**
   * Execute operation only if token not seen before
   */
  async executeIdempotent<T>(
    token: string,
    operation: () => Promise<T>
  ): Promise<T> {
    const key = `idempotency:${token}`;
    
    // Check if already processed
    const existing = await this.redis.get(key);
    if (existing) {
      // Already processed! Return cached result
      return JSON.parse(existing);
    }
    
    // Mark as in-progress (prevent concurrent execution)
    const acquired = await this.redis.set(
      key,
      'PROCESSING',
      'EX', 86400,  // Expire after 24h
      'NX'          // Only if not exists
    );
    
    if (!acquired) {
      // Another request with same token is in progress
      // Wait and retry
      await this.sleep(100);
      return await this.executeIdempotent(token, operation);
    }
    
    try {
      // Execute operation
      const result = await operation();
      
      // Cache result
      await this.redis.set(
        key,
        JSON.stringify(result),
        'EX', 86400
      );
      
      return result;
      
    } catch (error) {
      // Operation failed, allow retry
      await this.redis.del(key);
      throw error;
    }
  }
}

// Usage:
app.post('/api/v1/swipe', async (req, res) => {
  const { token, userId, targetId, action } = req.body;
  
  const result = await idempotencyGuard.executeIdempotent(
    token,
    async () => {
      // This will only execute once per token
      return await processSwipe(userId, targetId, action);
    }
  );
  
  res.json(result);
});
```

---

### 5. Conflict Resolution Strategies

**Last-Write-Wins (LWW):**
```typescript
/**
 * Simplest strategy: Timestamp-based
 * 
 * Pros: Simple, always converges
 * Cons: May lose data
 */
interface LWWEntity {
  id: string;
  data: any;
  timestamp: number;  // Lamport timestamp or physical clock
}

function mergeWithLWW(local: LWWEntity, remote: LWWEntity): LWWEntity {
  return local.timestamp > remote.timestamp ? local : remote;
}
```

**Merge Strategy (CRDT-inspired):**
```typescript
/**
 * Smart merging for specific fields
 * 
 * Example: Profile updates
 * - Bio: Last-write-wins
 * - Interests: Union (merge arrays)
 * - Photos: Union (merge arrays)
 */
function mergeProfiles(
  local: Profile,
  remote: Profile,
  base: Profile  // Common ancestor
): Profile {
  return {
    id: local.id,
    
    // Text fields: LWW
    bio: local.bioUpdatedAt > remote.bioUpdatedAt
      ? local.bio
      : remote.bio,
    
    // Arrays: Union
    interests: [...new Set([
      ...local.interests,
      ...remote.interests
    ])],
    
    // Photos: Union (keep all)
    photos: [...new Set([
      ...local.photos,
      ...remote.photos
    ])],
    
    // Numeric: Max (for counters)
    viewCount: Math.max(local.viewCount, remote.viewCount)
  };
}
```

---

### 6. Smart Load Balancing with Consistency

**Session Affinity (Sticky Sessions):**
```nginx
# Nginx configuration
upstream pocketbase_read {
  least_conn;
  
  # Sticky sessions based on user ID
  hash $cookie_user_id consistent;
  
  server pb1:8090;
  server pb2:8090;
  server pb3:8090;
  server pb4:8090;
}

# Route user to same replica for read-after-write consistency
location /api/pb/collections {
  proxy_pass http://pocketbase_read;
  
  # Set cookie for sticky sessions
  add_header Set-Cookie "user_id=$http_authorization; Path=/; Max-Age=3600";
}
```

**Request Routing Based on Operation:**
```typescript
/**
 * Smart routing: Send requests to appropriate server
 * 
 * Rules:
 * - Writes: Always to master
 * - Reads (recent write): To master
 * - Reads (no recent write): To any replica
 * - Realtime: Sticky to same instance
 */

class SmartRouter {
  private recentWrites: Map<string, number> = new Map();
  
  route(request: Request): string {
    const { operation, entityType, entityId, userId } = request;
    
    // 1. Writes always go to master
    if (operation === 'write') {
      this.recentWrites.set(`${userId}:${entityType}:${entityId}`, Date.now());
      return 'http://pocketbase-master:8090';
    }
    
    // 2. Reads after recent write: go to master
    const writeTime = this.recentWrites.get(`${userId}:${entityType}:${entityId}`);
    if (writeTime && (Date.now() - writeTime) < 2000) {
      return 'http://pocketbase-master:8090';  // Read-your-writes
    }
    
    // 3. Realtime: Sticky sessions
    if (operation === 'subscribe') {
      return this.getStickyInstance(userId);
    }
    
    // 4. Normal reads: Load balanced
    return this.getHealthyReplica();
  }
  
  private getStickyInstance(userId: string): string {
    // Hash user ID to consistent instance
    const hash = this.hashCode(userId);
    const instanceIndex = hash % 4;  // 4 instances
    return `http://pocketbase-${instanceIndex + 1}:8090`;
  }
  
  private getHealthyReplica(): string {
    // Return least-loaded healthy replica
    // (In production, query health metrics)
    return 'http://pocketbase-1:8090';
  }
}
```

---

### 7. Transaction Coordinator

**Distributed Transactions (2-Phase Commit):**
```typescript
/**
 * Coordinate multi-entity updates
 * 
 * Example: Create match when both users swipe right
 * - Update user1's swipes
 * - Update user2's swipes
 * - Create match record
 * - Send notifications
 * 
 * All must succeed or all must rollback
 */

interface Transaction {
  id: string;
  operations: Operation[];
  state: 'PENDING' | 'PREPARED' | 'COMMITTED' | 'ABORTED';
}

class TransactionCoordinator {
  /**
   * Execute multiple operations atomically
   */
  async executeTransaction(operations: Operation[]): Promise<void> {
    const txId = uuidv4();
    const participants: string[] = [];
    
    try {
      // Phase 1: PREPARE
      console.log(`📝 Transaction ${txId}: PREPARE`);
      
      for (const op of operations) {
        const participant = await this.prepare(txId, op);
        participants.push(participant);
      }
      
      // All prepared successfully!
      
      // Phase 2: COMMIT
      console.log(`✅ Transaction ${txId}: COMMIT`);
      
      for (const participant of participants) {
        await this.commit(txId, participant);
      }
      
      console.log(`🎉 Transaction ${txId}: SUCCESS`);
      
    } catch (error) {
      // Any failure: ABORT all
      console.log(`❌ Transaction ${txId}: ABORT`);
      
      for (const participant of participants) {
        await this.abort(txId, participant);
      }
      
      throw error;
    }
  }
  
  private async prepare(txId: string, op: Operation): Promise<string> {
    // Lock resources
    await pessimisticLock.withLock(
      `tx:${txId}:${op.resource}`,
      async () => {
        // Validate operation can succeed
        await op.validate();
        
        // Write to transaction log
        await this.redis.hset(`tx:${txId}`, op.resource, 'PREPARED');
      }
    );
    
    return op.resource;
  }
  
  private async commit(txId: string, resource: string) {
    // Execute operation
    await this.redis.hset(`tx:${txId}`, resource, 'COMMITTED');
  }
  
  private async abort(txId: string, resource: string) {
    // Rollback changes
    await this.redis.hset(`tx:${txId}`, resource, 'ABORTED');
  }
}
```

---

## Consistency Levels

```typescript
enum ConsistencyLevel {
  /**
   * EVENTUAL: Fastest, may see stale data
   * Use for: Feed browsing, match suggestions
   */
  EVENTUAL = 'eventual',
  
  /**
   * READ_YOUR_WRITES: See your own updates
   * Use for: Profile viewing after edit
   */
  READ_YOUR_WRITES = 'read_your_writes',
  
  /**
   * MONOTONIC_READS: Never see older data
   * Use for: Message threads
   */
  MONOTONIC_READS = 'monotonic_reads',
  
  /**
   * STRONG: Always latest data
   * Use for: Swipes, matches, payments
   */
  STRONG = 'strong'
}

class ConsistencyManager {
  async read(
    collection: string,
    id: string,
    level: ConsistencyLevel
  ): Promise<any> {
    switch (level) {
      case ConsistencyLevel.EVENTUAL:
        // Read from any replica (fastest)
        return await this.readFromReplica(collection, id);
      
      case ConsistencyLevel.READ_YOUR_WRITES:
        // Check if we recently wrote
        return await consistentReader.read(collection, id);
      
      case ConsistencyLevel.MONOTONIC_READS:
        // Read from same replica (sticky)
        return await this.readFromStickyReplica(collection, id);
      
      case ConsistencyLevel.STRONG:
        // Read from master (slowest, most consistent)
        return await this.readFromMaster(collection, id);
    }
  }
}
```

---

## Monitoring & Alerts

```typescript
/**
 * Detect and alert on consistency issues
 */

interface ConsistencyMetrics {
  versionConflicts: number;      // Optimistic lock failures
  lockTimeouts: number;          // Failed to acquire lock
  staleReads: number;            // Read outdated data
  duplicateOperations: number;   // Idempotency violations
  messageGaps: number;           // Out-of-order messages
  replicationLag: number;        // Master-replica delay (ms)
}

class ConsistencyMonitor {
  async getMetrics(): Promise<ConsistencyMetrics> {
    return {
      versionConflicts: await this.countConflicts(),
      lockTimeouts: await this.countTimeouts(),
      staleReads: await this.detectStaleReads(),
      duplicateOperations: await this.countDuplicates(),
      messageGaps: await this.detectGaps(),
      replicationLag: await this.measureReplicationLag()
    };
  }
  
  async alert(metric: keyof ConsistencyMetrics, threshold: number) {
    const value = await this.getMetricValue(metric);
    
    if (value > threshold) {
      console.error(`🚨 ${metric} exceeded threshold: ${value} > ${threshold}`);
      
      // Send alert to monitoring system
      await this.sendAlert({
        severity: 'high',
        metric,
        value,
        threshold
      });
    }
  }
}
```

---

## Implementation Checklist

✅ **Distributed Locking**
- [ ] Redis-based pessimistic locks
- [ ] Optimistic locking with versions
- [ ] Lock timeout & cleanup
- [ ] Deadlock detection

✅ **Sequencing**
- [ ] Message sequence numbers
- [ ] Client-side ordering buffer
- [ ] Gap detection & repair
- [ ] Out-of-order handling

✅ **Consistency Guarantees**
- [ ] Read-after-write consistency
- [ ] Session pinning (sticky routing)
- [ ] Write-through caching
- [ ] Replication lag monitoring

✅ **Idempotency**
- [ ] Token-based deduplication
- [ ] Result caching (24h TTL)
- [ ] Retry safety
- [ ] Double-submit prevention

✅ **Conflict Resolution**
- [ ] Last-write-wins (LWW)
- [ ] Smart merging (CRDTs)
- [ ] Application-level logic
- [ ] User-facing conflict UI

✅ **Monitoring**
- [ ] Version conflict rate
- [ ] Lock contention metrics
- [ ] Stale read detection
- [ ] Replication lag alerts

---

**Document Version**: 1.0  
**Last Updated**: January 30, 2026  
**Status**: Architecture Approved ✅
