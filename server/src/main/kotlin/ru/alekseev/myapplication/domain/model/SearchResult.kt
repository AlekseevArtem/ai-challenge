package ru.alekseev.myapplication.domain.model

/**
 * Result of a document search in the RAG system.
 * Represents a relevant code chunk found via semantic search.
 *
 * Domain model - used by RAG filters and retrieval logic.
 */
data class SearchResult(
    val similarity: Float,
    val filePath: String,
    val content: String,
    val fileType: String,
    val startToken: Int,
    val endToken: Int
)
