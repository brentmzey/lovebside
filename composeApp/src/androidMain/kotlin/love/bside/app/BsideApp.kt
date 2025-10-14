package love.bside.app

import android.app.Application
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import love.bside.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BsideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        val settings: Settings = SharedPreferencesSettings(
            getSharedPreferences("bside_prefs", MODE_PRIVATE)
        )
        
        startKoin {
            androidContext(this@BsideApp)
            modules(appModule(settings))
        }
    }
}
