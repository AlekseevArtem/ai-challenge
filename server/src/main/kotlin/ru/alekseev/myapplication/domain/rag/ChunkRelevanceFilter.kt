package ru.alekseev.myapplication.domain.rag

import ru.alekseev.myapplication.service.SearchResult

/**
 * Domain abstraction for filtering search results based on relevance.
 * Follows Clean Architecture principles by separating filtering logic from service implementation.
 */
interface ChunkRelevanceFilter {
    /**
     * Filters search results based on relevance criteria.
     * @param chunks List of search results from vector search
     * @return Filtered list of search results
     */
    fun filter(chunks: List<SearchResult>): List<SearchResult>
}
