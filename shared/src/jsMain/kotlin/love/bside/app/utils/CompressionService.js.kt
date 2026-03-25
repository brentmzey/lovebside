package love.bside.app.utils

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.some

actual object CompressionService {
    actual suspend fun compressToBase64(rawPayload: Option<String>): Option<String> {
        val text = rawPayload.getOrElse { return none() }
        if (text.isEmpty()) return none()

        return try {
            // Dynamic access to Node.js built-ins to avoid compile-time JsModule issues if browser target is also active
            val zlib = js("require('zlib')")
            val buffer = js("Buffer.from(text, 'utf-8')")
            
            // Avoid computed property name in js block for better compatibility
            val options = js("({ params: {} })")
            val brotliQualityKey = zlib.constants.BROTLI_PARAM_QUALITY
            val brotliMaxQuality = zlib.constants.BROTLI_MAX_QUALITY
            options.params[brotliQualityKey] = brotliMaxQuality
            
            val compressed = zlib.brotliCompressSync(buffer, options)
            (compressed.toString("base64") as String).some()
        } catch (e: Exception) {
            // Fallback to simple Base64 if zlib is unavailable (e.g. browser)
            try {
                js("btoa(text)").toString().some()
            } catch (e2: Exception) {
                text.some()
            }
        }
    }

    actual suspend fun decompressFromBase64(base64Payload: Option<String>): Option<String> {
        val base64 = base64Payload.getOrElse { return none() }
        if (base64.isEmpty()) return none()

        return try {
            val zlib = js("require('zlib')")
            val buffer = js("Buffer.from(base64, 'base64')")
            val decompressed = zlib.brotliDecompressSync(buffer, js("{}"))
            (decompressed.toString("utf-8") as String).some()
        } catch (e: Exception) {
            // Fallback to simple Base64 decoding if zlib is unavailable
            try {
                js("atob(base64)").toString().some()
            } catch (e2: Exception) {
                base64.some()
            }
        }
    }
}
