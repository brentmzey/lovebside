import { Queue, Worker, Job } from 'bullmq';
import Redis from 'ioredis';

/**
 * Intelligent Job Orchestrator — type-safe job payloads per JobType.
 *
 * Features: priority queues, load-aware scheduling, circuit breaker.
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
  USER_AUTH = 'user_auth',
  MESSAGE_SEND = 'message_send',
  PROFILE_UPDATE = 'profile_update',
  SWIPE_ACTION = 'swipe_action',
  MATCH_SCORE_UPDATE = 'match_score_update',
  ANALYTICS_EVENT = 'analytics_event',
  FULL_MATCH_RECALC = 'full_match_recalc',
  DATA_CLEANUP = 'data_cleanup'
}

// ===== Typed Job Payloads =====

export interface UserAuthPayload {
  userId: string;
  action: 'login' | 'logout' | 'refresh';
}

export interface MessageSendPayload {
  conversationId: string;
  senderId: string;
  content: string;
  type: 'text' | 'image' | 'audio' | 'video' | 'file';
}

export interface ProfileUpdatePayload {
  userId: string;
  updatedFields: string[];
  trigger: 'user_edit' | 'admin_edit' | 'location_update';
}

export interface SwipeActionPayload {
  swiperId: string;
  targetId: string;
  direction: 'like' | 'pass' | 'superlike';
}

export interface MatchScoreUpdatePayload {
  userId: string;
  trigger: 'profile_updated' | 'interests_updated' | 'swipe' | 'scheduler';
  affectedUserIds?: string[];
}

export interface AnalyticsEventPayload {
  eventType: string;
  userId: string;
  properties: Record<string, string | number | boolean>;
}

export interface FullMatchRecalcPayload {
  chunkIndex: number;
  totalChunks: number;
  userIdBatch: string[];
}

export interface DataCleanupPayload {
  targetCollection: string;
  olderThanDays: number;
}

/** Discriminated union mapping JobType to its payload type */
export type JobPayloadMap = {
  [JobType.USER_AUTH]: UserAuthPayload;
  [JobType.MESSAGE_SEND]: MessageSendPayload;
  [JobType.PROFILE_UPDATE]: ProfileUpdatePayload;
  [JobType.SWIPE_ACTION]: SwipeActionPayload;
  [JobType.MATCH_SCORE_UPDATE]: MatchScoreUpdatePayload;
  [JobType.ANALYTICS_EVENT]: AnalyticsEventPayload;
  [JobType.FULL_MATCH_RECALC]: FullMatchRecalcPayload;
  [JobType.DATA_CLEANUP]: DataCleanupPayload;
};

export type JobPayload = JobPayloadMap[JobType];

/** Internal job envelope stored in the queue */
interface JobEnvelope<T extends JobType> {
  type: T;
  priority: JobPriority;
  data: JobPayloadMap[T];
  estimatedDuration: number;
  queuedAt: number;
}

// ===== Time Window Config =====

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
    allowedJobTypes: [JobType.FULL_MATCH_RECALC, JobType.DATA_CLEANUP, JobType.ANALYTICS_EVENT],
    maxConcurrency: 10
  },
  {
    name: 'EARLY_MORNING',
    hours: [6, 9],
    allowedJobTypes: [JobType.MATCH_SCORE_UPDATE, JobType.ANALYTICS_EVENT],
    maxConcurrency: 5
  },
  {
    name: 'PEAK_HOURS',
    hours: [9, 23],
    allowedJobTypes: [JobType.USER_AUTH, JobType.MESSAGE_SEND, JobType.PROFILE_UPDATE, JobType.SWIPE_ACTION],
    maxConcurrency: 2
  },
  {
    name: 'EVENING',
    hours: [23, 2],
    allowedJobTypes: [JobType.MATCH_SCORE_UPDATE, JobType.ANALYTICS_EVENT],
    maxConcurrency: 4
  }
];

