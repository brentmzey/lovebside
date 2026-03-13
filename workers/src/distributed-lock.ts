import Redis from 'ioredis';
import { v4 as uuidv4 } from 'uuid';

/**
 * Distributed Locking Service
 * 
 * Prevents race conditions across multiple server instances
 * 
 * Features:
 * - Optimistic locking (version checking)
 * - Pessimistic locking (Redis-based)
 * - Automatic lock expiration
 * - Deadlock prevention
 * - Idempotency tokens
 * 
 * See: .code-hq/CONSISTENCY_STRATEGY.md
 */

export interface VersionedEntity {
  id: string;
  version: number;
  updatedAt: string;
  [key: string]: any;
}

export interface LockResult<T> {
  success: boolean;
  data?: T;
  error?: string;
  currentVersion?: number;
}

export class DistributedLockService {
  private redis: Redis;
  private writeTimes: Map<string, number> = new Map();
  
  constructor(redisUrl: string) {
    this.redis = new Redis(redisUrl);
  }
  
  // ==========================================
  // OPTIMISTIC LOCKING (Version-Based)
  // ==========================================
  
  /**
   * Update with version check (no blocking)
   * 
   * Use for: High-throughput operations
   * - Profile updates
   * - Match score updates
   * - Analytics counters
   */
  async updateWithVersion<T extends VersionedEntity>(
    collection: string,
    id: string,
    expectedVersion: number,
    updates: Partial<T>,
    updateFn: (id: string, data: Partial<T> & { version: number }) => Promise<T>
  ): Promise<LockResult<T>> {
    try {
      // Prepare update with incremented version
      const updateData = {
        ...updates,
        version: expectedVersion + 1,
        updatedAt: new Date().toISOString()
      };
      
      // Execute update (will fail if version changed)
      const result = await updateFn(id, updateData);
      
      // Track write time for read-after-write consistency
      this.writeTimes.set(`${collection}:${id}`, Date.now());
      
      return {
        success: true,
        data: result
      };
      
    } catch (error: any) {
      // Version mismatch or concurrent update
      return {
        success: false,
        error: 'VERSION_CONFLICT',
      };
    }
  }
  
  /**
   * Retry optimistic update on conflict
   */
  async updateWithRetry<T extends VersionedEntity>(
    collection: string,
    id: string,
    updateFn: (current: T) => Partial<T>,
    executeFn: (id: string, data: Partial<T> & { version: number }) => Promise<T>,
    getFn: () => Promise<T>,
    maxRetries: number = 3
  ): Promise<LockResult<T>> {
    for (let attempt = 0; attempt < maxRetries; attempt++) {
      // Read current version
      const current = await getFn();
      
      // Compute updates
      const updates = updateFn(current);
      
      // Try to update
      const result = await this.updateWithVersion(
        collection,
        id,
        current.version,
        updates,
        executeFn
      );
      
      if (result.success) {
        return result;
      }
      
      // Conflict! Wait and retry
      await this.sleep(50 * Math.pow(2, attempt));  // Exponential backoff
    }
    
    return {
      success: false,
      error: 'MAX_RETRIES_EXCEEDED'
    };
  }
  
  // ==========================================
  // PESSIMISTIC LOCKING (Redis-Based)
  // ==========================================
  
