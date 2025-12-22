package ru.alekseev.indexer.data.ollama

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Client for Ollama API to generate embeddings
 */
class OllamaClient(
    private val baseUrl: String = "http://localhost:11434",
    private val model: String = "nomic-embed-text"
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }

    /**
     * Generate embedding for a single text
     */
    suspend fun embed(text: String): List<Float> {
        val response: EmbeddingResponse = client.post("$baseUrl/api/embeddings") {
            contentType(ContentType.Application.Json)
            setBody(
                EmbeddingRequest(
                    model = model,
                    prompt = text
                )
            )
        }.body()

        return response.embedding
    }

    /**
     * Generate embeddings for multiple texts (batch processing)
     */
    suspend fun embedBatch(texts: List<String>): List<List<Float>> {
        return texts.map { embed(it) }
    }

    fun close() {
        client.close()
    }
}

@Serializable
private data class EmbeddingRequest(
    val model: String,
    val prompt: String
)

@Serializable
private data class EmbeddingResponse(
    val embedding: List<Float>
)
