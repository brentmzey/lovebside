package io.pocketbase.services

import io.pocketbase.PocketBase

/**
 * Base service class that all other services extend.
 */
abstract class BaseService(
    protected val client: PocketBase
)
