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
import kotlinx.serialization.json.JsonObject
import ru.alekseev.myapplication.data.dto.*

class ClaudeApiService(
    private val json: Json,
    private val mcpManager: MCPManager,
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

    private var mcpInitialized = false

    private fun loadApiKey(): String {
        System.getenv("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }?.let { return it }

        return ""
    }

    /**
     * Initialize MCP connections
     */
    suspend fun initializeMCP() {
        if (!mcpInitialized) {
            println("Initializing MCP connections...")
            mcpManager.connectAll()
            mcpInitialized = true
            println("MCP Manager initialized")

            // Log available tools
            val tools = mcpManager.getAllTools()
            println("Available MCP tools (${tools.size}): ${tools.map { it.name }}")
        }
    }

    /**
     * Get available MCP tools
     */
    suspend fun getMCPTools(): List<ClaudeTool> {
        if (!mcpInitialized) {
            initializeMCP()
        }
        return mcpManager.getAllTools()
    }

    /**
     * Send a message to Claude API with MCP tools and handle tool calls
     */
    suspend fun sendMessage(request: ClaudeRequest): ClaudeResponse {
        // Ensure MCP is initialized
        if (!mcpInitialized) {
            initializeMCP()
        }

        // Add MCP tools to the request if not already present
        val requestWithTools = if (request.tools == null) {
            val mcpTools = mcpManager.getAllTools()
            request.copy(tools = mcpTools)
        } else {
            request
        }

        // Send initial request
        var currentRequest = requestWithTools
        val conversationMessages = request.messages.toMutableList()

        while (true) {
            val response = try {
                val httpResponse = httpClient.post("https://api.anthropic.com/v1/messages") {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                    contentType(ContentType.Application.Json)
                    setBody(currentRequest)
                }

                httpResponse.body<ClaudeResponse>()
            } catch (e: Exception) {
                throw Exception("Failed to call Claude API: ${e.message}", e)
            }

            // Check if response contains tool_use
            val toolUses = response.content?.filter { it.type == "tool_use" } ?: emptyList()

            if (toolUses.isEmpty() || response.stopReason != "tool_use") {
                // No tool calls, return the response
                println("No more tool calls. Stop reason: ${response.stopReason}")
                return response
            }

            println("Received ${toolUses.size} tool use(s): ${toolUses.map { it.name }}")

            // Add assistant's response to conversation
            conversationMessages.add(
                ClaudeMessage(
                    role = "assistant",
                    content = ClaudeMessageContent.ContentBlocks(response.content ?: emptyList())
                )
            )

            // Execute tool calls
            val toolResults = mutableListOf<ClaudeContent>()
            for (toolUse in toolUses) {
                val toolName = toolUse.name ?: continue
                val toolInput = toolUse.input
                val toolUseId = toolUse.id ?: continue

                try {
                    println("Calling tool: $toolName with input: $toolInput")
                    val result = mcpManager.callTool(toolName, toolInput)
                    println("Tool $toolName returned: ${result.take(200)}${if (result.length > 200) "..." else ""}")
                    toolResults.add(
                        ClaudeContent(
                            type = "tool_result",
                            toolUseId = toolUseId,
                            content = kotlinx.serialization.json.JsonPrimitive(result)
                        )
                    )
                } catch (e: Exception) {
                    println("Tool $toolName failed with error: ${e.message}")
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
            conversationMessages.add(
                ClaudeMessage(
                    role = "user",
                    content = ClaudeMessageContent.ContentBlocks(toolResults)
                )
            )

            // Continue conversation with tool results
            currentRequest = currentRequest.copy(messages = conversationMessages)
        }
    }

    fun close() {
        httpClient.close()
    }
}