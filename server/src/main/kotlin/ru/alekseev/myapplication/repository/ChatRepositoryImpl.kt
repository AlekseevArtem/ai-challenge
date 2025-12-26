package ru.alekseev.myapplication.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.domain.model.*
import ru.alekseev.myapplication.mapper.toDomain

/**
 * Repository implementation that converts database entities to domain models.
 */
class ChatRepositoryImpl(
    database: ChatDatabase,
) : ChatRepository {

    private val queries = database.chatDatabaseQueries

    override suspend fun saveMessage(
        id: MessageId,
        userMessage: String,
        assistantMessage: String,
        claudeResponseJson: String,
        timestamp: Timestamp,
        responseTimeMs: ResponseTimeMs,
        userId: UserId,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertMessage(
                id = id.value,
                user_message = userMessage,
                assistant_message = assistantMessage,
                claude_response_json = claudeResponseJson,
                timestamp = timestamp.value,
                response_time_ms = responseTimeMs.value,
                is_compressed = 0,
                user_id = userId.value
            )
        }
    }

    override suspend fun getAllMessages(userId: UserId): List<Message> =
        withContext(Dispatchers.IO) {
            queries.getAllMessages(userId.value).executeAsList().toDomain()
        }

    override suspend fun getUncompressedMessages(userId: UserId): List<Message> =
        withContext(Dispatchers.IO) {
            queries.getUncompressedMessages(userId.value).executeAsList().toDomain()
        }

    override suspend fun getUncompressedMessagesCount(userId: UserId): Long =
        withContext(Dispatchers.IO) {
            queries.getUncompressedMessagesCount(userId.value).executeAsOne()
        }

    override suspend fun markMessagesAsCompressed(messageIds: List<MessageId>) =
        withContext(Dispatchers.IO) {
            // SQLDelight doesn't support IN clause with parameters directly, so we need to update each message
            messageIds.forEach { id ->
                queries.transaction {
                    queries.markMessagesAsCompressed(listOf(id.value))
                }
            }
        }

    override suspend fun saveSummary(
        id: SummaryId,
        summaryText: String,
        messagesCount: Int,
        timestamp: Timestamp,
        position: Int,
        userId: UserId,
    ) {
        withContext(Dispatchers.IO) {
            queries.insertSummary(
                id = id.value,
                summary_text = summaryText,
                messages_count = messagesCount.toLong(),
                timestamp = timestamp.value,
                position = position.toLong(),
                user_id = userId.value
            )
        }
    }

    override suspend fun getAllSummaries(userId: UserId): List<Summary> =
        withContext(Dispatchers.IO) {
            queries.getAllSummaries(userId.value).executeAsList().toDomain()
        }

    override suspend fun getMessageById(id: MessageId): Message? = withContext(Dispatchers.IO) {
        queries.getMessageById(id.value).executeAsOneOrNull()?.toDomain()
    }
}
