package love.bside.app.utils

import arrow.core.Option
import arrow.core.getOrElse
import arrow.core.none
import arrow.core.some
import platform.Foundation.*

actual object CompressionService {
    actual suspend fun compressToBase64(rawPayload: Option<String>): Option<String> {
        val text = rawPayload.getOrElse { return none() }
        if (text.isEmpty()) return none()
        
        // Basic Base64 implementation for iOS as a placeholder for full Brotli C-interop
        val data = (text as NSString).dataUsingEncoding(NSUTF8StringEncoding)
        return data?.base64EncodedStringWithOptions(0UL)?.some() ?: text.some()
    }

    actual suspend fun decompressFromBase64(base64Payload: Option<String>): Option<String> {
        val base64 = base64Payload.getOrElse { return none() }
        if (base64.isEmpty()) return none()
        
        val data = NSData.create(base64EncodedString = base64, options = 0UL)
        return if (data != null) {
            val decodedString = NSString.create(data = data, encoding = NSUTF8StringEncoding)
            decodedString?.toString()?.some() ?: base64.some()
        } else {
            base64.some()
        }
    }
}
