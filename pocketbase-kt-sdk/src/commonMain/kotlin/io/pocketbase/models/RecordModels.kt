package io.pocketbase.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Base record model that all PocketBase records extend.
 */
@Serializable
open class RecordModel(
    val id: String = "",
    val collectionId: String = "",
    val collectionName: String = "",
    val created: String = "",
    val updated: String = "",
    val expand: JsonObject? = null
)

/**
 * Authentication response model.
 */
@Serializable
data class AuthResponse(
    val token: String,
    val record: JsonObject,
    val meta: JsonObject? = null
)

/**
 * Options for queries (filter, sort, expand, etc).
 */
data class QueryOptions(
    val filter: String? = null,
    val sort: String? = null,
    val expand: String? = null,
    val fields: String? = null,
    val page: Int? = null,
    val perPage: Int? = null,
    val skipTotal: Boolean? = null,
    val headers: Map<String, String>? = null
)

/**
 * List result with pagination info.
 */
@Serializable
data class ListResult<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)