// ===== Orchestrator =====

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

  private initializeQueues(): void {
    const connection = { host: 'redis-master', port: 6379 };

    const priorities: { level: JobPriority; concurrency: number }[] = [
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

  async queueJob<T extends JobType>(
    type: T,
    priority: JobPriority,
    data: JobPayloadMap[T],
    estimatedDuration: number = 100
  ): Promise<void> {
    const shouldRun = await this.shouldRunJob(type, priority, estimatedDuration);

    if (!shouldRun && priority >= JobPriority.MEDIUM) {
      console.log(`⏸️  Job ${type} deferred (load too high)`);
      await this.deferJob(type, priority, data);
      return;
    }

    const queue = this.queues.get(priority);
    const envelope: JobEnvelope<T> = { type, priority, data, estimatedDuration, queuedAt: Date.now() };

    await queue?.add(type, envelope, {
      priority,
      attempts: this.getRetryAttempts(priority),
      backoff: { type: 'exponential', delay: 2000 }
    });

    console.log(`✅ Queued ${type} (priority: ${JobPriority[priority]})`);
  }

  private async shouldRunJob(type: JobType, priority: JobPriority, estimatedDuration: number): Promise<boolean> {
    if (priority === JobPriority.CRITICAL) return true;

    const timeWindow = this.getCurrentTimeWindow();
    if (!timeWindow.allowedJobTypes.includes(type)) return false;

    if (this.circuitBreakerState === 'OPEN' && priority >= JobPriority.MEDIUM) return false;

    const metrics = await this.getSystemMetrics();

    if (priority === JobPriority.BACKGROUND) {
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

    return metrics.isHealthy;
  }

  private getCurrentTimeWindow(): TimeWindow {
    const hour = new Date().getUTCHours();
    for (const window of TIME_WINDOWS) {
      const [start, end] = window.hours;
      if (start < end) {
        if (hour >= start && hour < end) return window;
      } else {
        if (hour >= start || hour < end) return window;
      }
    }
    return TIME_WINDOWS[2]; // Default: PEAK_HOURS
  }

  private async getSystemMetrics(): Promise<SystemMetrics> {
    const queueDepths = await Promise.all(Array.from(this.queues.values()).map(q => q.count()));
    const totalQueueDepth = queueDepths.reduce((a: number, b: number) => a + b, 0);

    return {
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
  }

  private async processJob(job: Job): Promise<void> {
    const envelope = job.data as JobEnvelope<JobType>;
    console.log(`▶️  Processing ${envelope.type} (${JobPriority[envelope.priority]})`);

    try {
      switch (envelope.type) {
        case JobType.MATCH_SCORE_UPDATE:
          await this.handleMatchUpdate(envelope.data as MatchScoreUpdatePayload);
          break;
        case JobType.FULL_MATCH_RECALC:
          await this.handleFullRecalc(envelope.data as FullMatchRecalcPayload);
          break;
        case JobType.PROFILE_UPDATE:
          await this.handleProfileUpdate(envelope.data as ProfileUpdatePayload);
          break;
        default:
          console.warn(`⚠️  Unhandled job type: ${envelope.type}`);
      }

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
        setTimeout(() => {
          this.circuitBreakerState = 'HALF_OPEN';
          console.log('🟡 Circuit breaker HALF_OPEN');
        }, 60000);
      }
      throw error;
    }
  }

  private async handleMatchUpdate(data: MatchScoreUpdatePayload): Promise<void> {
    console.log(`🎯 Updating match scores for user ${data.userId} (trigger: ${data.trigger})`);
    // Implementation in cron_matching_v2.ts
  }

  private async handleFullRecalc(data: FullMatchRecalcPayload): Promise<void> {
    console.log(`🔄 Full recalc chunk ${data.chunkIndex}/${data.totalChunks} (${data.userIdBatch.length} users)`);
    // Implementation in cron_matching_v2.ts
  }

  private async handleProfileUpdate(data: ProfileUpdatePayload): Promise<void> {
    console.log(`👤 Profile updated: ${data.userId} (trigger: ${data.trigger}, fields: ${data.updatedFields.join(', ')})`);
    await this.queueJob(
      JobType.MATCH_SCORE_UPDATE,
      JobPriority.HIGH,
      { userId: data.userId, trigger: 'profile_updated', affectedUserIds: [] }
    );
  }

  private async deferJob<T extends JobType>(type: T, priority: JobPriority, data: JobPayloadMap[T]): Promise<void> {
    const nextWindow = this.getNextAllowedWindow(type);
    const delayMs = this.getMillisecondsUntilWindow(nextWindow);
    await this.redis.zadd('jobs:deferred', Date.now() + delayMs, JSON.stringify({ type, priority, data }));
  }

  private getNextAllowedWindow(type: JobType): TimeWindow {
    const currentHour = new Date().getUTCHours();
    for (const window of TIME_WINDOWS) {
      if (window.allowedJobTypes.includes(type)) {
        const [start] = window.hours;
        if (start > currentHour) return window;
      }
    }
    return TIME_WINDOWS[0]; // Next day DEEP_NIGHT
  }

  private getMillisecondsUntilWindow(window: TimeWindow): number {
    const now = new Date();
    const [targetHour] = window.hours;
    const target = new Date(now);
    target.setUTCHours(targetHour, 0, 0, 0);
    if (target < now) target.setDate(target.getDate() + 1);
    return target.getTime() - now.getTime();
  }

  private getRetryAttempts(priority: JobPriority): number {
    switch (priority) {
      case JobPriority.CRITICAL: return 5;
      case JobPriority.HIGH: return 3;
      case JobPriority.MEDIUM: return 2;
      default: return 1;
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
        return { priority: JobPriority[priority], waiting, active, completed, failed };
      })
    );

    const metrics = await this.getSystemMetrics();
    const timeWindow = this.getCurrentTimeWindow();

    return { timeWindow: timeWindow.name, circuitBreaker: this.circuitBreakerState, metrics, queues: stats };
  }
}

export const orchestrator = new JobOrchestrator(
  process.env.REDIS_URL ?? 'redis://localhost:6379'
);
