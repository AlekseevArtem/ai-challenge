package ru.alekseev.myapplication.domain.model

/**
 * Type-safe value objects for domain layer.
 * Using inline value classes for zero runtime overhead.
 */

@JvmInline
value class MessageId(val value: String)

@JvmInline
value class UserId(val value: String) {
    companion object {
        val DEFAULT = UserId("default_user")
    }
}

@JvmInline
value class SummaryId(val value: String)

@JvmInline
value class Timestamp(val value: Long)

@JvmInline
value class ResponseTimeMs(val value: Long)
