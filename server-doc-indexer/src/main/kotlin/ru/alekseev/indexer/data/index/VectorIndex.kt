package ru.alekseev.indexer.data.index

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ru.alekseev.indexer.domain.models.DocumentChunk
import ru.alekseev.myapplication.core.common.JsonFactory
import java.io.File
import kotlin.math.sqrt

/**
 * Simple vector index for storing and searching document embeddings.
 * Provides FAISS-like functionality with disk persistence.
 */
class VectorIndex {
    private val vectors = mutableListOf<IndexedVector>()
    private val json = JsonFactory.create()

    /**
     * Add a document chunk with its embedding to the index
     */
    fun add(chunk: DocumentChunk) {
        require(chunk.embedding != null) { "Chunk must have an embedding" }

        vectors.add(
            IndexedVector(
                id = chunk.id,
                embedding = chunk.embedding,
                metadata = VectorMetadata(
                    filePath = chunk.filePath,
                    startToken = chunk.startTokenIndex,
                    endToken = chunk.endTokenIndex,
                    fileType = chunk.fileType
                )
            )
        )
    }

    /**
     * Add multiple chunks to the index
     */
    fun addAll(chunks: List<DocumentChunk>) {
        chunks.forEach { add(it) }
    }

    /**
     * Search for similar vectors using cosine similarity
     */
    fun search(queryEmbedding: List<Float>, topK: Int = 10): List<SearchResult> {
        if (vectors.isEmpty()) return emptyList()

        return vectors
            .map { vector ->
                val similarity = cosineSimilarity(queryEmbedding, vector.embedding)
                SearchResult(
                    id = vector.id,
                    similarity = similarity,
                    metadata = vector.metadata
                )
            }
            .sortedByDescending { it.similarity }
            .take(topK)
    }

    /**
     * Save the index to disk
     */
    fun save(indexFile: File) {
        indexFile.parentFile?.mkdirs()

        val indexData = IndexData(
            version = "1.0",
            dimension = vectors.firstOrNull()?.embedding?.size ?: 0,
            count = vectors.size,
            vectors = vectors
        )

        indexFile.writeText(json.encodeToString(IndexData.serializer(), indexData))
    }

    /**
     * Load the index from disk
     */
    fun load(indexFile: File) {
        if (!indexFile.exists()) {
            throw IllegalArgumentException("Index file does not exist: ${indexFile.absolutePath}")
        }

        val indexData = json.decodeFromString(IndexData.serializer(), indexFile.readText())

        vectors.clear()
        vectors.addAll(indexData.vectors)
    }

    /**
     * Get the number of vectors in the index
     */
    fun size(): Int = vectors.size

    /**
     * Calculate cosine similarity between two vectors
     */
    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        require(a.size == b.size) { "Vectors must have the same dimension" }

        var dotProduct = 0f
        var normA = 0f
        var normB = 0f

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
}

@Serializable
private data class IndexData(
    val version: String,
    val dimension: Int,
    val count: Int,
    val vectors: List<IndexedVector>
)

@Serializable
data class IndexedVector(
    val id: String,
    val embedding: List<Float>,
    val metadata: VectorMetadata
)

@Serializable
data class VectorMetadata(
    val filePath: String,
    val startToken: Int,
    val endToken: Int,
    val fileType: String
)

data class SearchResult(
    val id: String,
    val similarity: Float,
    val metadata: VectorMetadata
)
