package love.bside.app.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.Serializable
import love.bside.app.data.models.toDomain
import love.bside.app.domain.models.Match
import love.bside.app.domain.models.Prompt
import love.bside.app.domain.repository.MatchRepository

@Serializable
data class MatchStatusUpdateRequest(val matchStatus: String)

class PocketBaseMatchRepository(private val client: HttpClient) : MatchRepository {
    override suspend fun getMatches(userId: String): Result<List<Match>> {
        return try {
            val dtos = client.get("collections/t_matches/records?filter=(userOneId='$userId' || userTwoId='$userId')").body<List<love.bside.app.data.models.Match>>()
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateMatchStatus(matchId: String, status: String): Result<Unit> {
        return try {
            val request = MatchStatusUpdateRequest(status)
            client.patch("collections/t_matches/records/$matchId") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPromptsForMatch(matchId: String): Result<List<Prompt>> {
        return try {
            val dtos = client.get("collections/t_prompts/records?filter=(matchId='$matchId')").body<List<love.bside.app.data.models.Prompt>>()
            Result.success(dtos.map { it.toDomain() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
