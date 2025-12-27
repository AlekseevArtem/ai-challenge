package ru.alekseev.myapplication.domain.observability

import ru.alekseev.myapplication.domain.model.SearchResult

/**
 * Interface for collecting RAG (Retrieval Augmented Generation) metrics.
 *
 * Enables:
 * - Evaluating RAG quality (which chunks, relevance scores)
 * - Measuring retrieval performance (latency)
 * - A/B testing (RAG on vs off)
 * - Production monitoring
 *
 * Implementations can:
 * - Log to console (PrintlnRAGMetrics)
 * - Send to structured logging (StructuredLoggingMetrics)
 * - Push to metrics system (PrometheusMetrics, DatadogMetrics)
 * - Store in evaluation DB (EvaluationMetrics)
 */
interface RAGMetricsCollector {
    /**
     * Record a RAG retrieval event.
     *
     * @param query The user query that triggered retrieval
     * @param results The search results returned
     * @param durationMs Time taken for retrieval in milliseconds
     * @param filter Filter applied (if any)
     */
    fun recordRetrieval(
        query: String,
        results: List<SearchResult>,
        durationMs: Long,
        filter: String? = null
    )

    /**
     * Record RAG context injection into LLM prompt.
     *
     * @param contextLength Length of formatted context in characters
     * @param chunkCount Number of chunks included
     * @param wasFiltered Whether similarity filtering was applied
     */
    fun recordContextInjection(
        contextLength: Int,
        chunkCount: Int,
        wasFiltered: Boolean
    )

    /**
     * Record when RAG was skipped (e.g., not ready, disabled, no results)
     *
     * @param reason Why RAG was skipped
     */
    fun recordSkipped(reason: String)
}
