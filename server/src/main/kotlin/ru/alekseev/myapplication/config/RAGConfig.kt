package ru.alekseev.myapplication.config

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
            val indexDir = System.getenv("RAG_INDEX_DIR") ?: "./faiss_index"

            return RAGConfig(
                indexPath = System.getenv("RAG_INDEX_PATH") ?: "$indexDir/project.index",
                metadataPath = System.getenv("RAG_METADATA_PATH") ?: "$indexDir/metadata.json",
                ollamaUrl = System.getenv("OLLAMA_URL") ?: "http://host.docker.internal:11434",
                embeddingModel = System.getenv("EMBEDDING_MODEL") ?: "nomic-embed-text",
                topK = System.getenv("RAG_TOP_K")?.toIntOrNull() ?: 3,
                chunkSize = System.getenv("RAG_CHUNK_SIZE")?.toIntOrNull() ?: 1024,
                overlapSize = System.getenv("RAG_OVERLAP_SIZE")?.toIntOrNull() ?: 100
            )
        }
    }
}
