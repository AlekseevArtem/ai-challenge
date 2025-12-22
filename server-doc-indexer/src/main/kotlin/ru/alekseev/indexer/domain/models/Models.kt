package ru.alekseev.indexer.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents a document chunk with its metadata
 */
data class DocumentChunk(
    val id: String,
    val filePath: String,
    val content: String,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
    val fileType: String,
    val embedding: List<Float>? = null
)

/**
 * Metadata about indexed documents
 */
@Serializable
data class IndexMetadata(
    val chunks: List<ChunkMetadata>,
    val totalFiles: Int,
    val totalChunks: Int,
    val indexedAt: String,
    val chunkSize: Int,
    val overlapSize: Int,
    val embeddingModel: String
)

/**
 * Serializable chunk metadata
 */
@Serializable
data class ChunkMetadata(
    val chunkId: String,
    val filePath: String,
    val content: String,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
    val fileType: String
)

/**
 * Represents a file to be processed
 */
data class FileItem(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val extension: String
)
