import { Queue, Worker, Job } from 'bullmq';
import Redis from 'ioredis';

/**
 * Intelligent Job Orchestrator
 * 
 * Features:
 * - Priority-based queues (5 tiers)
 * - Load-aware scheduling
 * - Time-window based execution
 * - Circuit breaker pattern
 * - Write coordination
 * 
 * See: .code-hq/JOB_ORCHESTRATION_STRATEGY.md
 */

export enum JobPriority {
  CRITICAL = 0,
  HIGH = 1,
  MEDIUM = 2,
  LOW = 3,
  BACKGROUND = 4
}

export enum JobType {
  // CRITICAL
  USER_AUTH = 'user_auth',
  MESSAGE_SEND = 'message_send',
  
  // HIGH
  PROFILE_UPDATE = 'profile_update',
  SWIPE_ACTION = 'swipe_action',
  
  // MEDIUM
  MATCH_SCORE_UPDATE = 'match_score_update',
  
  // LOW
  ANALYTICS_EVENT = 'analytics_event',
  
  // BACKGROUND
  FULL_MATCH_RECALC = 'full_match_recalc',
  DATA_CLEANUP = 'data_cleanup'
}

interface SystemMetrics {
  sqliteWriteQueueDepth: number;
  sqliteWriteLatency: number;
  apiRequestRate: number;
  apiErrorRate: number;
  apiLatencyP95: number;
  cpuUsage: number;
  memoryUsage: number;
  isHealthy: boolean;
  loadLevel: 'low' | 'medium' | 'high' | 'critical';
}

interface TimeWindow {
  name: string;
  hours: [number, number];
  allowedJobTypes: JobType[];
  maxConcurrency: number;
}

const TIME_WINDOWS: TimeWindow[] = [
  {
    name: 'DEEP_NIGHT',
    hours: [2, 6],
    allowedJobTypes: [
      JobType.FULL_MATCH_RECALC,
      JobType.DATA_CLEANUP,
      JobType.ANALYTICS_EVENT
    ],
    maxConcurrency: 10
  },
  {
    name: 'EARLY_MORNING',
    hours: [6, 9],
    allowedJobTypes: [
      JobType.MATCH_SCORE_UPDATE,
      JobType.ANALYTICS_EVENT
    ],
    maxConcurrency: 5
  },
  {
    name: 'PEAK_HOURS',
    hours: [9, 23],
    allowedJobTypes: [
      JobType.USER_AUTH,
      JobType.MESSAGE_SEND,
      JobType.PROFILE_UPDATE,
      JobType.SWIPE_ACTION
    ],
    maxConcurrency: 2
  },
  {
    name: 'EVENING',
    hours: [23, 2],
    allowedJobTypes: [
      JobType.MATCH_SCORE_UPDATE,
      JobType.ANALYTICS_EVENT
    ],
    maxConcurrency: 4
  }
];

export class JobOrchestrator {
  private redis: Redis;
  private queues: Map<JobPriority, Queue>;
  private workers: Map<JobPriority, Worker>;
  private circuitBreakerState: 'CLOSED' | 'OPEN' | 'HALF_OPEN' = 'CLOSED';
  private failureCount = 0;
  
  constructor(redisUrl: string) {
    this.redis = new Redis(redisUrl);
    this.queues = new Map();
    this.workers = new Map();
    this.initializeQueues();
  }
  
  private initializeQueues() {
    const connection = { host: 'redis-master', port: 6379 };
    
    // Create queues for each priority level
    const priorities = [
      { level: JobPriority.CRITICAL, concurrency: 50 },
      { level: JobPriority.HIGH, concurrency: 20 },
      { level: JobPriority.MEDIUM, concurrency: 10 },
      { level: JobPriority.LOW, concurrency: 5 },
      { level: JobPriority.BACKGROUND, concurrency: 2 }
    ];
    
    for (const { level, concurrency } of priorities) {
      const queue = new Queue(`priority-${level}`, { connection });
      const worker = new Worker(
        `priority-${level}`,
        async (job: Job) => this.processJob(job),
        { connection, concurrency }
      );
      
      this.queues.set(level, queue);
      this.workers.set(level, worker);
    }
  }
  
