package ru.alekseev.indexer.domain.pipeline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ru.alekseev.indexer.data.index.VectorIndex
import ru.alekseev.indexer.data.mcp.MCPClient
import ru.alekseev.indexer.data.ollama.OllamaClient
import ru.alekseev.indexer.domain.chunking.TextChunker
import ru.alekseev.indexer.domain.crawler.FileCrawler
import ru.alekseev.indexer.domain.models.ChunkMetadata
import ru.alekseev.indexer.domain.models.DocumentChunk
import ru.alekseev.indexer.domain.models.IndexMetadata
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.core.common.OllamaDefaults
import ru.alekseev.myapplication.core.common.RAGDefaults
import java.io.File
import java.time.Instant
import java.util.*

/**
 * Main pipeline for indexing project documents.
 * Orchestrates the entire process from crawling to embedding to indexing.
 */
class IndexingPipeline(
    private val mcpClient: MCPClient,
    private val ollamaClient: OllamaClient,
    private val outputDir: String = RAGDefaults.DEFAULT_INDEX_DIR,
    private val chunkSize: Int = 1024,
    private val overlapSize: Int = 100
) {
    private val textChunker = TextChunker(chunkSize, overlapSize)
    private val fileCrawler = FileCrawler(mcpClient)
    private val vectorIndex = VectorIndex()
    private val json = JsonFactory.create()

    /**
     * Run the complete indexing pipeline
     */
    suspend fun run() {
        println("=== Starting Document Indexing Pipeline ===\n")

        // Step 1: Crawl project files
        println("Step 1: Crawling project files...")
        val files = fileCrawler.crawl(".")
        println("Found ${files.size} files to process\n")

        // Step 2: Read and chunk files
        println("Step 2: Reading and chunking files...")
        val allChunks = mutableListOf<DocumentChunk>()
        var processedFiles = 0

        for (file in files) {
            val content = fileCrawler.readFile(file) ?: continue
            processedFiles++

            val chunks = textChunker.chunk(content)
            println("  ${file.path}: ${chunks.size} chunks")

            chunks.forEachIndexed { index, chunk ->
                val chunkId = "${UUID.randomUUID()}"
                allChunks.add(
                    DocumentChunk(
                        id = chunkId,
                        filePath = file.path,
                        content = chunk.content,
                        startTokenIndex = chunk.startTokenIndex,
                        endTokenIndex = chunk.endTokenIndex,
                        fileType = file.extension
                    )
                )
            }
        }

        println("\nTotal chunks created: ${allChunks.size}")
        println("Files processed: $processedFiles\n")

        // Step 3: Generate embeddings
        println("Step 3: Generating embeddings using Ollama (${ollamaClient})...")
        val chunksWithEmbeddings = mutableListOf<DocumentChunk>()

        for ((index, chunk) in allChunks.withIndex()) {
            if (index > 0 && index % 10 == 0) {
                println("  Progress: $index / ${allChunks.size}")
            }

            try {
                val embedding = withContext(Dispatchers.IO) {
                    ollamaClient.embed(chunk.content)
                }

                chunksWithEmbeddings.add(chunk.copy(embedding = embedding))
            } catch (e: Exception) {
                System.err.println("  Warning: Failed to embed chunk ${chunk.id}: ${e.message}")
            }
        }

        println("Embeddings generated: ${chunksWithEmbeddings.size}\n")

        // Step 4: Build vector index
        println("Step 4: Building vector index...")
        vectorIndex.addAll(chunksWithEmbeddings)
        println("Index size: ${vectorIndex.size()} vectors\n")

        // Step 5: Save to disk
        println("Step 5: Saving index to disk...")
        val outputDirFile = File(outputDir)
        outputDirFile.mkdirs()

        val indexFile = File(outputDirFile, RAGDefaults.INDEX_FILENAME)
        vectorIndex.save(indexFile)
        println("Index saved to: ${indexFile.absolutePath}")

        // Save metadata
        val metadataFile = File(outputDirFile, RAGDefaults.METADATA_FILENAME)
        val metadata = IndexMetadata(
            chunks = chunksWithEmbeddings.map {
                ChunkMetadata(
                    chunkId = it.id,
                    filePath = it.filePath,
                    content = it.content,
                    startTokenIndex = it.startTokenIndex,
                    endTokenIndex = it.endTokenIndex,
                    fileType = it.fileType
                )
            },
            totalFiles = processedFiles,
            totalChunks = chunksWithEmbeddings.size,
            indexedAt = Instant.now().toString(),
            chunkSize = chunkSize,
            overlapSize = overlapSize,
            embeddingModel = OllamaDefaults.EMBEDDING_MODEL
        )

        metadataFile.writeText(json.encodeToString(metadata))
        println("Metadata saved to: ${metadataFile.absolutePath}\n")

        println("=== Indexing Complete ===")
        println("Summary:")
        println("  Files processed: $processedFiles")
        println("  Total chunks: ${chunksWithEmbeddings.size}")
        println("  Chunk size: $chunkSize tokens")
        println("  Overlap: $overlapSize tokens")
        println("  Index location: ${indexFile.absolutePath}")
    }
}
