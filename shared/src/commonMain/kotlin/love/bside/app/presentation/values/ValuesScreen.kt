package love.bside.app.presentation.values

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.ui.components.EmptyView
import love.bside.app.ui.components.ErrorView
import love.bside.app.ui.components.LoadingView

@Composable
fun ValuesScreen(component: ValuesScreenComponent) {
    val uiState by component.uiState.collectAsState()

    when (val state = uiState) {
        is ValuesUiState.Idle -> {
            LoadingView(message = "Initializing...")
        }
        is ValuesUiState.Loading -> {
            LoadingView(message = "Loading values...")
        }
        is ValuesUiState.Error -> {
            ErrorView(
                exception = state.exception,
                onRetry = { component.onBack() }
            )
        }
        is ValuesUiState.Success -> {
            if (state.allValues.isEmpty()) {
                EmptyView(
                    message = "No values available",
                    icon = "📋"
                )
            } else {
                ValuesContent(
                    allValues = state.allValues,
                    userValues = state.userValues,
                    onToggleValue = component::toggleUserValue
                )
            }
        }
    }
}

@Composable
private fun ValuesContent(
    allValues: List<love.bside.app.domain.models.KeyValue>,
    userValues: List<love.bside.app.domain.models.UserValue>,
    onToggleValue: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allValues) { keyValue ->
            val isSelected = userValues.any { it.valueId == keyValue.id }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleValue(keyValue.id) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onToggleValue(keyValue.id) }
                    )
                    Column {
                        Text(
                            text = keyValue.valueText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = keyValue.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
