package love.bside.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import love.bside.app.routing.RootComponent
import org.koin.android.ext.android.inject
import org.koin.core.Koin

class MainActivity : ComponentActivity() {
    private val koin: Koin by inject()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootComponent = RootComponent(
            componentContext = defaultComponentContext(),
            koin = koin
        )

        setContent {
            App(rootComponent)
        }
    }
}