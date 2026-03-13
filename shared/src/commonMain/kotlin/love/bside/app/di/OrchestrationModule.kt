package love.bside.app.di

import love.bside.app.orchestration.*
import love.bside.app.orchestration.events.EventBus
import love.bside.app.orchestration.health.HealthMonitor
import love.bside.app.orchestration.jobs.JobScheduler
import love.bside.app.orchestration.jobs.JobSchedulerFactory
import love.bside.app.orchestration.jobs.EventToJobMapper
import love.bside.app.orchestration.lifecycle.AppLifecycle
import love.bside.app.orchestration.sync.SyncOrchestrator
import love.bside.app.domain.services.*
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Orchestration layer dependency injection module.
 * Provides event bus, job scheduler, sync orchestrator, and health monitoring.
 */
val orchestrationModule: Module = module {
    
    // Event Bus - Central event distribution
    single { EventBus() }
    
    // Job Scheduler - Platform-specific background job scheduling
    single<JobScheduler> { JobSchedulerFactory.create() }
    
    // Sync Orchestrator - Handles offline-first synchronization
    single { SyncOrchestrator(eventBus = get()) }
    
    // Health Monitor - System health monitoring
    single { HealthMonitor() }
    
    // App Lifecycle - Tracks app foreground/background state
    single { AppLifecycle() }
    
    // Event to Job Mapper - Maps domain events to background jobs
    single { EventToJobMapper(scheduler = get()) }
    
    // App Orchestrator - Central coordinator for all systems
    single {
        AppOrchestrator(
            eventBus = get(),
            jobScheduler = get(),
            lifecycle = get()
        )
    }
}

/**
 * Domain layer dependency injection module.
 * Provides domain services and business logic.
 */
val domainServicesModule: Module = module {
    
    // Matching Service - Calculate compatibility and create matches
    single { MatchingService(eventBus = get()) }
    
    // Conversation Service - Conversation business rules
    single { ConversationService() }
    
    // Notification Service - Notification logic
    single { NotificationService(eventBus = get()) }
}

/**
 * Combined module for easy inclusion
 */
val professionalArchitectureModules = listOf(
    orchestrationModule,
    domainServicesModule
)
