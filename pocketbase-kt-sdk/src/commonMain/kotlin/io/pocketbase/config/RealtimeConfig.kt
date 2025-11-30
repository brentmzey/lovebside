package io.pocketbase.config

/**
 * Configuration for PocketBase realtime connectivity.
 */
data class RealtimeConfig(
    val mode: RealtimeMode = RealtimeMode.HYBRID,
    val smartPolling: SmartPollingConfig = SmartPollingConfig()
)

/**
 * Determines which transport to prefer for realtime communication.
 */
enum class RealtimeMode {
    /** Always use native SSE connections when available. */
    SSE_ONLY,
    /** Always use smart client-side polling (no SSE). */
    SMART_POLLING_ONLY,
    /** Prefer SSE but automatically fall back to smart polling. */
    HYBRID
}

/**
 * Tunable values for the smart polling transport.
 */
data class SmartPollingConfig(
    val initialDelayMs: Long = 1_200L,
    val minDelayMs: Long = 1_200L,
    val maxDelayMs: Long = 12_000L,
    val jitterRatio: Double = 0.25,
    val deleteAfterMisses: Int = 3,
    val activationThreshold: Int = 3,
    val batchSize: Int = 200
)

/**
 * Indicates which transport is currently active for realtime delivery.
 */
enum class RealtimeTransportKind {
    SSE,
    SMART_POLLING,
    INACTIVE
}
