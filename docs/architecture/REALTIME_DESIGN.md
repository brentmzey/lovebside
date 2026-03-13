# Real-Time Design Philosophy

## Core Principle: Real-Time is ALWAYS Enabled

Real-time subscriptions are **ALWAYS active** in this application. We never disable them for synchronicity or consistency concerns.

## Why Always-On?

1. **Best User Experience**: Users see updates immediately from all sources
2. **Simplified State Management**: No complex enable/disable logic to maintain
3. **Network Resilience**: SDK handles reconnection and transport fallback automatically
4. **Scalability**: Designed for high-concurrency scenarios

## Handling Write-Then-Read Scenarios

Instead of disabling real-time, we use these patterns:

### 1. Optimistic Updates
```kotlin
// UI updates immediately on write
suspend fun sendMessage(text: String) {
    val optimisticMessage = createOptimisticMessage(text)
    _messages.update { it + optimisticMessage }
    
    // Actual write happens in background
    val result = repository.sendMessage(conversationId, text)
    
    // Real-time will reconcile any differences
}
```

### 2. Eventual Consistency
- Real-time updates reconcile any discrepancies
- Conflicts resolved by "last write wins" with timestamps
- UI shows loading states during reconciliation

### 3. Rate Limiting
```kotlin
// Prevent overwhelming the backend
private val rateLimiter = RateLimiter(
    maxRequests = 60,
    timeWindow = 60.seconds,
    burstCapacity = 5
)
```

### 4. Write Serialization
```kotlin
// Single-writer mutex prevents conflicts
private val writeMutex = Mutex()

suspend fun sendMessage(...) = writeMutex.withLock {
    // Only one write at a time
}
```

## Architecture Components

### RealtimeService
- Always active once started
- Automatically reconnects on failures
- Falls back to smart polling when WebSocket unavailable
- Never disabled by application code

### PocketBaseMessagingRepository
- Uses optimistic updates for writes
- Subscribes to real-time updates on init
- Reconciles optimistic vs. actual data via real-time

### Smart Polling Engine
- Automatically activates when SSE unavailable
- Efficient change detection via timestamps
- Configurable polling intervals with jitter

## Configuration

Real-time behavior is configured at SDK initialization:

```kotlin
val pocketBase = PocketBase(
    baseURL = url,
    realtimeConfig = RealtimeConfig(
        mode = RealtimeMode.HYBRID,  // SSE with smart polling fallback
        smartPolling = SmartPollingConfig(
            initialDelayMs = 1_200,
            minDelayMs = 1_000,
            maxDelayMs = 12_000,
            jitterRatio = 0.30,
            activationThreshold = 2  // Switch to polling after 2 SSE failures
        )
    )
)
```

## Trade-offs

### Benefits
✅ Immediate updates for all users  
✅ Simple mental model (always on)  
✅ Better UX (no stale data)  
✅ Network-resilient (automatic fallback)  

### Considerations
⚠️ Requires careful conflict resolution  
⚠️ Need robust error handling  
⚠️ Backend must support concurrent writes  

## Anti-Patterns to Avoid

❌ **Don't**: Disable real-time to ensure fresh reads after writes  
✅ **Do**: Use optimistic updates + eventual consistency

❌ **Don't**: Toggle real-time on/off based on user actions  
✅ **Do**: Keep it always on, manage state via optimistic updates

❌ **Don't**: Wait for write confirmation before showing changes  
✅ **Do**: Show changes immediately, reconcile via real-time

## Testing

Real-time behavior is testable via:
- Mock `RealtimeService` for unit tests
- Network simulation for integration tests
- Manual testing with multiple clients

Example test:
```kotlin
@Test
fun `messages appear immediately via optimistic update`() = runTest {
    val viewModel = ChatViewModel(repository)
    
    viewModel.sendMessage("Hello")
    
    // Should see message immediately (optimistic)
    assertEquals(1, viewModel.messages.value.size)
    
    // Real-time reconciles when server responds
    advanceTimeBy(100)
    assertEquals(1, viewModel.messages.value.size)  // Still 1, reconciled
}
```

## Summary

Real-time is a first-class citizen, not an optional feature. By keeping it always active and using proven patterns like optimistic updates and eventual consistency, we provide the best possible user experience while maintaining data integrity.
