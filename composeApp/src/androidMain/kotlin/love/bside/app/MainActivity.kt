package love.bside.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.arkivanov.decompose.defaultComponentContext
import love.bside.app.routing.RootComponent
import org.koin.android.ext.android.getKoin

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootComponent = RootComponent(
            componentContext = defaultComponentContext()
        )
        val appDependencies = buildAppDependencies(getKoin())

        setContent {
            App(rootComponent, appDependencies)
        }
    }
}