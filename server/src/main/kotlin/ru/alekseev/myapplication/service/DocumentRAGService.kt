package ru.alekseev.myapplication.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.alekseev.indexer.data.index.VectorIndex
import ru.alekseev.indexer.data.mcp.MCPClient
import ru.alekseev.indexer.data.ollama.OllamaClient
import ru.alekseev.indexer.domain.models.IndexMetadata
import ru.alekseev.indexer.domain.pipeline.IndexingPipeline
import ru.alekseev.myapplication.domain.rag.ChunkRelevanceFilter
import java.io.File

/**
 * RAG (Retrieval Augmented Generation) service for document search.
 * Provides semantic search over indexed project documents.
 */
class DocumentRAGService(
    private val indexPath: String = "./faiss_index/project.index",
    private val metadataPath: String = "./faiss_index/metadata.json",
    private val ollamaUrl: String = "http://localhost:11434",
    private val mcpUrl: String = "http://localhost:8082"
) {
    private val vectorIndex = VectorIndex()
    private val ollamaClient = OllamaClient(ollamaUrl, "nomic-embed-text")
    private val json = Json { ignoreUnknownKeys = true }

    private var metadata: IndexMetadata? = null
    private var isIndexLoaded = false

    /**
     * Initialize the service by loading the index from disk
     */
    suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val indexFile = File(indexPath)

            if (indexFile.exists()) {
                try {
                    println("[DocumentRAGService] Loading index from ${indexFile.absolutePath}")
                    vectorIndex.load(indexFile)
                    isIndexLoaded = true
                    println("[DocumentRAGService] Index loaded successfully with ${vectorIndex.size()} vectors")

                    // Load metadata
                    val metadataFile = File(metadataPath)
                    if (metadataFile.exists()) {
                        metadata = json.decodeFromString(
                            IndexMetadata.serializer(),
                            metadataFile.readText()
                        )
                        println("[DocumentRAGService] Metadata loaded: ${metadata?.totalChunks} chunks from ${metadata?.totalFiles} files")
                    }
                } catch (e: Exception) {
                    println("[DocumentRAGService] Warning: Failed to load index: ${e.message}")
                    isIndexLoaded = false
                }
            } else {
                println("[DocumentRAGService] Index not found at ${indexFile.absolutePath}")
                println("[DocumentRAGService] You can create the index by calling reindex() or running the indexer manually")
                isIndexLoaded = false
            }
        }
    }

    /**
     * Search for relevant documents based on a query
     * @param query The search query
     * @param topK Number of top results to return
     * @param filter Optional relevance filter to apply to results
     */
    suspend fun search(
        query: String,
        topK: Int = 5,
        filter: ChunkRelevanceFilter? = null
    ): List<SearchResult> {
        if (!isIndexLoaded) {
            println("[DocumentRAGService] Index not loaded, returning empty results")
            return emptyList()
        }

        return withContext(Dispatchers.IO) {
            try {
                // Generate query embedding
                val queryEmbedding = ollamaClient.embed(query)

                // Search the index
                val results = vectorIndex.search(queryEmbedding, topK)

                // Convert to SearchResult with chunk content from metadata
                val searchResults = results.mapNotNull { result ->
                    val chunkMetadata = metadata?.chunks?.find { it.chunkId == result.id }
                    if (chunkMetadata != null) {
                        SearchResult(
                            similarity = result.similarity,
                            filePath = chunkMetadata.filePath,
                            content = chunkMetadata.content,
                            fileType = chunkMetadata.fileType,
                            startToken = chunkMetadata.startTokenIndex,
                            endToken = chunkMetadata.endTokenIndex
                        )
                    } else {
                        null
                    }
                }

                // Apply filter if provided
                filter?.filter(searchResults) ?: searchResults
            } catch (e: Exception) {
                println("[DocumentRAGService] Search failed: ${e.message}")
                emptyList()
            }
        }
    }

    /**
     * Get context string from search results to add to Claude prompt
     * @param query The search query
     * @param topK Number of top results to return
     * @param filter Optional relevance filter to apply to results
     */
    suspend fun getContextForQuery(
        query: String,
        topK: Int = 5,
        filter: ChunkRelevanceFilter? = null
    ): String {
        val results = search(query, topK, filter)

        if (results.isEmpty()) {
            return ""
        }

        return buildString {
            appendLine("Here is relevant context from the project codebase:")
            appendLine()
            results.forEachIndexed { index, result ->
                appendLine("--- Document ${index + 1} (${result.filePath}, similarity: ${"%.3f".format(result.similarity)}) ---")
                appendLine(result.content.take(1000)) // Limit to 1000 chars per chunk
                if (result.content.length > 1000) {
                    appendLine("... (truncated)")
                }
                appendLine()
            }
            appendLine("--- End of Context ---")
        }
    }

    /**
     * Trigger re-indexing of the project
     */
    suspend fun reindex() {
        withContext(Dispatchers.IO) {
            try {
                println("[DocumentRAGService] Starting re-indexing...")

                val mcpClient = MCPClient(mcpUrl)
                val pipeline = IndexingPipeline(
                    mcpClient = mcpClient,
                    ollamaClient = ollamaClient,
                    outputDir = File(indexPath).parent,
                    chunkSize = 1024,
                    overlapSize = 100
                )

                pipeline.run()

                mcpClient.close()

                // Reload the index
                initialize()

                println("[DocumentRAGService] Re-indexing completed")
            } catch (e: Exception) {
                println("[DocumentRAGService] Re-indexing failed: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Check if the index is loaded and ready
     */
    fun isReady(): Boolean = isIndexLoaded

    /**
     * Get index statistics
     */
    fun getStats(): IndexStats {
        return IndexStats(
            isLoaded = isIndexLoaded,
            totalVectors = if (isIndexLoaded) vectorIndex.size() else 0,
            totalFiles = metadata?.totalFiles ?: 0,
            totalChunks = metadata?.totalChunks ?: 0,
            indexedAt = metadata?.indexedAt
        )
    }

    fun close() {
        ollamaClient.close()
    }
}

/**
 * Result of a document search
 */
data class SearchResult(
    val similarity: Float,
    val filePath: String,
    val content: String,
    val fileType: String,
    val startToken: Int,
    val endToken: Int
)

/**
 * Statistics about the loaded index
 */
data class IndexStats(
    val isLoaded: Boolean,
    val totalVectors: Int,
    val totalFiles: Int,
    val totalChunks: Int,
    val indexedAt: String?
)
