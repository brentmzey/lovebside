package love.bside.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun <T> StatefulScreen(
    uiState: UiState<T>,
    content: @Composable (T) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading -> CircularProgressIndicator()
            uiState.error != null -> Text("Error: ${uiState.error}")
            else -> {
                val data = uiState.data
                if (data != null) {
                    content(data)
                }
            }
        }
    }
}

interface UiState<T> {
    val isLoading: Boolean
    val data: T?
    val error: String?
}
