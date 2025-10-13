package love.bside.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.koin.android.ext.android.get

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = RootComponent(defaultComponentContext(), get())

        setContent {
            App(root)
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}