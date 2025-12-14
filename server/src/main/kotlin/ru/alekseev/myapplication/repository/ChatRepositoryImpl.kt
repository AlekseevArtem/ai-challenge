package ru.alekseev.myapplication.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.db.Message
import ru.alekseev.myapplication.db.Summary

class ChatRepositoryImpl(
    private val database: ChatDatabase,
) : ChatRepository {

    private val queries = database.chatDatabaseQueries

    override suspend fun saveMessage(
        id: String,
        userMessage: String,
        assistantMessage: String,
        claudeResponseJson: String,
        timestamp: Long,
        responseTimeMs: Long,
        userId: String,
    ) = withContext(Dispatchers.IO) {
        queries.insertMessage(
            id = id,
            user_message = userMessage,
            assistant_message = assistantMessage,
            claude_response_json = claudeResponseJson,
            timestamp = timestamp,
            response_time_ms = responseTimeMs,
            is_compressed = 0,
            user_id = userId
        )
    }

    override suspend fun getAllMessages(userId: String): List<Message> =
        withContext(Dispatchers.IO) {
            queries.getAllMessages(userId).executeAsList()
        }

    override suspend fun getUncompressedMessages(userId: String): List<Message> =
        withContext(Dispatchers.IO) {
            queries.getUncompressedMessages(userId).executeAsList()
        }

    override suspend fun getUncompressedMessagesCount(userId: String): Long =
        withContext(Dispatchers.IO) {
            queries.getUncompressedMessagesCount(userId).executeAsOne()
        }

    override suspend fun markMessagesAsCompressed(messageIds: List<String>) =
        withContext(Dispatchers.IO) {
            // SQLDelight doesn't support IN clause with parameters directly, so we need to update each message
            messageIds.forEach { id ->
                queries.transaction {
                    queries.markMessagesAsCompressed(listOf(id))
                }
            }
        }

    override suspend fun saveSummary(
        id: String,
        summaryText: String,
        messagesCount: Int,
        timestamp: Long,
        position: Int,
        userId: String,
    ) = withContext(Dispatchers.IO) {
        queries.insertSummary(
            id = id,
            summary_text = summaryText,
            messages_count = messagesCount.toLong(),
            timestamp = timestamp,
            position = position.toLong(),
            user_id = userId
        )
    }

    override suspend fun getAllSummaries(userId: String): List<Summary> =
        withContext(Dispatchers.IO) {
            queries.getAllSummaries(userId).executeAsList()
        }

    override suspend fun getMessageById(id: String): Message? = withContext(Dispatchers.IO) {
        queries.getMessageById(id).executeAsOneOrNull()
    }
}
