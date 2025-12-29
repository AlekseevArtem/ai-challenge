package ru.alekseev.myapplication.config

import ru.alekseev.myapplication.core.common.OllamaDefaults
import ru.alekseev.myapplication.core.common.RAGDefaults

/**
 * Configuration for RAG (Retrieval Augmented Generation) service.
 * Loads settings from environment variables with sensible defaults.
 */
data class RAGConfig(
    val indexPath: String,
    val metadataPath: String,
    val ollamaUrl: String,
    val embeddingModel: String,
    val topK: Int,
    val chunkSize: Int,
    val overlapSize: Int
) {
    companion object {
        /**
         * Load RAG configuration from environment variables.
         * Falls back to defaults if environment variables are not set.
         */
        fun load(): RAGConfig {
            val indexDir = System.getenv("RAG_INDEX_DIR") ?: RAGDefaults.DEFAULT_INDEX_DIR

            return RAGConfig(
                indexPath = System.getenv("RAG_INDEX_PATH") ?: "$indexDir/${RAGDefaults.INDEX_FILENAME}",
                metadataPath = System.getenv("RAG_METADATA_PATH") ?: "$indexDir/${RAGDefaults.METADATA_FILENAME}",
                ollamaUrl = System.getenv("OLLAMA_URL") ?: OllamaDefaults.DOCKER_BASE_URL,
                embeddingModel = System.getenv("EMBEDDING_MODEL") ?: OllamaDefaults.EMBEDDING_MODEL,
                topK = System.getenv("RAG_TOP_K")?.toIntOrNull() ?: RAGDefaults.DEFAULT_TOP_K,
                chunkSize = System.getenv("RAG_CHUNK_SIZE")?.toIntOrNull() ?: RAGDefaults.DEFAULT_CHUNK_SIZE,
                overlapSize = System.getenv("RAG_OVERLAP_SIZE")?.toIntOrNull() ?: RAGDefaults.DEFAULT_OVERLAP_SIZE
            )
        }
    }
}
