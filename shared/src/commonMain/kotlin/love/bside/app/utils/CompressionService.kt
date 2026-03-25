package love.bside.app.utils

import arrow.core.Option

/**
 * Shared service for extreme data compression using Brotli + Base64.
 * Ported from the monadic architecture pattern used in industrial-scale text optimization.
 */
expect object CompressionService {
    /**
     * Compresses [rawPayload] using Brotli and encodes the result as Base64.
     */
    suspend fun compressToBase64(rawPayload: Option<String>): Option<String>

    /**
     * Decodes Base64 and decompresses the resulting Brotli payload.
     * Fallback: returns the original [base64Payload] if decompression fails (legacy compatibility).
     */
    suspend fun decompressFromBase64(base64Payload: Option<String>): Option<String>
}