  async queueJob(
    type: JobType,
    priority: JobPriority,
    data: any,
    estimatedDuration: number = 100
  ) {
    // Check if job should run now
    const shouldRun = await this.shouldRunJob(type, priority, estimatedDuration);
    
    if (!shouldRun && priority >= JobPriority.MEDIUM) {
      console.log(`⏸️  Job ${type} deferred (load too high)`);
      await this.deferJob(type, priority, data);
      return;
    }
    
    // Add to appropriate priority queue
    const queue = this.queues.get(priority);
    await queue?.add(type, {
      type,
      priority,
      data,
      estimatedDuration,
      queuedAt: Date.now()
    }, {
      priority: priority,
      attempts: this.getRetryAttempts(priority),
      backoff: {
        type: 'exponential',
        delay: 2000
      }
    });
    
    console.log(`✅ Queued ${type} (priority: ${JobPriority[priority]})`);
  }
  
  private async shouldRunJob(
    type: JobType,
    priority: JobPriority,
    estimatedDuration: number
  ): Promise<boolean> {
    // Critical jobs always run
    if (priority === JobPriority.CRITICAL) {
      return true;
    }
    
    // Check time window
    const timeWindow = this.getCurrentTimeWindow();
    if (!timeWindow.allowedJobTypes.includes(type)) {
      return false;
    }
    
    // Check circuit breaker
    if (this.circuitBreakerState === 'OPEN' && priority >= JobPriority.MEDIUM) {
      return false;
    }
    
    // Check system load
    const metrics = await this.getSystemMetrics();
    
    if (priority === JobPriority.BACKGROUND) {
      // Background: All checks must pass
      return (
        metrics.sqliteWriteQueueDepth < 100 &&
        metrics.apiLatencyP95 < 300 &&
        metrics.cpuUsage < 60 &&
        metrics.loadLevel === 'low'
      );
    }
    
    if (priority === JobPriority.LOW) {
      return (
        metrics.sqliteWriteQueueDepth < 500 &&
        metrics.apiLatencyP95 < 500 &&
        metrics.loadLevel !== 'critical'
      );
    }
    
    // Medium/High: Basic health check
    return metrics.isHealthy;
  }
  
  private getCurrentTimeWindow(): TimeWindow {
    const hour = new Date().getUTCHours();
    
    for (const window of TIME_WINDOWS) {
      const [start, end] = window.hours;
      
      if (start < end) {
        if (hour >= start && hour < end) {
          return window;
        }
      } else {
        // Wraps midnight
        if (hour >= start || hour < end) {
          return window;
        }
      }
    }
    
    return TIME_WINDOWS[2]; // Default to PEAK_HOURS
  }
  
  private async getSystemMetrics(): Promise<SystemMetrics> {
    // In production, query Prometheus/DataDog
    // For now, simulate with Redis metrics
    
    const queueDepths = await Promise.all(
      Array.from(this.queues.values()).map(q => q.count())
    );
    
    const totalQueueDepth = queueDepths.reduce((a, b) => a + b, 0);
    
    // Simulate metrics (replace with real monitoring)
    const metrics: SystemMetrics = {
      sqliteWriteQueueDepth: totalQueueDepth,
      sqliteWriteLatency: 10,
      apiRequestRate: 1000,
      apiErrorRate: 0.001,
      apiLatencyP95: 200,
      cpuUsage: 50,
      memoryUsage: 60,
      isHealthy: totalQueueDepth < 1000,
      loadLevel: totalQueueDepth > 500 ? 'high' : 'low'
    };
    
    return metrics;
  }
  
