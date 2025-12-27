package ru.alekseev.myapplication.infrastructure.observability

import ru.alekseev.myapplication.domain.model.SearchResult
import ru.alekseev.myapplication.domain.observability.RAGMetricsCollector

/**
 * Console-based RAG metrics collector for development and debugging.
 *
 * Prints metrics to stdout in a structured format.
 * Can be easily replaced with structured logging or metrics systems in production.
 */
class PrintlnRAGMetrics : RAGMetricsCollector {
    override fun recordRetrieval(
        query: String,
        results: List<SearchResult>,
        durationMs: Long,
        filter: String?
    ) {
        println("[RAG_METRICS] Retrieval completed in ${durationMs}ms")
        println("[RAG_METRICS]   Query: ${query.take(100)}${if (query.length > 100) "..." else ""}")
        println("[RAG_METRICS]   Results: ${results.size} chunks")
        if (results.isNotEmpty()) {
            println("[RAG_METRICS]   Top similarity: ${"%.3f".format(results.first().similarity)}")
            println("[RAG_METRICS]   Lowest similarity: ${"%.3f".format(results.last().similarity)}")
        }
        if (filter != null) {
            println("[RAG_METRICS]   Filter: $filter")
        }
    }

    override fun recordContextInjection(
        contextLength: Int,
        chunkCount: Int,
        wasFiltered: Boolean
    ) {
        println("[RAG_METRICS] Context injected: $contextLength chars, $chunkCount chunks, filtered=$wasFiltered")
    }

    override fun recordSkipped(reason: String) {
        println("[RAG_METRICS] RAG skipped: $reason")
    }
}
