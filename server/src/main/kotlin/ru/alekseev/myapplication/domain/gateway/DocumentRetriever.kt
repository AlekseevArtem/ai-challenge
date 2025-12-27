package ru.alekseev.myapplication.domain.gateway

import ru.alekseev.myapplication.domain.model.SearchResult
import ru.alekseev.myapplication.domain.rag.ChunkRelevanceFilter

/**
 * Domain interface for retrieving relevant documents using RAG.
 * This is a port in hexagonal architecture - it defines what the domain needs
 * from document retrieval services without coupling to specific implementations.
 *
 * Implementations handle:
 * - Vector indexing and search
 * - Embedding generation
 * - Similarity scoring
 * - Document chunking and metadata
 *
 * NOTE: This interface returns raw SearchResults. Formatting for LLM prompts
 * should be done by use cases (e.g., FormatRAGContextUseCase), not by this gateway.
 */
interface DocumentRetriever {
    /**
     * Initialize the retriever by loading indexes and establishing connections.
     * Should be called before first use.
     */
    suspend fun initialize()

    /**
     * Check if the retriever is ready to handle queries.
     * Returns false if indexes aren't loaded or initialization failed.
     *
     * @return true if ready for queries, false otherwise
     */
    fun isReady(): Boolean

    /**
     * Search for relevant documents based on a query.
     * Returns raw search results with similarity scores.
     *
     * @param query The search query (typically the user's message)
     * @param topK Number of top results to return
     * @param filter Optional filter to apply relevance thresholds
     * @return List of search results ordered by relevance, empty if no relevant results
     */
    suspend fun search(
        query: String,
        topK: Int = 5,
        filter: ChunkRelevanceFilter? = null
    ): List<SearchResult>

    /**
     * Close any resources held by this retriever.
     */
    fun close()
}
