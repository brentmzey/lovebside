package love.bside.app.presentation.matches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.ui.components.StatefulScreen

@Composable
fun MatchesScreen(component: MatchScreenComponent) {
    val uiState by component.uiState.collectAsState()

    StatefulScreen(uiState) { matches ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(matches) { match ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { component.onMatchClicked(match.id) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Match with: ${match.userTwoId}")
                        Text("Score: ${match.matchScore}")
                    }
                }
            }
        }
    }
}
