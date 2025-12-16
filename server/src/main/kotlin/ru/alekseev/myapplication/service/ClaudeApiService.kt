package ru.alekseev.myapplication.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.data.dto.ClaudeResponse

class ClaudeApiService(
    private val json: Json,
) {
    private val apiKey: String by lazy {
        loadApiKey()
    }

    private val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000  // 60 seconds
            connectTimeoutMillis = 10_000  // 10 seconds
            socketTimeoutMillis = 60_000   // 60 seconds
        }

        install(ContentNegotiation) {
            json(this@ClaudeApiService.json)
        }
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.INFO
        }
    }

    private fun loadApiKey(): String {
        System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    suspend fun sendMessage(request: ClaudeRequest): ClaudeResponse {
        return try {
            val response = httpClient.post("https://api.anthropic.com/v1/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", "2023-06-01")
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            val result = response.body<ClaudeResponse>()
            result
        } catch (e: Exception) {
            throw Exception("Failed to call Claude API: ${e.message}", e)
        }
    }

    fun close() {
        httpClient.close()
    }
}