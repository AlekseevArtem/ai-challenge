package ru.alekseev.myapplication.domain.observability

/**
 * Interface for collecting conversation and LLM interaction metrics.
 *
 * Enables:
 * - Tool usage analytics (which tools, success rate, latency)
 * - Token usage tracking (cost monitoring)
 * - Performance monitoring (response time by model)
 * - Error tracking and debugging
 *
 * Implementations can:
 * - Log to console (PrintlnConversationMetrics)
 * - Send to structured logging
 * - Push to APM systems
 * - Store for billing/cost analysis
 */
interface ConversationMetricsCollector {
    /**
     * Record a tool call execution.
     *
     * @param toolName Name of the tool called
     * @param success Whether the tool executed successfully
     * @param durationMs Time taken for tool execution in milliseconds
     * @param errorMessage Error message if failed (null if success)
     */
    fun recordToolCall(
        toolName: String,
        success: Boolean,
        durationMs: Long,
        errorMessage: String? = null
    )

    /**
     * Record token usage for an LLM API call.
     *
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @param model Model used (e.g., "claude-sonnet-4-5-20250929")
     * @param cost Estimated cost in USD
     */
    fun recordTokenUsage(
        inputTokens: Int,
        outputTokens: Int,
        model: String,
        cost: Double
    )

    /**
     * Record overall message processing metrics.
     *
     * @param userId User who sent the message
     * @param totalDurationMs Total time from request to response
     * @param ragUsed Whether RAG was used
     * @param toolCallsCount Number of tool calls made
     */
    fun recordMessageProcessed(
        userId: String,
        totalDurationMs: Long,
        ragUsed: Boolean,
        toolCallsCount: Int
    )
}
