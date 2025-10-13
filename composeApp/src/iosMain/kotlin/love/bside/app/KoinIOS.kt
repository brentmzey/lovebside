package love.bside.app

import love.bside.app.di.appModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

fun initKoin(): KoinApplication {
    return startKoin {
        modules(appModule)
    }
}
