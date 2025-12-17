package ru.alekseev.myapplication.core.common

// Порт сервера (внутренний)
const val SERVER_PORT = 8080

// Внешний порт для подключения клиента (Docker использует 8081)
const val SERVER_EXTERNAL_PORT = 8081

// Platform-specific server host
expect val SERVER_HOST: String

// WebSocket URL для подключения клиента
val SERVER_WS_URL: String
    get() = "ws://$SERVER_HOST:$SERVER_EXTERNAL_PORT/chat"

/**
 * Application-wide constants for the chat server
 */
object ChatConstants {
    /**
     * Default user identifier for single-user mode
     */
    const val DEFAULT_USER_ID = "default_user"

    /**
     * Suffix appended to message ID to create user message identifier
     */
    const val USER_MESSAGE_ID_SUFFIX = "_user"

    /**
     * Number of uncompressed messages that triggers automatic summarization
     */
    const val SUMMARIZATION_THRESHOLD = 5

    /**
     * Timestamp offset in milliseconds for user messages to ensure correct ordering
     */
    const val USER_MESSAGE_TIMESTAMP_OFFSET = 1L
}

/**
 * Claude API model identifiers and pricing
 */
object ClaudeModels {
    const val SONNET_4_5 = "claude-sonnet-4-5-20250929"
    const val HAIKU_4 = "claude-haiku-4"

    /**
     * Default model used when model is not specified in response
     */
    const val DEFAULT_MODEL = SONNET_4_5
}

/**
 * Pricing for Claude API models per 1M tokens (as of 2025)
 */
object ClaudePricing {
    /**
     * Sonnet 4.5 pricing: input cost per 1M tokens, output cost per 1M tokens
     */
    val SONNET_4_PRICING = Pair(3.0, 15.0)

    /**
     * Haiku 4 pricing: input cost per 1M tokens, output cost per 1M tokens
     */
    val HAIKU_4_PRICING = Pair(0.25, 1.25)

    /**
     * Default pricing when model is unknown
     */
    val DEFAULT_PRICING = SONNET_4_PRICING

    /**
     * Divisor for converting token count to millions (for cost calculation)
     */
    const val TOKENS_PER_MILLION = 1_000_000.0
}
