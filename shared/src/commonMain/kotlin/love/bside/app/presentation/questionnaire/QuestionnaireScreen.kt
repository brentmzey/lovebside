package love.bside.app.presentation.questionnaire

import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.foundation.layout.padding

import androidx.compose.foundation.lazy.LazyColumn

import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button

import androidx.compose.material3.Card

import androidx.compose.material3.Text

import androidx.compose.material3.TextField

import androidx.compose.runtime.*

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp

import love.bside.app.ui.components.StatefulScreen



@Composable
fun QuestionnaireScreen(component: QuestionnaireScreenComponent) {
    val uiState by component.uiState.collectAsState()

    StatefulScreen(uiState) { questions ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(questions) { question ->
                var answer by remember { mutableStateOf("") }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(question.questionText)
                        TextField(
                            value = answer,
                            onValueChange = { answer = it },
                            label = { Text("Your answer") }
                        )
                        Button(onClick = { component.submitAnswer(question.id, answer) }) {
                            Text("Submit")
                        }
                    }
                }

            }

        }

    }

}