  /**
   * Acquire exclusive lock before operation
   * 
   * Use for: Critical operations
   * - Swipe actions (prevent double-swipe)
   * - Match creation (prevent duplicates)
   * - Payment processing
   */
  async withLock<T>(
    lockKey: string,
    operation: () => Promise<T>,
    options: {
      ttl?: number;        // Lock timeout (ms)
      maxRetries?: number; // Max acquisition attempts
      retryDelay?: number; // Initial retry delay (ms)
    } = {}
  ): Promise<T> {
    const {
      ttl = 5000,
      maxRetries = 10,
      retryDelay = 50
    } = options;
    
    const lockValue = uuidv4();
    const lockPath = `lock:${lockKey}`;
    let retries = 0;
    
    while (retries < maxRetries) {
      // Try to acquire lock
      const acquired = await this.redis.set(
        lockPath,
        lockValue,
        'PX', ttl,   // Expire after ttl ms
        'NX'         // Only if not exists
      );
      
      if (acquired === 'OK') {
        try {
          // Lock acquired! Execute operation
          const result = await operation();
          return result;
        } finally {
          // Release lock (only if we still own it)
          await this.releaseLock(lockPath, lockValue);
        }
      }
      
      // Lock not acquired, wait and retry
      retries++;
      const delay = retryDelay * Math.pow(1.5, retries);  // Exponential backoff
      await this.sleep(delay + Math.random() * delay);  // Add jitter
    }
    
    throw new Error(`Failed to acquire lock: ${lockKey} after ${maxRetries} attempts`);
  }
  
  private async releaseLock(lockPath: string, lockValue: string): Promise<void> {
    // Lua script: Only delete if value matches (atomic)
    const script = `
      if redis.call("get", KEYS[1]) == ARGV[1] then
        return redis.call("del", KEYS[1])
      else
        return 0
      end
    `;
    
    await this.redis.eval(script, 1, lockPath, lockValue);
  }
  
  // ==========================================
  // IDEMPOTENCY (Token-Based)
  // ==========================================
  
  /**
   * Execute operation only once per token
   * 
   * Use for: User-facing actions
   * - Button double-clicks
   * - Network retries
   * - Mobile app reconnections
   */
  async executeIdempotent<T>(
    token: string,
    operation: () => Promise<T>,
    ttl: number = 86400  // Cache result for 24h
  ): Promise<T> {
    const key = `idempotency:${token}`;
    
    // Check if already processed
    const cached = await this.redis.get(key);
    if (cached) {
      console.log(`♻️  Idempotency hit: ${token}`);
      return JSON.parse(cached);
    }
    
    // Mark as in-progress
    const acquired = await this.redis.set(
      key,
      JSON.stringify({ status: 'PROCESSING' }),
      'EX', ttl,
      'NX'
    );
    
    if (!acquired) {
      // Another request with same token is in progress
      // Wait briefly and check again
      await this.sleep(100);
      return await this.executeIdempotent(token, operation, ttl);
    }
    
    try {
      // Execute operation
      const result = await operation();
      
      // Cache result
      await this.redis.set(
        key,
        JSON.stringify({ status: 'SUCCESS', result }),
        'EX', ttl
      );
      
      console.log(`✅ Idempotency: Executed ${token}`);
      return result;
      
    } catch (error) {
      // Operation failed, allow retry
      await this.redis.del(key);
      throw error;
    }
  }
  
  // ==========================================
  // READ-AFTER-WRITE CONSISTENCY
  // ==========================================
  
  /**
   * Check if entity was recently written by this client
   */
  wasRecentlyWritten(collection: string, id: string, windowMs: number = 2000): boolean {
    const writeTime = this.writeTimes.get(`${collection}:${id}`);
    if (!writeTime) {
      return false;
    }
    
    return (Date.now() - writeTime) < windowMs;
  }
  
  /**
   * Clean up old write timestamps
   */
  cleanupWriteTimes(maxAge: number = 60000): void {
    const now = Date.now();
    for (const [key, time] of this.writeTimes.entries()) {
      if (now - time > maxAge) {
        this.writeTimes.delete(key);
      }
    }
  }
  
  // ==========================================
  // MESSAGE SEQUENCING
  // ==========================================
  
  /**
   * Get next sequence number for conversation
   */
  async getNextSequence(conversationId: string): Promise<number> {
    return await this.withLock(
      `conversation:${conversationId}:seq`,
      async () => {
        const current = await this.redis.get(`seq:${conversationId}`);
        const next = parseInt(current || '0') + 1;
        await this.redis.set(`seq:${conversationId}`, next.toString());
        return next;
      }
    );
  }
  
