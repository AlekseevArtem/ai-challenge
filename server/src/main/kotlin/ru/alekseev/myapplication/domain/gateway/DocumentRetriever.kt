package ru.alekseev.myapplication.domain.gateway

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
     * Get context for a query by searching relevant documents.
     * Returns formatted context string ready to be added to Claude prompts.
     *
     * @param query The search query (typically the user's message)
     * @param topK Number of top results to return
     * @param filter Optional filter to apply relevance thresholds
     * @return Formatted context string, or empty string if no relevant results
     */
    suspend fun getContextForQuery(
        query: String,
        topK: Int = 5,
        filter: ChunkRelevanceFilter? = null
    ): String

    /**
     * Close any resources held by this retriever.
     */
    fun close()
}
