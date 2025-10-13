package love.bside.app.domain.repository

import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.models.UserAnswer

interface QuestionnaireRepository {
    suspend fun getAllQuestionnaires(): Result<List<ProustQuestionnaire>>
    suspend fun getActiveQuestionnaires(): Result<List<ProustQuestionnaire>>
    suspend fun submitAnswer(answer: UserAnswer): Result<Unit>
    suspend fun getUserAnswers(userId: String): Result<List<UserAnswer>>
}
