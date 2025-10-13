package love.bside.app.di

/**
 * DI Module interface for platform-specific dependency injection
 */
interface DIModule {
    fun <T : Any> get(clazz: kotlin.reflect.KClass<T>): T
    fun <T : Any> inject(clazz: kotlin.reflect.KClass<T>): Lazy<T>
}

/**
 * Platform-specific DI initialization
 */
expect fun initializeDI()

/**
 * Get DI instance (platform-specific)
 */
expect fun getDI(): DIModule
