package ru.alekseev.myapplication.repository

import ru.alekseev.myapplication.db.Message
import ru.alekseev.myapplication.db.Summary

interface ChatRepository {
    suspend fun saveMessage(
        id: String,
        userMessage: String,
        assistantMessage: String,
        claudeResponseJson: String,
        timestamp: Long,
        responseTimeMs: Long,
        userId: String = "default_user"
    )

    suspend fun getAllMessages(userId: String = "default_user"): List<Message>

    suspend fun getUncompressedMessages(userId: String = "default_user"): List<Message>

    suspend fun getUncompressedMessagesCount(userId: String = "default_user"): Long

    suspend fun markMessagesAsCompressed(messageIds: List<String>)

    suspend fun saveSummary(
        id: String,
        summaryText: String,
        messagesCount: Int,
        timestamp: Long,
        position: Int,
        userId: String = "default_user"
    )

    suspend fun getAllSummaries(userId: String = "default_user"): List<Summary>

    suspend fun getMessageById(id: String): Message?
}
