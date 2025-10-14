package love.bside.app

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import love.bside.app.di.appModule
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import platform.Foundation.NSUserDefaults

fun initKoin(): KoinApplication {
    val settings: Settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    
    return startKoin {
        modules(appModule(settings))
    }
}
