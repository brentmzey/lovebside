package love.bside.app.data

import love.bside.app.core.AppException
import love.bside.app.core.AppLogger

/**
 * Maps PocketBase SDK exceptions to AppException.
 */
fun mapPocketBaseError(action: String, e: Exception): AppException {
    AppLogger.error("ErrorMapping", "PocketBase error during $action", e)
    
    // Check for ClientException by name if we can't import it directly yet,
    // or if it's nested. 
    // The official SDK often throws ClientException which contains statusCode and response.
    
    val message = e.message ?: ""
    
    // Check for 404
    if (message.contains("404")) {
        return AppException.Business.ResourceNotFound(action)
    }
    
    // Check for unique constraint violation (400 Bad Request usually)
    if (message.contains("400") && (message.contains("UNIQUE constraint failed") || message.contains("already exists") || message.contains("must be unique"))) {
        return AppException.Business.DuplicateResource(action)
    }
    
    // Default fallback
    return AppException.Unknown("Failed to $action: ${e.message}", e)
}
