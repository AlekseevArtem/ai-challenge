package ru.alekseev.myapplication.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ru.alekseev.indexer.data.index.VectorIndex
import ru.alekseev.indexer.data.mcp.MCPClient
import ru.alekseev.indexer.data.ollama.OllamaClient
import ru.alekseev.indexer.domain.models.IndexMetadata
import ru.alekseev.indexer.domain.pipeline.IndexingPipeline
import ru.alekseev.myapplication.config.RAGConfig
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.core.common.MCPDefaults
import ru.alekseev.myapplication.core.common.logTag
import ru.alekseev.myapplication.domain.gateway.DocumentRetriever
import ru.alekseev.myapplication.domain.model.SearchResult
import ru.alekseev.myapplication.domain.rag.ChunkRelevanceFilter
import java.io.File

/**
 * RAG (Retrieval Augmented Generation) service for document search.
 * Provides semantic search over indexed project documents.
 */
class DocumentRAGService(
    private val config: RAGConfig,
    private val mcpUrl: String = MCPDefaults.DEFAULT_BASE_URL
) : DocumentRetriever {
    private val indexPath: String = config.indexPath
    private val metadataPath: String = config.metadataPath
    private val vectorIndex = VectorIndex()
    private val ollamaClient = OllamaClient(config.ollamaUrl, config.embeddingModel)
    private val json = JsonFactory.create()

    private var metadata: IndexMetadata? = null
    private var isIndexLoaded = false

    /**
     * Initialize the service by loading the index from disk
     */
    override suspend fun initialize() {
        withContext(Dispatchers.IO) {
            val indexFile = File(indexPath)

            if (indexFile.exists()) {
                try {
                    println("$logTag Loading index from ${indexFile.absolutePath}")
                    vectorIndex.load(indexFile)
                    isIndexLoaded = true
                    println("$logTag Index loaded successfully with ${vectorIndex.size()} vectors")

                    // Load metadata
                    val metadataFile = File(metadataPath)
                    if (metadataFile.exists()) {
                        metadata = json.decodeFromString(
                            IndexMetadata.serializer(),
                            metadataFile.readText()
                        )
                        println("$logTag Metadata loaded: ${metadata?.totalChunks} chunks from ${metadata?.totalFiles} files")
                    }
                } catch (e: Exception) {
                    println("$logTag Warning: Failed to load index: ${e.message}")
                    isIndexLoaded = false
                }
            } else {
                println("$logTag Index not found at ${indexFile.absolutePath}")
                println("$logTag You can create the index by calling reindex() or running the indexer manually")
                isIndexLoaded = false
            }
        }
    }

    /**
     * Search for relevant documents based on a query
     * Implements DocumentRetriever.search()
     * @param query The search query
     * @param topK Number of top results to return
     * @param filter Optional relevance filter to apply to results
     */
    override suspend fun search(
        query: String,
        topK: Int,
        filter: ChunkRelevanceFilter?
    ): List<SearchResult> {
        if (!isIndexLoaded) {
            println("$logTag Index not loaded, returning empty results")
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
                println("$logTag Search failed: ${e.message}")
                emptyList()
            }
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
                    chunkSize = config.chunkSize,
                    overlapSize = config.overlapSize
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
    override fun isReady(): Boolean = isIndexLoaded

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

    override fun close() {
        ollamaClient.close()
    }
}

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
