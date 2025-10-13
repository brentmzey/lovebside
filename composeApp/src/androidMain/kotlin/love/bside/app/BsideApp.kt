package love.bside.app

import android.app.Application
import love.bside.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class BsideApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BsideApp)
            modules(appModule)
        }
    }
}
