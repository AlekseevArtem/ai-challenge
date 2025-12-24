package ru.alekseev.myapplication.domain.rag

import ru.alekseev.myapplication.service.SearchResult

/**
 * Filters search results based on similarity score threshold.
 *
 * @param threshold Minimum similarity score (0.0 to 1.0). Chunks with similarity below this value are filtered out.
 */
class SimilarityThresholdFilter(
    private val threshold: Float
) : ChunkRelevanceFilter {

    init {
        require(threshold in 0.0f..1.0f) { "Threshold must be between 0.0 and 1.0, got $threshold" }
    }

    override fun filter(chunks: List<SearchResult>): List<SearchResult> {
        println("[SimilarityThresholdFilter] filter input $chunks ${chunks.size}")
        val filtered = chunks.filter { it.similarity >= threshold }
        println("[SimilarityThresholdFilter] filter output $filtered ${filtered.size}")
        return filtered
    }
}