  /**
   * Store message with sequence number
   */
  async storeSequencedMessage(
    conversationId: string,
    message: any
  ): Promise<number> {
    const sequenceNumber = await this.getNextSequence(conversationId);
    
    // Store message with sequence
    await this.redis.zadd(
      `messages:${conversationId}`,
      sequenceNumber,
      JSON.stringify({ ...message, sequenceNumber })
    );
    
    return sequenceNumber;
  }
  
  /**
   * Get messages in order
   */
  async getSequencedMessages(
    conversationId: string,
    fromSeq: number,
    toSeq: number
  ): Promise<any[]> {
    const messages = await this.redis.zrangebyscore(
      `messages:${conversationId}`,
      fromSeq,
      toSeq
    );
    
    return messages.map(m => JSON.parse(m));
  }
  
  // ==========================================
  // UTILITIES
  // ==========================================
  
  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
  
  async getMetrics() {
    const info = await this.redis.info('stats');
    const lines = info.split('\r\n');
    
    const metrics: any = {};
    for (const line of lines) {
      const [key, value] = line.split(':');
      if (key && value) {
        metrics[key] = value;
      }
    }
    
    return {
      totalConnections: metrics.total_connections_received,
      totalCommands: metrics.total_commands_processed,
      usedMemory: metrics.used_memory_human,
      connectedClients: metrics.connected_clients,
      blockedClients: metrics.blocked_clients
    };
  }
  
  async healthCheck(): Promise<boolean> {
    try {
      const pong = await this.redis.ping();
      return pong === 'PONG';
    } catch (error) {
      return false;
    }
  }
}

// ==========================================
// CLIENT-SIDE MESSAGE BUFFER
// ==========================================

/**
 * Handle out-of-order message delivery
 */
export class MessageBuffer {
  private buffer: Map<number, any> = new Map();
  private nextExpected: number = 1;
  
  /**
   * Add message to buffer, return ready messages
   */
  addMessage(message: any): any[] {
    const { sequenceNumber } = message;
    
    // Already processed?
    if (sequenceNumber < this.nextExpected) {
      console.log(`⚠️  Duplicate message: seq=${sequenceNumber}`);
      return [];
    }
    
    // Add to buffer
    this.buffer.set(sequenceNumber, message);
    
    // Emit all consecutive messages
    const ready: any[] = [];
    while (this.buffer.has(this.nextExpected)) {
      const msg = this.buffer.get(this.nextExpected)!;
      ready.push(msg);
      this.buffer.delete(this.nextExpected);
      this.nextExpected++;
    }
    
    if (ready.length > 0) {
      console.log(`📨 Emitting ${ready.length} messages (seq ${ready[0].sequenceNumber}-${ready[ready.length-1].sequenceNumber})`);
    }
    
    return ready;
  }
  
  /**
   * Detect gaps in sequence
   */
  detectGaps(): number[] {
    const gaps: number[] = [];
    const bufferedSeqs = Array.from(this.buffer.keys()).sort((a, b) => a - b);
    
    if (bufferedSeqs.length === 0) {
      return gaps;
    }
    
    const maxBuffered = Math.max(...bufferedSeqs);
    for (let seq = this.nextExpected; seq < maxBuffered; seq++) {
      if (!this.buffer.has(seq)) {
        gaps.push(seq);
      }
    }
    
    return gaps;
  }
  
  /**
   * Get buffer status
   */
  getStatus() {
    return {
      nextExpected: this.nextExpected,
      buffered: this.buffer.size,
      gaps: this.detectGaps().length
    };
  }
}

// Singleton instance
export const lockService = new DistributedLockService(
  process.env.REDIS_URL || 'redis://localhost:6379'
);

// Cleanup old write timestamps every minute
setInterval(() => {
  lockService.cleanupWriteTimes();
}, 60000);