  private async processJob(job: Job) {
    const { type, data, priority } = job.data;
    
    console.log(`▶️  Processing ${type} (${JobPriority[priority]})`);
    
    try {
      // Route to appropriate handler
      switch (type) {
        case JobType.MATCH_SCORE_UPDATE:
          await this.handleMatchUpdate(data);
          break;
        case JobType.FULL_MATCH_RECALC:
          await this.handleFullRecalc(data);
          break;
        case JobType.PROFILE_UPDATE:
          await this.handleProfileUpdate(data);
          break;
        default:
          console.warn(`⚠️  Unknown job type: ${type}`);
      }
      
      // Reset circuit breaker on success
      if (this.circuitBreakerState === 'HALF_OPEN') {
        this.circuitBreakerState = 'CLOSED';
        this.failureCount = 0;
        console.log('✅ Circuit breaker CLOSED');
      }
      
    } catch (error) {
      this.failureCount++;
      
      if (this.failureCount >= 5) {
        this.circuitBreakerState = 'OPEN';
        console.log('🔴 Circuit breaker OPEN');
        
        // Auto-recovery after 1 minute
        setTimeout(() => {
          this.circuitBreakerState = 'HALF_OPEN';
          console.log('🟡 Circuit breaker HALF_OPEN');
        }, 60000);
      }
      
      throw error;
    }
  }
  
  private async handleMatchUpdate(data: any) {
    // Incremental match score update
    console.log(`🎯 Updating match scores for user ${data.userId}`);
    // Implementation in cron_matching_v2.ts
  }
  
  private async handleFullRecalc(data: any) {
    // Full match recalculation (off-peak only)
    console.log(`🔄 Full recalc chunk ${data.chunkIndex}/${data.totalChunks}`);
    // Implementation in cron_matching_v2.ts
  }
  
  private async handleProfileUpdate(data: any) {
    // Profile update triggers match score recalc
    console.log(`👤 Profile updated: ${data.userId}`);
    // Queue match updates for affected users
    await this.queueJob(
      JobType.MATCH_SCORE_UPDATE,
      JobPriority.HIGH,
      { userId: data.userId, trigger: 'profile_updated' },
      50
    );
  }
  
  private async deferJob(type: JobType, priority: JobPriority, data: any) {
    // Store in Redis sorted set for later execution
    const nextWindow = this.getNextAllowedWindow(type);
    const delayMs = this.getMillisecondsUntilWindow(nextWindow);
    
    await this.redis.zadd(
      'jobs:deferred',
      Date.now() + delayMs,
      JSON.stringify({ type, priority, data })
    );
  }
  
  private getNextAllowedWindow(type: JobType): TimeWindow {
    const currentHour = new Date().getUTCHours();
    
    for (const window of TIME_WINDOWS) {
      if (window.allowedJobTypes.includes(type)) {
        const [start] = window.hours;
        if (start > currentHour) {
          return window;
        }
      }
    }
    
    return TIME_WINDOWS[0]; // Next day DEEP_NIGHT
  }
  
  private getMillisecondsUntilWindow(window: TimeWindow): number {
    const now = new Date();
    const [targetHour] = window.hours;
    
    const target = new Date(now);
    target.setUTCHours(targetHour, 0, 0, 0);
    
    if (target < now) {
      target.setDate(target.getDate() + 1);
    }
    
    return target.getTime() - now.getTime();
  }
  
  private getRetryAttempts(priority: JobPriority): number {
    switch (priority) {
      case JobPriority.CRITICAL:
        return 5;
      case JobPriority.HIGH:
        return 3;
      case JobPriority.MEDIUM:
        return 2;
      default:
        return 1;
    }
  }
  
  async getStats() {
    const stats = await Promise.all(
      Array.from(this.queues.entries()).map(async ([priority, queue]) => {
        const [waiting, active, completed, failed] = await Promise.all([
          queue.getWaitingCount(),
          queue.getActiveCount(),
          queue.getCompletedCount(),
          queue.getFailedCount()
        ]);
        
        return {
          priority: JobPriority[priority],
          waiting,
          active,
          completed,
          failed
        };
      })
    );
    
    const metrics = await this.getSystemMetrics();
    const timeWindow = this.getCurrentTimeWindow();
    
    return {
      timeWindow: timeWindow.name,
      circuitBreaker: this.circuitBreakerState,
      metrics,
      queues: stats
    };
  }
}

// Singleton instance
export const orchestrator = new JobOrchestrator(
  process.env.REDIS_URL || 'redis://localhost:6379'
);
