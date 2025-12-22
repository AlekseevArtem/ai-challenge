package ru.alekseev.indexer

import kotlinx.coroutines.runBlocking
import ru.alekseev.indexer.data.mcp.MCPClient
import ru.alekseev.indexer.data.ollama.OllamaClient
import ru.alekseev.indexer.domain.pipeline.IndexingPipeline

/**
 * Main entry point for the document indexing application.
 *
 * Usage:
 *   ./gradlew :server-doc-indexer:run
 *
 * Prerequisites:
 *   1. MCP server must be running on http://localhost:8082
 *   2. Ollama must be running on http://localhost:11434
 *   3. nomic-embed-text model must be pulled in Ollama
 *
 * Output:
 *   - ./faiss_index/project.index (vector index)
 *   - ./faiss_index/metadata.json (chunk metadata)
 */
fun main(args: Array<String>) {
    val mcpUrl = args.getOrNull(0) ?: "http://localhost:8082"
    val ollamaUrl = args.getOrNull(1) ?: "http://localhost:11434"
    val outputDir = args.getOrNull(2) ?: "./faiss_index"

    val currentDir = System.getProperty("user.dir")

    println("Document Indexing Pipeline")
    println("===========================")
    println("Working Directory: $currentDir")
    println("MCP Server: $mcpUrl")
    println("Ollama Server: $ollamaUrl")
    println("Output Directory: $outputDir")
    println()

    val mcpClient = MCPClient(mcpUrl)
    val ollamaClient = OllamaClient(ollamaUrl, "nomic-embed-text")

    try {
        runBlocking {
            // Test connections first
            println("Testing connections...")
            try {
                mcpClient.listDirectory(".")
                println("✓ MCP server is reachable")
            } catch (e: Exception) {
                println("✗ MCP server is not reachable: ${e.message}")
                println("  Please start the MCP server first:")
                println("  ./start-devops-mcp.sh")
                return@runBlocking
            }

            try {
                ollamaClient.embed("test")
                println("✓ Ollama server is reachable")
            } catch (e: Exception) {
                println("✗ Ollama server is not reachable: ${e.message}")
                println("  Please ensure Ollama is running and the model is pulled:")
                println("  ollama pull nomic-embed-text")
                return@runBlocking
            }

            println()

            // Run the indexing pipeline
            val pipeline = IndexingPipeline(
                mcpClient = mcpClient,
                ollamaClient = ollamaClient,
                outputDir = outputDir,
                chunkSize = 1024,
                overlapSize = 100
            )

            pipeline.run()
        }
    } finally {
        mcpClient.close()
        ollamaClient.close()
    }

    println("\nDone!")
}
