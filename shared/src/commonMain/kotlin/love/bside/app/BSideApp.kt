package love.bside.app

import love.bside.app.di.appModule
import love.bside.app.di.professionalArchitectureModules
import love.bside.app.orchestration.AppOrchestrator
import org.koin.core.context.startKoin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import com.russhwolf.settings.Settings

/**
 * Application initializer with full orchestration setup.
 * Call this from your platform-specific entry point.
 */
class BSideApp : KoinComponent {
    
    private val orchestrator: AppOrchestrator by inject()
    
    suspend fun initialize(settings: Settings) {
        // Start Koin DI
        startKoin {
            modules(
                appModule(settings),
                *professionalArchitectureModules.toTypedArray()
            )
        }
        
        // Initialize orchestration layer
        orchestrator.initialize()
    }
    
    suspend fun shutdown() {
        orchestrator.shutdown()
    }
}

/**
 * Simple initialization function for platforms
 */
suspend fun initializeBSideApp(settings: Settings) {
    BSideApp().initialize(settings)
}
