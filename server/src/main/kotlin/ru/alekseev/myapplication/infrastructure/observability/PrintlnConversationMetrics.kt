package ru.alekseev.myapplication.infrastructure.observability

import ru.alekseev.myapplication.domain.observability.ConversationMetricsCollector

/**
 * Console-based conversation metrics collector for development and debugging.
 *
 * Prints metrics to stdout in a structured format.
 * Can be easily replaced with APM systems or structured logging in production.
 */
class PrintlnConversationMetrics : ConversationMetricsCollector {
    override fun recordToolCall(
        toolName: String,
        success: Boolean,
        durationMs: Long,
        errorMessage: String?
    ) {
        val status = if (success) "SUCCESS" else "FAILED"
        println("[CONVERSATION_METRICS] Tool call: $toolName - $status in ${durationMs}ms")
        if (errorMessage != null) {
            println("[CONVERSATION_METRICS]   Error: $errorMessage")
        }
    }

    override fun recordTokenUsage(
        inputTokens: Int,
        outputTokens: Int,
        model: String,
        cost: Double
    ) {
        println("[CONVERSATION_METRICS] Token usage: in=$inputTokens, out=$outputTokens, model=$model, cost=$${String.format("%.4f", cost)}")
    }

    override fun recordMessageProcessed(
        userId: String,
        totalDurationMs: Long,
        ragUsed: Boolean,
        toolCallsCount: Int
    ) {
        println("[CONVERSATION_METRICS] Message processed for user=$userId in ${totalDurationMs}ms, RAG=$ragUsed, tools=$toolCallsCount")
    }
}
