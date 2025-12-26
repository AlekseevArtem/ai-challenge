package ru.alekseev.myapplication.domain.model

import ru.alekseev.myapplication.domain.exception.ValidationException

/**
 * Type-safe value objects for domain layer.
 * Using inline value classes for zero runtime overhead with factory methods for validation.
 */

@JvmInline
value class MessageId(val value: String) {
    companion object {
        /**
         * Create MessageId with validation.
         * @throws ValidationException if value is blank
         */
        fun create(value: String): MessageId {
            if (value.isBlank()) {
                throw ValidationException("MessageId", value, "ID cannot be blank")
            }
            return MessageId(value)
        }
    }
}

@JvmInline
value class UserId(val value: String) {
    companion object {
        val DEFAULT = UserId("default_user")

        /**
         * Create UserId with validation.
         * @throws ValidationException if value is blank
         */
        fun create(value: String): UserId {
            if (value.isBlank()) {
                throw ValidationException("UserId", value, "User ID cannot be blank")
            }
            return UserId(value)
        }
    }
}

@JvmInline
value class SummaryId(val value: String) {
    companion object {
        /**
         * Create SummaryId with validation.
         * @throws ValidationException if value is blank
         */
        fun create(value: String): SummaryId {
            if (value.isBlank()) {
                throw ValidationException("SummaryId", value, "Summary ID cannot be blank")
            }
            return SummaryId(value)
        }
    }
}

@JvmInline
value class Timestamp(val value: Long) {
    companion object {
        /**
         * Create Timestamp with validation.
         * @throws ValidationException if value is negative
         */
        fun create(value: Long): Timestamp {
            if (value < 0) {
                throw ValidationException("Timestamp", value.toString(), "Timestamp cannot be negative")
            }
            return Timestamp(value)
        }

        /**
         * Create Timestamp with current time.
         */
        fun now(): Timestamp = Timestamp(System.currentTimeMillis())
    }
}

@JvmInline
value class ResponseTimeMs(val value: Long) {
    companion object {
        /**
         * Create ResponseTimeMs with validation.
         * @throws ValidationException if value is negative
         */
        fun create(value: Long): ResponseTimeMs {
            if (value < 0) {
                throw ValidationException("ResponseTimeMs", value.toString(), "Response time cannot be negative")
            }
            return ResponseTimeMs(value)
        }
    }
}
