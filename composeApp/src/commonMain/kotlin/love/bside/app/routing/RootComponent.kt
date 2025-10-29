package love.bside.app.routing

import com.arkivanov.decompose.ComponentContext
import org.koin.core.Koin

class RootComponent(
    componentContext: ComponentContext,
    private val koin: Koin
) : ComponentContext by componentContext {
    // This is a placeholder to resolve build errors.
    // The actual navigation logic will be implemented here.
}
