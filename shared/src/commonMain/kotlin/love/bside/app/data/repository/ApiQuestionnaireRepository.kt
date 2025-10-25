package love.bside.app.data.repository

import love.bside.app.core.Result
import love.bside.app.data.api.InternalApiClient
import love.bside.app.data.mappers.toDomain
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.models.UserAnswer
import love.bside.app.domain.repository.QuestionnaireRepository
import kotlinx.datetime.Clock

/**
 * API-based implementation of QuestionnaireRepository
 * This repository communicates with our internal API instead of PocketBase directly
 */
class ApiQuestionnaireRepository(
    private val apiClient: InternalApiClient
) : QuestionnaireRepository {
    
    override suspend fun getAllQuestionnaires(): Result<List<ProustQuestionnaire>> {
        // Convert prompts to questionnaires
        return apiClient.getAllPrompts().map { promptDTOs ->
            promptDTOs.mapIndexed { index, promptDTO ->
                val now = Clock.System.now()
                ProustQuestionnaire(
                    id = promptDTO.id,
                    created = now,
                    updated = now,
                    questionText = promptDTO.text,
                    questionOrder = promptDTO.displayOrder,
                    isActive = true
                )
            }
        }
    }
    
    override suspend fun getActiveQuestionnaires(): Result<List<ProustQuestionnaire>> {
        // All prompts from API are considered active
        return getAllQuestionnaires()
    }
    
    override suspend fun submitAnswer(answer: UserAnswer): Result<Unit> {
        return apiClient.submitAnswer(
            promptId = answer.questionId,
            answer = answer.answerText
        ).map { Unit }
    }
    
    override suspend fun getUserAnswers(userId: String): Result<List<UserAnswer>> {
        return apiClient.getUserAnswers().map { answerDTOs ->
            answerDTOs.map { it.toDomain(userId) }
        }
    }
}
