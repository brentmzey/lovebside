package io.pocketbase.functional

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.pocketbase.models.ClientResponseException
import io.pocketbase.models.RecordModel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.Json

// Helper to wrap suspend calls in Either
suspend fun <T> catchRequest(block: suspend () -> T): Either<ClientResponseException, T> {
    return try {
        block().right()
    } catch (e: ClientResponseException) {
        e.left()
    } catch (e: Exception) {
        // Wrap unexpected exceptions in ClientResponseException or handle differently
        ClientResponseException(
            url = "", 
            statusCode = 0, 
            response = io.pocketbase.models.ErrorResponse(message = e.message ?: "Unknown error"), 
            originalError = e
        ).left()
    }
}

// Extension to map generic RecordModel to specific Domain Models via Json re-encoding (simplest approach for now)
inline fun <reified T : RecordModel> RecordModel.toDomain(json: Json): Either<Exception, T> {
   return try {
       // This assumes RecordModel can be serialized back to Json and then deserialized to T
       // Or more efficiently, if we have the original JsonObject, use that.
       // Since RecordModel doesn't hold the original JsonObject by default unless we modify it, 
       // let's assume we are working with the response JsonObject directly in the service layer mostly.
       // BUT, for this extension, let's assume T is a @Serializable class that *extends* RecordModel 
       // and we want to "upcast" or "map" from a raw response.
       
       // Actually, a better pattern with PocketBase KMP is often to fetch directly as T.
       // So this might be more of a utility for mapping 'expand' fields or generic 'items'.
       Exception("Not implemented: Need original JsonObject source").left()
   } catch (e: Exception) {
       e.left()
   }
}

// Better approach: Extension on Json which is available in the SDK
inline fun <reified T> Json.tryDecode(element: JsonElement): Either<Exception, T> {
    return try {
        this.decodeFromJsonElement<T>(element).right()
    } catch (e: Exception) {
        e.left()
    }
}

// Extension to get typed list from RecordService
suspend inline fun <reified T : RecordModel> io.pocketbase.services.RecordService.getListTyped(
    options: io.pocketbase.models.QueryOptions? = null
): Either<ClientResponseException, io.pocketbase.models.ListResult<T>> {
    return catchRequest {
        val jsonResult = this.getList(options)
        // We need to re-decode the items. This is a bit inefficient but works without changing core RecordService for now.
        // A better way would be if RecordService was generic or allowed passing a KSerializer.
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true } // or access client.json if possible (it's internal)
        
        io.pocketbase.models.ListResult(
            page = jsonResult.page,
            perPage = jsonResult.perPage,
            totalItems = jsonResult.totalItems,
            totalPages = jsonResult.totalPages,
            items = jsonResult.items.map { json.decodeFromJsonElement<T>(it) }
        )
    }
}

suspend inline fun <reified T : RecordModel> io.pocketbase.services.RecordService.getOneTyped(
    id: String,
    options: io.pocketbase.models.QueryOptions? = null
): Either<ClientResponseException, T> {
    return catchRequest {
        val jsonObject = this.getOne(id, options)
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
        json.decodeFromJsonElement<T>(jsonObject)
    }
}

