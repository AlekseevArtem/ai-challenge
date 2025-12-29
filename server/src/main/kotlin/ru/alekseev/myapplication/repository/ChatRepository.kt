package ru.alekseev.myapplication.repository

import ru.alekseev.myapplication.domain.model.*

/**
 * Repository for chat messages and summaries.
 * Returns domain entities, not database entities or DTOs.
 */
interface ChatRepository {
    /**
     * Save a new message to the database.
     */
    suspend fun saveMessage(
        id: MessageId,
        userMessage: String,
        assistantMessage: String,
        claudeResponseJson: String,
        timestamp: Timestamp,
        responseTimeMs: ResponseTimeMs,
        userId: UserId = UserId.DEFAULT
    )

    /**
     * Get all messages for a user (domain entities).
     */
    suspend fun getAllMessages(userId: UserId = UserId.DEFAULT): List<Message>

    /**
     * Get uncompressed messages for a user (domain entities).
     */
    suspend fun getUncompressedMessages(userId: UserId = UserId.DEFAULT): List<Message>

    /**
     * Get count of uncompressed messages.
     */
    suspend fun getUncompressedMessagesCount(userId: UserId = UserId.DEFAULT): Long

    /**
     * Mark messages as compressed.
     */
    suspend fun markMessagesAsCompressed(messageIds: List<MessageId>)

    /**
     * Save a summary to the database.
     */
    suspend fun saveSummary(
        id: SummaryId,
        summaryText: String,
        messagesCount: Int,
        timestamp: Timestamp,
        position: Int,
        userId: UserId = UserId.DEFAULT
    )

    /**
     * Get all summaries for a user (domain entities).
     */
    suspend fun getAllSummaries(userId: UserId = UserId.DEFAULT): List<Summary>

    /**
     * Delete all summaries for a user.
     * Used when replacing old summaries with a new cumulative one.
     */
    suspend fun deleteAllSummaries(userId: UserId = UserId.DEFAULT)

    /**
     * Get a message by ID (domain entity).
     */
    suspend fun getMessageById(id: MessageId): Message?
}
