package ru.alekseev.myapplication.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
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
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.*
import ru.alekseev.myapplication.domain.exception.GatewayException
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway

class ClaudeApiService(
    private val json: Json,
    private val mcpManager: MCPManager,
    private val conversationOrchestrator: ConversationOrchestrator
) : ClaudeGateway {
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

    private var mcpInitialized = false

    private fun loadApiKey(): String {
        println("$logTag Loading API key from environment...")
        val key = System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }
        if (key != null) {
            println("$logTag API key loaded successfully (length: ${key.length})")
            return key
        } else {
            println("$logTag WARNING: API key not found or empty!")
            return ""
        }
    }

    /**
     * Initialize MCP connections
     */
    override suspend fun initialize() {
        if (!mcpInitialized) {
            println("$logTag Initializing MCP connections...")
            try {
                mcpManager.connectAll()
                mcpInitialized = true
                println("$logTag MCP Manager initialized successfully")

                // Log available tools
                val tools = mcpManager.getAllTools()
                println("$logTag Available MCP tools (${tools.size}): ${tools.map { it.name }}")
            } catch (e: Exception) {
                println("$logTag ERROR: Failed to initialize MCP: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("$logTag MCP already initialized, skipping...")
        }
    }

    /**
     * Get available MCP tools
     */
    suspend fun getMCPTools(): List<ClaudeTool> {
        println("$logTag Getting MCP tools...")
        if (!mcpInitialized) {
            println("$logTag MCP not initialized, initializing now...")
            initialize()
        }
        val tools = mcpManager.getAllTools()
        println("$logTag Retrieved ${tools.size} MCP tools")
        return tools
    }

    /**
     * Execute Claude API request with retry on 429 errors
     */
    private suspend fun executeWithRetry(
        request: ClaudeRequest,
        maxRetries: Int = 3
    ): ClaudeResponse {
        var attempt = 0

        while (true) {
            attempt++
            try {
                val httpResponse = httpClient.post("https://api.anthropic.com/v1/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }

                println("$logTag Received HTTP response with status: ${httpResponse.status}")

                // Check status code before parsing
                when (httpResponse.status) {
                    HttpStatusCode.OK -> {
                        // Success - parse and return
                        val claudeResponse = httpResponse.body<ClaudeResponse>()
                        println("$logTag Response parsed successfully")
                        println("$logTag Response details: id=${claudeResponse.id}, model=${claudeResponse.model}, stop_reason=${claudeResponse.stopReason}")
                        println("$logTag Response content blocks: ${claudeResponse.content?.size ?: 0}")
                        return claudeResponse
                    }

                    HttpStatusCode.TooManyRequests -> {
                        // Rate limit - retry with backoff
                        println("$logTag Rate limit exceeded (429). Attempt $attempt/$maxRetries")

                        if (attempt >= maxRetries) {
                            println("$logTag Max retries reached, throwing exception")
                            throw GatewayException("Claude API", "Rate limit exceeded after $maxRetries attempts")
                        }

                        // Get retry-after header (in seconds) or use exponential backoff
                        val retryAfter = httpResponse.headers["retry-after"]?.toLongOrNull()
                        val waitTimeMs = if (retryAfter != null) {
                            retryAfter * 1000
                        } else {
                            // Exponential backoff: 2^attempt seconds
                            (1L shl attempt) * 1000
                        }

                        println("$logTag Waiting ${waitTimeMs / 1000}s before retry...")
                        delay(waitTimeMs)
                        println("$logTag Retrying request...")
                        continue
                    }

                    else -> {
                        // Other error - throw exception
                        val errorBody = try {
                            httpResponse.body<String>()
                        } catch (e: Exception) {
                            "Unable to read error body"
                        }
                        println("$logTag ERROR: HTTP ${httpResponse.status.value}: $errorBody")
                        throw GatewayException("Claude API", "Request failed with status ${httpResponse.status}: $errorBody")
                    }
                }

            } catch (e: GatewayException) {
                // Re-throw domain exceptions as-is
                throw e
            } catch (e: ClientRequestException) {
                // This shouldn't happen anymore since we check status above,
                // but keep as fallback
                println("$logTag ERROR: ClientRequestException: ${e.message}")
                throw GatewayException("Claude API", "HTTP request failed: ${e.message}", e)
            } catch (e: Exception) {
                println("$logTag ERROR: Failed to call Claude API: ${e.message}")
                e.printStackTrace()
                throw GatewayException("Claude API", "Unexpected error: ${e.message}", e)
            }
        }
    }

    /**
     * Send a message to Claude API with MCP tools and handle tool calls
     */
    override suspend fun sendMessage(request: ClaudeRequest): ClaudeResponse {
        println("$logTag sendMessage called")
        println("$logTag Request details: model=${request.model}, max_tokens=${request.maxTokens}, messages=${request.messages.size}")

        // Ensure MCP is initialized
        if (!mcpInitialized) {
            println("$logTag MCP not initialized, initializing...")
            initialize()
        }

        // Add MCP tools to the request if not already present
        val requestWithTools = if (request.tools == null) {
            println("$logTag Adding MCP tools to request...")
            val mcpTools = mcpManager.getAllTools()
            println("$logTag Added ${mcpTools.size} MCP tools to request")
            request.copy(tools = mcpTools)
        } else {
            println("$logTag Request already has ${request.tools?.size ?: 0} tools")
            request
        }

        // Use ConversationOrchestrator to handle multi-turn conversation with tool use
        return conversationOrchestrator.orchestrateConversation(requestWithTools) { req ->
            executeWithRetry(req)
        }
    }

    override fun close() {
        println("$logTag Closing HTTP client...")
        httpClient.close()
        println("$logTag HTTP client closed")
    }
}