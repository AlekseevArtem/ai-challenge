package ru.alekseev.myapplication.core.common

/**
 * Extension property to get a log tag based on the class name.
 * Usage: println("$logTag Starting service...")
 * Output: [ClassName] Starting service...
 */
val Any.logTag: String
    get() = "[${this::class.simpleName ?: "Unknown"}]"

// Порт сервера (внутренний)
const val SERVER_PORT = 8080

// Внешний порт для подключения клиента (Docker использует 8081)
const val SERVER_EXTERNAL_PORT = 8081

// Platform-specific server host
expect val SERVER_HOST: String

/**
 * Protocol prefixes for URLs
 */
object Protocols {
    const val WS = "ws://"
    const val HTTP = "http://"
    const val HTTPS = "https://"
}

/**
 * API endpoint paths
 */
object ApiEndpoints {
    const val CHAT = "/chat"
    const val MCP = "/mcp"
    const val HEALTH = "/health"
    const val OLLAMA_EMBEDDINGS = "/api/embeddings"
}

// WebSocket URL для подключения клиента
val SERVER_WS_URL: String
    get() = "${Protocols.WS}$SERVER_HOST:$SERVER_EXTERNAL_PORT${ApiEndpoints.CHAT}"

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

/**
 * Claude API message role identifiers
 */
object ClaudeRoles {
    const val USER = "user"
    const val ASSISTANT = "assistant"
    const val SYSTEM = "system"
}

/**
 * Ollama service configuration defaults
 */
object OllamaDefaults {
    const val DEFAULT_HOST = "localhost"
    const val DEFAULT_PORT = 11434
    const val DEFAULT_BASE_URL = "http://localhost:$DEFAULT_PORT"
    const val DOCKER_HOST = "host.docker.internal"
    const val DOCKER_BASE_URL = "http://$DOCKER_HOST:$DEFAULT_PORT"
    const val EMBEDDING_MODEL = "nomic-embed-text"
}

/**
 * MCP (Model Context Protocol) service defaults
 */
object MCPDefaults {
    const val DEFAULT_PORT = 8082
    const val DEFAULT_BASE_URL = "http://localhost:$DEFAULT_PORT"
}

/**
 * RAG (Retrieval Augmented Generation) configuration defaults
 */
object RAGDefaults {
    const val DEFAULT_INDEX_DIR = "./faiss_index"
    const val INDEX_FILENAME = "project.index"
    const val METADATA_FILENAME = "metadata.json"
    const val DEFAULT_THRESHOLD = 0.4f
    const val DEFAULT_TOP_K = 3
    const val DEFAULT_CHUNK_SIZE = 1024
    const val DEFAULT_OVERLAP_SIZE = 100
}

/**
 * OAuth configuration constants
 */
object OAuthDefaults {
    const val ACCESS_TYPE_OFFLINE = "offline"
    const val CREDENTIAL_USER_ID = "user"
}
