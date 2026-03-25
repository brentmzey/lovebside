package love.bside.app.utils

import com.aayushatharva.brotli4j.Brotli4jLoader
import com.aayushatharva.brotli4j.encoder.Encoder
import com.aayushatharva.brotli4j.decoder.Decoder
import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.some
import java.util.Base64

actual object CompressionService {

    init {
        // Ensure native libraries are loaded
        Brotli4jLoader.ensureAvailability()
    }

    actual suspend fun compressToBase64(rawPayload: Option<String>): Option<String> {
        val text = rawPayload.getOrElse { return none() }
        if (text.isEmpty()) return none()

        return try {
            val inputBytes = text.toByteArray(Charsets.UTF_8)
            val params = Encoder.Parameters().setQuality(11)
            val compressedBytes = Encoder.compress(inputBytes, params)
            Base64.getEncoder().encodeToString(compressedBytes).some()
        } catch (e: Exception) {
            println("⚠️ JVM Compression failed: ${e.message}")
            none()
        }
    }

    actual suspend fun decompressFromBase64(base64Payload: Option<String>): Option<String> {
        val base64 = base64Payload.getOrElse { return none() }
        if (base64.isEmpty()) return none()

        return try {
            val compressedBytes = Base64.getDecoder().decode(base64)
            val decompressed = Decoder.decompress(compressedBytes)
            val data = decompressed.decompressedData
            if (data != null) {
                String(data, Charsets.UTF_8).some()
            } else {
                base64.some()
            }
        } catch (e: Exception) {
            // Defensive fallback: if input was actually raw uncompressed text
            println("⚠️ JVM Decompression failed, falling back to raw: ${e.message}")
            base64.some()
        }
    }
}
