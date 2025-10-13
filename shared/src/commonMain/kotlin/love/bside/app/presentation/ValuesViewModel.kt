package love.bside.app.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import love.bside.app.domain.models.KeyValue
import love.bside.app.domain.models.UserValue
import love.bside.app.domain.repository.ValuesRepository

data class ValuesUiState(
    val isLoading: Boolean = false,
    val allKeyValues: List<KeyValue> = emptyList(),
    val userValues: List<UserValue> = emptyList(),
    val error: String? = null
)

class ValuesViewModel(private val valuesRepository: ValuesRepository) {

    private val viewModelScope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(ValuesUiState())
    val uiState: StateFlow<ValuesUiState> = _uiState.asStateFlow()

    fun fetchValues(userId: String) {
        _uiState.value = ValuesUiState(isLoading = true)
        viewModelScope.launch {
            val allValuesResult = valuesRepository.getAllKeyValues()
            val userValuesResult = valuesRepository.getUserValues(userId)

            if (allValuesResult.isSuccess && userValuesResult.isSuccess) {
                _uiState.value = ValuesUiState(
                    allKeyValues = allValuesResult.getOrThrow(),
                    userValues = userValuesResult.getOrThrow()
                )
            } else {
                _uiState.value = ValuesUiState(error = "Failed to load values")
            }
        }
    }

    fun toggleUserValue(userId: String, valueId: String) {
        viewModelScope.launch {
            val userValue = UserValue(
                id = "", // Not needed for add/remove
                created = kotlinx.datetime.Clock.System.now(),
                updated = kotlinx.datetime.Clock.System.now(),
                userId = userId,
                valueId = valueId
            )
            val isSelected = _uiState.value.userValues.any { it.valueId == valueId }

            val result = if (isSelected) {
                valuesRepository.removeUserValue(userValue)
            } else {
                valuesRepository.addUserValue(userValue)
            }

            if (result.isSuccess) {
                fetchValues(userId) // Refresh the values
            }
        }
    }
}
