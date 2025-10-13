package love.bside.app.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import love.bside.app.ui.components.StatefulScreen

@Composable
fun ProfileScreen(component: ProfileScreenComponent) {
    val uiState by component.uiState.collectAsState()

    StatefulScreen(uiState) { profile ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Name: ${profile.firstName} ${profile.lastName}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bio: ${profile.bio}", style = MaterialTheme.typography.bodyLarge)
                }
            }
            Button(onClick = { /* TODO */ }) {
                Text("Go to Questionnaire")
            }
            Button(onClick = { /* TODO */ }) {
                Text("Edit My Values")
            }
            Button(onClick = { /* TODO */ }) {
                Text("My Matches")
            }
            Button(onClick = { component.logout() }) {
                Text("Logout")
            }
        }
    }
}
