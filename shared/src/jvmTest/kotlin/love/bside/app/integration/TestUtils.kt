package love.bside.app.integration

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

object TestUtils {
    /**
     * extracts the record model from an auth response which might vary in key name
     * depending on the auth type (admin vs user vs collection).
     */
    fun extractAuthRecord(response: JsonObject): JsonObject? {
        return response["record"]?.let { if (it is JsonObject) it else null }
            ?: response["admin"]?.let { if (it is JsonObject) it else null }
            ?: response["model"]?.let { if (it is JsonObject) it else null }
    }
    
    fun extractToken(response: JsonObject): String {
        return response["token"]?.jsonPrimitive?.content ?: ""
    }
}
