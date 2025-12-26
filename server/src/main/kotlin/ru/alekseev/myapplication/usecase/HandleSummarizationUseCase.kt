package ru.alekseev.myapplication.usecase

import ru.alekseev.myapplication.core.common.ChatConstants
import ru.alekseev.myapplication.domain.model.*
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.service.SummarizationService
import java.util.UUID

/**
 * Use case for handling automatic conversation summarization.
 * Checks if summarization is needed and performs it if the threshold is reached.
 *
 * Now works with domain entities instead of database entities.
 */
class HandleSummarizationUseCase(
    private val chatRepository: ChatRepository,
    private val summarizationService: SummarizationService
) {
    /**
     * Checks if summarization is needed and performs it.
     * Summarization triggers when uncompressed messages reach the threshold.
     *
     * @param userId The user identifier (domain value object)
     * @return true if summarization was performed, false otherwise
     */
    suspend operator fun invoke(userId: UserId): Boolean {
        val uncompressedCount = chatRepository.getUncompressedMessagesCount(userId)

        if (uncompressedCount < ChatConstants.SUMMARIZATION_THRESHOLD) {
            return false
        }

        return try {
            val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

            // Create message pairs for summarization (domain entities)
            val messagePairs = uncompressedMessages.map { msg ->
                msg.userMessage to msg.assistantMessage
            }

            // Generate summary
            val summaryText = summarizationService.createSummary(messagePairs)

            // Save summary to database
            val summaries = chatRepository.getAllSummaries(userId)
            chatRepository.saveSummary(
                id = SummaryId(UUID.randomUUID().toString()),
                summaryText = summaryText,
                messagesCount = uncompressedCount.toInt(),
                timestamp = Timestamp(System.currentTimeMillis()),
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
