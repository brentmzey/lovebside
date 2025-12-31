package love.bside.app.di


import love.bside.app.presentation.ChatViewModel
import love.bside.app.presentation.MessagingViewModel
import love.bside.app.ui.screens.proust.QuestionnaireViewModel
import love.bside.app.ui.screens.home.DashboardViewModel
import org.koin.dsl.bind
import org.koin.dsl.module

val uiModule = module {


    // 3. ViewModels
    // Parameter injection: calling org.koin.compose.koinViewModel<MessagingViewModel> { parametersOf(userId) }
    // Using factory instead of viewModel DSL for KMP compatibility without Android
    factory { params -> MessagingViewModel(get(), params.get()) }
    factory { params -> ChatViewModel(get(), params.get()) }
    factory { QuestionnaireViewModel(get()) }
    factory { DashboardViewModel(get()) }
    factory { love.bside.app.ui.screens.auth.AuthViewModel(get(), get(), get()) }
}
