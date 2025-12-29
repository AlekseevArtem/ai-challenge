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
     * This implementation creates a SINGLE cumulative summary that replaces all previous summaries.
     * Each new summary integrates the previous summary with new messages.
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

            // Get the latest (and only) summary if it exists
            val summaries = chatRepository.getAllSummaries(userId)
            val previousSummary = summaries.lastOrNull()?.summaryText

            // Generate NEW cumulative summary that integrates previous summary + new messages
            val summaryText = summarizationService.createSummary(messagePairs, previousSummary)

            // Delete ALL old summaries (we only keep one cumulative summary)
            chatRepository.deleteAllSummaries(userId)

            // Save the new cumulative summary
            chatRepository.saveSummary(
                id = SummaryId(UUID.randomUUID().toString()),
                summaryText = summaryText,
                messagesCount = uncompressedCount.toInt(),
                timestamp = Timestamp(System.currentTimeMillis()),
                position = 0, // Always position 0 since we only have one summary
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
