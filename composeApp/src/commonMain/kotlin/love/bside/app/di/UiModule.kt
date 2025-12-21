package love.bside.app.di

import io.pocketbase.PocketBase
import love.bside.app.domain.repository.MessagingRepository
import love.bside.app.data.repository.PocketBaseMessagingRepository
import love.bside.app.presentation.ChatViewModel
import love.bside.app.presentation.MessagingViewModel
import love.bside.app.ui.screens.proust.QuestionnaireViewModel
import love.bside.app.ui.screens.home.DashboardViewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val uiModule = module {
    // 1. PocketBase Instance
    // Using localhost for dev. Ideally this comes from config.
    single { 
        PocketBase("http://127.0.0.1:8090").apply {
             // Sync AuthStore with TokenStorage if needed?
             // For now, raw instance.
        } 
    }

    // 2. Messaging Repository
    single { PocketBaseMessagingRepository(get()) } bind MessagingRepository::class

    // 3. ViewModels
    // Parameter injection: calling org.koin.compose.koinViewModel<MessagingViewModel> { parametersOf(userId) }
    // Using factory instead of viewModel DSL for KMP compatibility without Android
    factory { params -> MessagingViewModel(get(), params.get()) }
    factory { params -> ChatViewModel(get(), params.get()) }
    factory { QuestionnaireViewModel(get()) }
    factory { DashboardViewModel(get()) }
}
