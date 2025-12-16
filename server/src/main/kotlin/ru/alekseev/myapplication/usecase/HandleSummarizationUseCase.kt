package ru.alekseev.myapplication.usecase

import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.service.SummarizationService
import java.util.UUID

/**
 * Use case for handling automatic conversation summarization.
 * Checks if summarization is needed and performs it if the threshold is reached.
 */
class HandleSummarizationUseCase(
    private val chatRepository: ChatRepository,
    private val summarizationService: SummarizationService
) {
    /**
     * Checks if summarization is needed and performs it.
     * Summarization triggers when uncompressed messages reach the threshold.
     *
     * @param userId The user identifier
     * @return true if summarization was performed, false otherwise
     */
    suspend operator fun invoke(userId: String): Boolean {
        val uncompressedCount = chatRepository.getUncompressedMessagesCount(userId)

        if (uncompressedCount < ChatConstants.SUMMARIZATION_THRESHOLD) {
            return false
        }

        return try {
            val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

            // Create message pairs for summarization
            val messagePairs = uncompressedMessages.map { msg ->
                msg.user_message to msg.assistant_message
            }

            // Generate summary
            val summaryText = summarizationService.createSummary(messagePairs)

            // Save summary to database
            val summaries = chatRepository.getAllSummaries(userId)
            chatRepository.saveSummary(
                id = UUID.randomUUID().toString(),
                summaryText = summaryText,
                messagesCount = uncompressedCount.toInt(),
                timestamp = System.currentTimeMillis(),
                position = summaries.size,
                userId = userId
            )

            // Mark messages as compressed
            chatRepository.markMessagesAsCompressed(
                uncompressedMessages.map { it.id }
            )

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
