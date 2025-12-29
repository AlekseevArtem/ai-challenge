package ru.alekseev.indexer.data.ollama

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import ru.alekseev.myapplication.core.common.OllamaDefaults
import ru.alekseev.myapplication.core.common.ApiEndpoints
import ru.alekseev.myapplication.core.common.JsonFactory

/**
 * Client for Ollama API to generate embeddings
 */
class OllamaClient(
    private val baseUrl: String = OllamaDefaults.DEFAULT_BASE_URL,
    private val model: String = OllamaDefaults.EMBEDDING_MODEL
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(JsonFactory.create())
        }
    }

    /**
     * Generate embedding for a single text
     */
    suspend fun embed(text: String): List<Float> {
        val response: EmbeddingResponse = client.post("$baseUrl${ApiEndpoints.OLLAMA_EMBEDDINGS}") {
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
