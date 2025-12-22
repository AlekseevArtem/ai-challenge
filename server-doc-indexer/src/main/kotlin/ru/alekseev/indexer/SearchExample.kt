package ru.alekseev.indexer

import kotlinx.coroutines.runBlocking
import ru.alekseev.indexer.data.index.VectorIndex
import ru.alekseev.indexer.data.ollama.OllamaClient
import java.io.File

/**
 * Example: Search the indexed project using semantic similarity
 *
 * Usage:
 *   ./gradlew :server-doc-indexer:runSearch --args="your search query"
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("Usage: ./gradlew :server-doc-indexer:runSearch --args=\"your search query\"")
        println("Example: ./gradlew :server-doc-indexer:runSearch --args=\"how to build android app\"")
        return
    }

    val query = args.joinToString(" ")
    val indexPath = "./faiss_index/project.index"

    println("Searching index for: \"$query\"")
    println()

    if (!File(indexPath).exists()) {
        println("Error: Index not found at $indexPath")
        println("Please run the indexer first: ./run-indexer.sh")
        return
    }

    runBlocking {
        // Load the index
        val index = VectorIndex()
        index.load(File(indexPath))
        println("Loaded index with ${index.size()} vectors")
        println()

        // Generate query embedding
        val ollamaClient = OllamaClient()
        println("Generating query embedding...")
        val queryEmbedding = ollamaClient.embed(query)
        println()

        // Search
        println("Searching for similar chunks...")
        val results = index.search(queryEmbedding, topK = 5)
        println()

        // Display results
        println("Top ${results.size} results:")
        println("=" * 80)
        println()

        results.forEachIndexed { index, result ->
            println("${index + 1}. Similarity: ${"%.4f".format(result.similarity)}")
            println("   File: ${result.metadata.filePath}")
            println("   Tokens: ${result.metadata.startToken}-${result.metadata.endToken}")
            println("   Type: ${result.metadata.fileType}")
            println()
        }

        ollamaClient.close()
    }
}

private operator fun String.times(n: Int) = repeat(n)
