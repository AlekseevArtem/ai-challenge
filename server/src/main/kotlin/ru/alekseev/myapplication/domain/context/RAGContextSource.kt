package ru.alekseev.myapplication.domain.context

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.gateway.DocumentRetriever
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.domain.observability.RAGMetricsCollector
import ru.alekseev.myapplication.domain.rag.SimilarityThresholdFilter
import ru.alekseev.myapplication.usecase.FormatRAGContextUseCase
import kotlin.system.measureTimeMillis

/**
 * Provides RAG (Retrieval Augmented Generation) context from codebase.
 *
 * Searches for relevant code chunks based on the current message and
 * formats them into context for the LLM.
 *
 * Supports two modes:
 * - Enabled: Return top-K results without filtering
 * - EnabledWithFiltering: Apply similarity threshold filter
 *
 * Format: User message with formatted code chunks + Assistant acknowledgment
 *
 * Integrated with RAGMetricsCollector for observability.
 */
class RAGContextSource(
    private val documentRetriever: DocumentRetriever,
    private val formatRAGContextUseCase: FormatRAGContextUseCase,
    private val ragMetrics: RAGMetricsCollector,
    private val topK: Int = 3
) : ContextSource {
    override suspend fun getContext(
        userId: UserId,
        currentMessage: String,
        ragMode: RagMode
    ): List<ClaudeMessage> {
        when (ragMode) {
            is RagMode.Disabled -> {
                // No RAG context
                ragMetrics.recordSkipped("RAG disabled by user")
                return emptyList()
            }
            is RagMode.Enabled -> {
                // RAG without filtering
                if (!documentRetriever.isReady()) {
                    println("[RAGContextSource] Document retriever not ready, skipping RAG")
                    ragMetrics.recordSkipped("Document retriever not ready")
                    return emptyList()
                }

                println("[RAGContextSource] RAG enabled without filtering")

                // Measure retrieval time
                var searchResults: List<ru.alekseev.myapplication.domain.model.SearchResult>
                val retrievalDuration = measureTimeMillis {
                    searchResults = documentRetriever.search(currentMessage, topK = topK, filter = null)
                }

                ragMetrics.recordRetrieval(currentMessage, searchResults, retrievalDuration, filter = null)

                val ragContext = formatRAGContextUseCase(searchResults)

                if (ragContext.isBlank()) {
                    ragMetrics.recordSkipped("No results from search")
                    return emptyList()
                }

                ragMetrics.recordContextInjection(ragContext.length, searchResults.size, wasFiltered = false)

                return listOf(
                    ClaudeMessage(
                        role = "user",
                        content = ClaudeMessageContent.Text(ragContext)
                    ),
                    ClaudeMessage(
                        role = "assistant",
                        content = ClaudeMessageContent.Text("I understand. I'll use this context from the project codebase to answer your question.")
                    )
                )
            }
            is RagMode.EnabledWithFiltering -> {
                // RAG with similarity threshold filtering
                if (!documentRetriever.isReady()) {
                    println("[RAGContextSource] Document retriever not ready, skipping RAG")
                    ragMetrics.recordSkipped("Document retriever not ready")
                    return emptyList()
                }

                println("[RAGContextSource] RAG enabled with filtering (threshold: ${ragMode.threshold})")
                val filter = SimilarityThresholdFilter(ragMode.threshold)

                // Measure retrieval time
                var searchResults: List<ru.alekseev.myapplication.domain.model.SearchResult>
                val retrievalDuration = measureTimeMillis {
                    searchResults = documentRetriever.search(currentMessage, topK = topK, filter = filter)
                }

                ragMetrics.recordRetrieval(currentMessage, searchResults, retrievalDuration, filter = "SimilarityThreshold(${ragMode.threshold})")

                val ragContext = formatRAGContextUseCase(searchResults)

                if (ragContext.isBlank()) {
                    println("[RAGContextSource] No chunks passed the similarity threshold, proceeding without RAG context")
                    ragMetrics.recordSkipped("No results passed similarity threshold ${ragMode.threshold}")
                    return emptyList()
                }

                ragMetrics.recordContextInjection(ragContext.length, searchResults.size, wasFiltered = true)

                return listOf(
                    ClaudeMessage(
                        role = "user",
                        content = ClaudeMessageContent.Text(ragContext)
                    ),
                    ClaudeMessage(
                        role = "assistant",
                        content = ClaudeMessageContent.Text("I understand. I'll use this filtered context from the project codebase to answer your question.")
                    )
                )
            }
        }
    }
}
