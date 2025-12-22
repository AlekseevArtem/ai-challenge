package ru.alekseev.indexer.domain.chunking

/**
 * Chunks text into overlapping segments based on token count.
 * Uses simple whitespace tokenization.
 */
class TextChunker(
    private val chunkSize: Int = 1024,
    private val overlapSize: Int = 100
) {
    init {
        require(chunkSize > 0) { "Chunk size must be positive" }
        require(overlapSize >= 0) { "Overlap size must be non-negative" }
        require(overlapSize < chunkSize) { "Overlap size must be less than chunk size" }
    }

    /**
     * Split text into chunks with overlap.
     * Returns list of TextChunk objects with token indices.
     */
    fun chunk(text: String): List<TextChunk> {
        val tokens = tokenize(text)

        if (tokens.isEmpty()) {
            return emptyList()
        }

        val chunks = mutableListOf<TextChunk>()
        var startIndex = 0

        while (startIndex < tokens.size) {
            val endIndex = minOf(startIndex + chunkSize, tokens.size)

            val chunkTokens = tokens.subList(startIndex, endIndex)
            val chunkText = chunkTokens.joinToString(" ")

            chunks.add(
                TextChunk(
                    content = chunkText,
                    startTokenIndex = startIndex,
                    endTokenIndex = endIndex,
                    tokenCount = chunkTokens.size
                )
            )

            // Move forward by (chunkSize - overlapSize) tokens
            // This ensures exactly overlapSize tokens overlap between chunks
            val step = chunkSize - overlapSize
            startIndex += step

            // Stop if we've reached the end
            if (endIndex >= tokens.size) break
        }

        return chunks
    }

    /**
     * Simple whitespace tokenization.
     * Splits on whitespace and filters out empty tokens.
     */
    private fun tokenize(text: String): List<String> {
        return text.split(Regex("\\s+"))
            .filter { it.isNotBlank() }
    }

    /**
     * Estimate token count for a text string
     */
    fun estimateTokenCount(text: String): Int {
        return tokenize(text).size
    }
}

/**
 * Represents a chunk of text with its token boundaries
 */
data class TextChunk(
    val content: String,
    val startTokenIndex: Int,
    val endTokenIndex: Int,
    val tokenCount: Int
)
