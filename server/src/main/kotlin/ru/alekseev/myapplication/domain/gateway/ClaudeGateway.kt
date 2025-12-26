package ru.alekseev.myapplication.domain.gateway

import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.data.dto.ClaudeResponse

/**
 * Domain interface for interacting with Claude API.
 * This is a port in hexagonal architecture - it defines what the domain needs
 * from external AI services without coupling to specific implementations.
 *
 * Implementations handle:
 * - API communication with Claude
 * - Tool/MCP integration
 * - Retry logic and error handling
 * - Multi-turn conversations with tool use
 */
interface ClaudeGateway {
    /**
     * Initialize the gateway and any required connections (e.g., MCP tools).
     * Should be called before first use.
     */
    suspend fun initialize()

    /**
     * Send a message to Claude and get a response.
     * Handles tool use, multi-turn conversations, and retries automatically.
     *
     * @param request The Claude API request with messages and configuration
     * @return The Claude API response containing the assistant's reply
     * @throws Exception if the request fails after retries
     */
    suspend fun sendMessage(request: ClaudeRequest): ClaudeResponse

    /**
     * Close any resources held by this gateway (e.g., HTTP clients).
     */
    fun close()
}
