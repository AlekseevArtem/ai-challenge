package ru.alekseev.myapplication.usecase

import ru.alekseev.myapplication.domain.model.SearchResult

/**
 * Formats RAG search results into a context string suitable for LLM prompts.
 *
 * Responsibilities:
 * - Format search results into readable context
 * - Truncate content to prevent excessive token usage
 * - Add metadata (file paths, similarity scores)
 * - Provide clear delimiters for context boundaries
 *
 * Single Responsibility: Transform SearchResult domain objects into formatted LLM context.
 * Separated from DocumentRetriever to keep infrastructure layer clean.
 */
class FormatRAGContextUseCase {
    /**
     * Format search results into context string for LLM.
     *
     * @param results List of search results from RAG retrieval
     * @param maxCharsPerChunk Maximum characters to include per chunk (default 1000)
     * @return Formatted context string, or empty if no results
     */
    operator fun invoke(
        results: List<SearchResult>,
        maxCharsPerChunk: Int = 1000
    ): String {
        if (results.isEmpty()) {
            return ""
        }

        return buildString {
            appendLine("Here is relevant context from the project codebase:")
            appendLine()
            results.forEachIndexed { index, result ->
                appendLine("--- Document ${index + 1} (${result.filePath}, similarity: ${"%.3f".format(result.similarity)}) ---")
                appendLine(result.content.take(maxCharsPerChunk))
                if (result.content.length > maxCharsPerChunk) {
                    appendLine("... (truncated)")
                }
                appendLine()
            }
            appendLine("--- End of Context ---")
        }
    }
}
