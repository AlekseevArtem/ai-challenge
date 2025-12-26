package ru.alekseev.myapplication.domain.model

/**
 * Domain model for a conversation summary.
 * Summaries are created when conversations get too long.
 */
data class Summary(
    val id: SummaryId,
    val summaryText: String,
    val messagesCount: Int,
    val timestamp: Timestamp,
    val position: Int,
    val userId: UserId
)
