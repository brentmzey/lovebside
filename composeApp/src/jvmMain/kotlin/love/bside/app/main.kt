package love.bside.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.runBlocking
import love.bside.app.core.appConfig
import love.bside.app.core.logInfo
import love.bside.app.data.api.ApiClient
import love.bside.app.data.api.PocketBaseClient
import love.bside.app.di.appModule
import love.bside.app.routing.RootComponent
import love.bside.app.test.PocketBaseManualTests
import org.koin.core.context.startKoin

fun main(args: Array<String>) {
    val koin = startKoin {
        modules(appModule)
    }.koin

    // Run PocketBase tests if --test flag is provided
    if (args.contains("--test") || args.contains("-t")) {
        logInfo("🧪 Running PocketBase tests...")
        runPocketBaseTests()
        return
    }

    // Run quick smoke test on startup in development
    val config = appConfig()
    if (config.environment == love.bside.app.core.Environment.DEVELOPMENT) {
        logInfo("🔥 Running quick smoke test...")
        runBlocking {
            try {
                val httpClient = ApiClient.create()
                val pocketBaseClient = PocketBaseClient(httpClient)
                val passed = PocketBaseManualTests.quickSmokeTest(pocketBaseClient)
                if (!passed) {
                    logInfo("⚠️ Smoke test failed - PocketBase may have issues. Run with --test for details.")
                }
            } catch (e: Exception) {
                logInfo("⚠️ Could not run smoke test: ${e.message}")
            }
        }
    }

    val lifecycle = LifecycleRegistry()
    val root = RootComponent(DefaultComponentContext(lifecycle), koin.get())

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "B-Side App",
        ) {
            App(root)
        }
    }
}

/**
 * Run comprehensive PocketBase tests
 */
private fun runPocketBaseTests() {
    runBlocking {
        try {
            val httpClient = ApiClient.create()
            val pocketBaseClient = PocketBaseClient(httpClient)
            
            logInfo("\n" + "═".repeat(60))
            logInfo("         B-SIDE POCKETBASE TEST SUITE")
            logInfo("═".repeat(60) + "\n")
            
            // Run full test suite
            PocketBaseManualTests.runAllTests(pocketBaseClient)
            
            logInfo("\n" + "═".repeat(60))
            logInfo("To test with authentication, use:")
            logInfo("  PocketBaseManualTests.testAuthentication(client, email, password)")
            logInfo("═".repeat(60) + "\n")
            
        } catch (e: Exception) {
            logInfo("❌ Test suite failed to initialize: ${e.message}")
            e.printStackTrace()
        }
    }
}