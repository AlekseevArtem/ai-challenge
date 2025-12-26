package ru.alekseev.myapplication.domain.model

/**
 * Domain model for a chat message.
 * Represents a single user-assistant message pair.
 */
data class Message(
    val id: MessageId,
    val userMessage: String,
    val assistantMessage: String,
    val claudeResponseJson: String,
    val timestamp: Timestamp,
    val responseTimeMs: ResponseTimeMs,
    val isCompressed: Boolean,
    val userId: UserId
)
