package love.bside.server.services

import love.bside.app.core.Result
import love.bside.server.models.api.PromptDTO
import love.bside.server.models.api.PromptAnswerDTO
import love.bside.server.models.api.SubmitAnswerRequest
import love.bside.server.plugins.ValidationException
import love.bside.server.repositories.PromptRepository
import love.bside.server.utils.toDTO

/**
 * Service for prompt operations
 */
class PromptService(
    private val promptRepository: PromptRepository
) {
    
    /**
     * Get all available prompts
     */
    suspend fun getAllPrompts(): List<PromptDTO> {
        return when (val result = promptRepository.getAllPrompts()) {
            is Result.Success -> result.data.map { it.toDTO() }
            is Result.Error -> throw Exception("Failed to get prompts: ${result.exception.message}")
            is Result.Loading -> throw Exception("Prompts lookup is still loading")
        }
    }
    
    /**
     * Get user's answers
     */
    suspend fun getUserAnswers(userId: String): List<PromptAnswerDTO> {
        val answers = when (val result = promptRepository.getUserAnswers(userId)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to get answers: ${result.exception.message}")
            is Result.Loading -> throw Exception("Answers lookup is still loading")
        }
        
        return answers.mapNotNull { answer ->
            // Get prompt details
            when (val promptResult = promptRepository.getPromptById(answer.promptId)) {
                is Result.Success -> answer.toDTO(promptResult.data)
                is Result.Error -> null
                is Result.Loading -> null
            }
        }
    }
    
    /**
     * Submit answer to a prompt
     */
    suspend fun submitAnswer(userId: String, request: SubmitAnswerRequest): PromptAnswerDTO {
        // Validate
        if (request.answer.isBlank()) {
            throw ValidationException("Answer cannot be empty")
        }
        if (request.answer.length > 500) {
            throw ValidationException("Answer must be less than 500 characters")
        }
        
        // Save answer
        val answer = when (val result = promptRepository.saveUserAnswer(userId, request.promptId, request.answer)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Failed to save answer: ${result.exception.message}")
            is Result.Loading -> throw Exception("Answer save is still loading")
        }
        
        // Get prompt details
        val prompt = when (val result = promptRepository.getPromptById(answer.promptId)) {
            is Result.Success -> result.data
            is Result.Error -> throw Exception("Prompt not found")
            is Result.Loading -> throw Exception("Prompt lookup is still loading")
        }
        
        return answer.toDTO(prompt)
    }
}
