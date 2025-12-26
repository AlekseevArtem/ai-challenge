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
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway

class ClaudeApiService(
    private val json: Json,
    private val mcpManager: MCPManager,
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
        println("[ClaudeApiService] Loading API key from environment...")
        val key = System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }
        if (key != null) {
            println("[ClaudeApiService] API key loaded successfully (length: ${key.length})")
            return key
        } else {
            println("[ClaudeApiService] WARNING: API key not found or empty!")
            return ""
        }
    }

    /**
     * Initialize MCP connections
     */
    override suspend fun initialize() {
        if (!mcpInitialized) {
            println("[ClaudeApiService] Initializing MCP connections...")
            try {
                mcpManager.connectAll()
                mcpInitialized = true
                println("[ClaudeApiService] MCP Manager initialized successfully")

                // Log available tools
                val tools = mcpManager.getAllTools()
                println("[ClaudeApiService] Available MCP tools (${tools.size}): ${tools.map { it.name }}")
            } catch (e: Exception) {
                println("[ClaudeApiService] ERROR: Failed to initialize MCP: ${e.message}")
                e.printStackTrace()
                throw e
            }
        } else {
            println("[ClaudeApiService] MCP already initialized, skipping...")
        }
    }

    /**
     * Get available MCP tools
     */
    suspend fun getMCPTools(): List<ClaudeTool> {
        println("[ClaudeApiService] Getting MCP tools...")
        if (!mcpInitialized) {
            println("[ClaudeApiService] MCP not initialized, initializing now...")
            initialize()
        }
        val tools = mcpManager.getAllTools()
        println("[ClaudeApiService] Retrieved ${tools.size} MCP tools")
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

                println("[ClaudeApiService] Received HTTP response with status: ${httpResponse.status}")

                // Check status code before parsing
                when (httpResponse.status) {
                    HttpStatusCode.OK -> {
                        // Success - parse and return
                        val claudeResponse = httpResponse.body<ClaudeResponse>()
                        println("[ClaudeApiService] Response parsed successfully")
                        println("[ClaudeApiService] Response details: id=${claudeResponse.id}, model=${claudeResponse.model}, stop_reason=${claudeResponse.stopReason}")
                        println("[ClaudeApiService] Response content blocks: ${claudeResponse.content?.size ?: 0}")
                        return claudeResponse
                    }

                    HttpStatusCode.TooManyRequests -> {
                        // Rate limit - retry with backoff
                        println("[ClaudeApiService] Rate limit exceeded (429). Attempt $attempt/$maxRetries")

                        if (attempt >= maxRetries) {
                            println("[ClaudeApiService] Max retries reached, throwing exception")
                            throw Exception("Rate limit exceeded after $maxRetries attempts")
                        }

                        // Get retry-after header (in seconds) or use exponential backoff
                        val retryAfter = httpResponse.headers["retry-after"]?.toLongOrNull()
                        val waitTimeMs = if (retryAfter != null) {
                            retryAfter * 1000
                        } else {
                            // Exponential backoff: 2^attempt seconds
                            (1L shl attempt) * 1000
                        }

                        println("[ClaudeApiService] Waiting ${waitTimeMs / 1000}s before retry...")
                        delay(waitTimeMs)
                        println("[ClaudeApiService] Retrying request...")
                        continue
                    }

                    else -> {
                        // Other error - throw exception
                        val errorBody = try {
                            httpResponse.body<String>()
                        } catch (e: Exception) {
                            "Unable to read error body"
                        }
                        println("[ClaudeApiService] ERROR: HTTP ${httpResponse.status.value}: $errorBody")
                        throw Exception("Claude API request failed with status ${httpResponse.status}: $errorBody")
                    }
                }

            } catch (e: ClientRequestException) {
                // This shouldn't happen anymore since we check status above,
                // but keep as fallback
                println("[ClaudeApiService] ERROR: ClientRequestException: ${e.message}")
                throw Exception("Failed to call Claude API: ${e.message}", e)
            } catch (e: Exception) {
                println("[ClaudeApiService] ERROR: Failed to call Claude API: ${e.message}")
                e.printStackTrace()
                throw Exception("Failed to call Claude API: ${e.message}", e)
            }
        }
    }

    /**
     * Send a message to Claude API with MCP tools and handle tool calls
     */
    override suspend fun sendMessage(request: ClaudeRequest): ClaudeResponse {
        println("[ClaudeApiService] sendMessage called")
        println("[ClaudeApiService] Request details: model=${request.model}, max_tokens=${request.maxTokens}, messages=${request.messages.size}")

        // Ensure MCP is initialized
        if (!mcpInitialized) {
            println("[ClaudeApiService] MCP not initialized, initializing...")
            initialize()
        }

        // Add MCP tools to the request if not already present
        val requestWithTools = if (request.tools == null) {
            println("[ClaudeApiService] Adding MCP tools to request...")
            val mcpTools = mcpManager.getAllTools()
            println("[ClaudeApiService] Added ${mcpTools.size} MCP tools to request")
            request.copy(tools = mcpTools)
        } else {
            println("[ClaudeApiService] Request already has ${request.tools?.size ?: 0} tools")
            request
        }

        // Send initial request
        var currentRequest = requestWithTools
        val conversationMessages = request.messages.toMutableList()
        var iterationCount = 0

        println("[ClaudeApiService] Starting conversation loop...")

        while (true) {
            iterationCount++
            println("[ClaudeApiService] ===== Iteration $iterationCount =====")
            println("[ClaudeApiService] Sending request to Claude API...")
            println("[ClaudeApiService] Current conversation has ${conversationMessages.size} messages")

            val response = executeWithRetry(currentRequest)

            // Check if response contains tool_use
            val toolUses = response.content?.filter { it.type == "tool_use" } ?: emptyList()
            println("[ClaudeApiService] Found ${toolUses.size} tool_use blocks in response")

            if (toolUses.isEmpty() || response.stopReason != "tool_use") {
                // No tool calls, return the response
                println("[ClaudeApiService] No more tool calls. Stop reason: ${response.stopReason}")
                println("[ClaudeApiService] Returning final response after $iterationCount iteration(s)")
                return response
            }

            println("[ClaudeApiService] Processing ${toolUses.size} tool use(s): ${toolUses.map { it.name }}")

            // Add assistant's response to conversation
            println("[ClaudeApiService] Adding assistant response to conversation history")
            conversationMessages.add(
                ClaudeMessage(
                    role = "assistant",
                    content = ClaudeMessageContent.ContentBlocks(response.content ?: emptyList())
                )
            )

            // Execute tool calls
            println("[ClaudeApiService] Executing ${toolUses.size} tool call(s)...")
            val toolResults = mutableListOf<ClaudeContent>()
            for ((index, toolUse) in toolUses.withIndex()) {
                val toolName = toolUse.name
                val toolInput = toolUse.input
                val toolUseId = toolUse.id

                if (toolName == null || toolUseId == null) {
                    println("[ClaudeApiService] WARNING: Tool use ${index + 1} missing name or id, skipping")
                    continue
                }

                try {
                    println("[ClaudeApiService] [${index + 1}/${toolUses.size}] Calling tool: $toolName")
                    println("[ClaudeApiService] Tool input: $toolInput")

                    val result = mcpManager.callTool(toolName, toolInput)
                    val truncatedResult = if (result.length > 200) {
                        "${result.take(200)}... (${result.length} chars total)"
                    } else {
                        result
                    }
                    println("[ClaudeApiService] Tool $toolName completed successfully")
                    println("[ClaudeApiService] Tool result: $truncatedResult")

                    toolResults.add(
                        ClaudeContent(
                            type = "tool_result",
                            toolUseId = toolUseId,
                            content = kotlinx.serialization.json.JsonPrimitive(result)
                        )
                    )
                } catch (e: Exception) {
                    println("[ClaudeApiService] ERROR: Tool $toolName failed with error: ${e.message}")
                    e.printStackTrace()
                    toolResults.add(
                        ClaudeContent(
                            type = "tool_result",
                            toolUseId = toolUseId,
                            content = kotlinx.serialization.json.JsonPrimitive("Error: ${e.message}"),
                            isError = true
                        )
                    )
                }
            }

            // Add tool results to conversation
            println("[ClaudeApiService] Adding ${toolResults.size} tool result(s) to conversation")
            conversationMessages.add(
                ClaudeMessage(
                    role = "user",
                    content = ClaudeMessageContent.ContentBlocks(toolResults)
                )
            )

            // Continue conversation with tool results
            println("[ClaudeApiService] Continuing conversation with updated messages")
            currentRequest = currentRequest.copy(messages = conversationMessages)
        }
    }

    override fun close() {
        println("[ClaudeApiService] Closing HTTP client...")
        httpClient.close()
        println("[ClaudeApiService] HTTP client closed")
    }
}