package love.bside.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import love.bside.app.data.models.toDomain
import love.bside.app.domain.models.ProustQuestionnaire
import love.bside.app.domain.models.UserAnswer
import love.bside.app.domain.repository.QuestionnaireRepository

@Serializable
data class UserAnswerCreateRequest(
    val userId: String,
    val questionId: String,
    val answerText: String
)

class PocketBaseQuestionnaireRepository(private val client: HttpClient) : QuestionnaireRepository {
    override suspend fun getAllQuestionnaires(): Result<List<ProustQuestionnaire>> {
        return try {
            val dtos = client.get("collections/s_proust_questionnaires/records").body<List<love.bside.app.data.models.ProustQuestionnaire>>()
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveQuestionnaires(): Result<List<ProustQuestionnaire>> {
        return try {
            val dtos = client.get("collections/s_proust_questionnaires/records?filter=(isActive=true)").body<List<love.bside.app.data.models.ProustQuestionnaire>>()
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun submitAnswer(answer: UserAnswer): Result<Unit> {
        return try {
            val request = UserAnswerCreateRequest(
                userId = answer.userId,
                questionId = answer.questionId,
                answerText = answer.answerText
            )
            client.post("collections/s_user_answers/records") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserAnswers(userId: String): Result<List<UserAnswer>> {
        return try {
            val dtos = client.get("collections/s_user_answers/records?filter=(userId='$userId')").body<List<love.bside.app.data.models.UserAnswer>>()
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
