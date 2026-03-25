package love.bside.app.utils

import arrow.core.some
import arrow.core.getOrElse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class CompressionIntegrationTest {

    private fun String.truncate(limit: Int = 150): String {
        return if (this.length > limit) this.take(limit) + "... [TRUNCATED]" else this
    }

    private fun printSection(title: String) {
        println("\n    ┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("    ┃ $title")
        println("    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }

    private fun printStat(label: String, value: String) {
        val paddedLabel = label.padEnd(20)
        println("    ┃ $paddedLabel │ $value")
    }

    @Test
    fun verifyExtremeCompressionMetrics() = runTest {
        val massiveText = """
            The social engine of BSide requires high-fidelity data structures. 
            When we store user bios, essays, and long-form content, we must optimize for storage.
            Brotli at quality level 11 provides industry-leading compression ratios.
            This test verifies that our shared Kotlin service achieves these goals.
        """.trimIndent().repeat(100)
        
        printSection("🧪 EXTREME COMPRESSION METRICS")
        printStat("Input Preview", massiveText.truncate(100))
        printStat("Original Size", "${massiveText.length} bytes")
        
        // 1. Compress
        val compressedOption = CompressionService.compressToBase64(massiveText.some())
        val compressed = compressedOption.getOrElse { "" }
        val compressedSize = compressed.length
        
        val ratio = if (massiveText.isNotEmpty()) {
            ((1.0 - compressedSize.toDouble() / massiveText.length.toDouble()) * 100).toInt()
        } else 0
        
        printStat("Compressed Preview", compressed.truncate(100))
        printStat("Compressed Size", "$compressedSize bytes (Base64 Brotli)")
        printStat("Gain", "$ratio% reduction")
        
        // 2. Decompress
        val decompressedOption = CompressionService.decompressFromBase64(compressed.some())
        val decompressed = decompressedOption.getOrElse { "" }
        
        printStat("Decompressed Size", "${decompressed.length} bytes")
        printStat("Parity Match", if (decompressed == massiveText) "✅ YES" else "❌ NO")
        
        // 3. XSS Test
        val xssPayload = "<script>alert('pwned')</script> This is safe text."
        val compressedXss = CompressionService.compressToBase64(xssPayload.some())
        val decompressedXss = CompressionService.decompressFromBase64(compressedXss)
        val finalXss = decompressedXss.getOrElse { "" }
        
        printStat("XSS Roundtrip", finalXss)
        println("    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\n")
        
        assertEquals(massiveText, decompressed)
        assertTrue(compressedSize <= massiveText.length)
        assertEquals(xssPayload, finalXss)
    }

    @Test
    fun verifyFullCompressionLifecycleFlow() = runTest {
        printSection("🔄 FULL COMPRESSION LIFECYCLE FLOW")
        
        // 1. Human Readable Massive Text
        val testText = "MASSIVE ".repeat(3000) + "END"
        val originalSize = testText.length
        printStat("[1] Input", testText.truncate(120))
        printStat("    Size", "$originalSize bytes")
        
        // 2. Compress Brotli Base64
        val compressed = CompressionService.compressToBase64(testText.some())
        val compressedStr = compressed.getOrElse { "" }
        val compressedSize = compressedStr.length
        
        printStat("[2] Compressed", compressedStr.truncate(120))
        printStat("    Size", "$compressedSize bytes")
        
        val gain = if (originalSize > 0) {
            ((originalSize - compressedSize).toDouble() / originalSize * 100).toInt()
        } else 0
        printStat("[3] DB Savings", "$gain% (Value stored as optimized text)")
        
        // 3. Decompress
        val decompressed = CompressionService.decompressFromBase64(compressed)
        val decompressedVal = decompressed.getOrElse { "" }
        
        printStat("[4] Retrieval", "Pulling from DB...")
        printStat("[5] Decompressed", decompressedVal.truncate(120))
        printStat("[6] Integrity", if (decompressedVal == testText) "✅ VERIFIED (100% Match)" else "❌ CORRUPTED")
        
        assertEquals(testText, decompressedVal, "Decompressed value must match original")
        
        println("    ┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛\n")
    }
}
