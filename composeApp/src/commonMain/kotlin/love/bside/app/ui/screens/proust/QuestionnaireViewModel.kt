package love.bside.app.ui.screens.proust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import love.bside.app.core.Result
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.repository.MessagingRepository

data class QuestionnaireUiState(
    val isLoading: Boolean = false,
    val questions: List<ProustQuestionnaire> = emptyList(),
    val answers: Map<String, String> = emptyMap(), // QuestionId -> AnswerText
    val error: String? = null
)

class QuestionnaireViewModel(
    private val repository: MessagingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(QuestionnaireUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            // Parallel fetch could be better but sequential is safer for now
            val qResult = repository.getQuestionnaire()
            val aResult = repository.getUserAnswers()
            
            val questions = when(qResult) {
                is Result.Success -> qResult.data
                is Result.Error -> emptyList()
                else -> emptyList()
            }
            
            val answers = when(aResult) {
                is Result.Success -> aResult.data.associate { it.questionId to it.answerText }
                is Result.Error -> emptyMap()
                else -> emptyMap()
            }
            
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    questions = questions,
                    answers = answers,
                    error = if (qResult is Result.Error) qResult.exception.message else null
                )
            }
        }
    }
    
    fun onAnswerChanged(questionId: String, text: String) {
        _uiState.update { 
            val newAnswers = it.answers.toMutableMap()
            newAnswers[questionId] = text
            it.copy(answers = newAnswers)
        }
    }

    fun saveAnswer(questionId: String, text: String) {
        viewModelScope.launch {
             // Optimistic update already happened in onAnswerChanged
             val result = repository.submitQuestionnaireResponse(questionId, text)
             if (result is Result.Error) {
                 // Revert or show error? For now, just show error toast idea
                 _uiState.update { it.copy(error = "Failed to save answer") }
             }
        }
    }
}
