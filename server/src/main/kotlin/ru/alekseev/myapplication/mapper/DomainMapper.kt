package ru.alekseev.myapplication.mapper

import ru.alekseev.myapplication.domain.model.*
import ru.alekseev.myapplication.db.Message as DbMessage
import ru.alekseev.myapplication.db.Summary as DbSummary

/**
 * Mappers for converting between database entities and domain models.
 * Database entities use snake_case and primitives.
 * Domain models use camelCase and value objects.
 */

/**
 * Convert database Message entity to domain Message model.
 */
fun DbMessage.toDomain(): Message {
    return Message(
        id = MessageId(this.id),
        userMessage = this.user_message,
        assistantMessage = this.assistant_message,
        claudeResponseJson = this.claude_response_json,
        timestamp = Timestamp(this.timestamp),
        responseTimeMs = ResponseTimeMs(this.response_time_ms),
        isCompressed = this.is_compressed != 0L,
        userId = UserId(this.user_id)
    )
}

/**
 * Convert list of database Messages to domain Messages.
 */
@JvmName("messagesToDomain")
fun List<DbMessage>.toDomain(): List<Message> {
    return this.map { it.toDomain() }
}

/**
 * Convert database Summary entity to domain Summary model.
 */
fun DbSummary.toDomain(): Summary {
    return Summary(
        id = SummaryId(this.id),
        summaryText = this.summary_text,
        messagesCount = this.messages_count.toInt(),
        timestamp = Timestamp(this.timestamp),
        position = this.position.toInt(),
        userId = UserId(this.user_id)
    )
}

/**
 * Convert list of database Summaries to domain Summaries.
 */
@JvmName("summariesToDomain")
fun List<DbSummary>.toDomain(): List<Summary> {
    return this.map { it.toDomain() }
}
