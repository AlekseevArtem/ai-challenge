package ru.alekseev.mcp.devops

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.alekseev.mcp.devops.models.*
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.core.common.logTag

/**
 * Main MCP Server class that handles JSONRPC protocol.
 * Delegates tool execution to registered tool providers.
 */
class MCPServer(
    private val toolProviders: List<MCPToolProvider>
) {
    private val json = JsonFactory.create()

    fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("$logTag Handling request: method=${request.method}, id=${request.id}")
        return try {
            val response = when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleListTools(request)
                "tools/call" -> handleCallTool(request)
                else -> {
                    System.err.println("$logTag ERROR: Method not found: ${request.method}")
                    JSONRPCResponse(
                        id = request.id,
                        error = JSONRPCError(-32601, "Method not found: ${request.method}")
                    )
                }
            }
            System.err.println("$logTag Successfully handled request: method=${request.method}")
            response
        } catch (e: Exception) {
            System.err.println("$logTag ERROR: Internal error handling request: ${e.message}")
            e.printStackTrace(System.err)
            JSONRPCResponse(
                id = request.id,
                error = JSONRPCError(-32603, "Internal error: ${e.message}")
            )
        }
    }

    private fun handleInitialize(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("$logTag Initializing server...")
        val result = InitializeResult(
            serverInfo = ServerInfo(
                name = "devops-mcp",
                version = "1.0.0"
            ),
            capabilities = ServerCapabilities(
                tools = buildJsonObject { }
            )
        )
        System.err.println("$logTag Server initialized: name=devops-mcp, version=1.0.0")

        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleListTools(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("$logTag Listing available tools...")

        // Collect all tools from all providers
        val tools = toolProviders.flatMap { it.getTools() }

        System.err.println("$logTag Found ${tools.size} tools: ${tools.map { it.name }}")

        val result = ListToolsResult(tools)
        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleCallTool(request: JSONRPCRequest): JSONRPCResponse {
        val params = request.params?.let {
            json.decodeFromJsonElement<CallToolParams>(it)
        } ?: run {
            System.err.println("$logTag ERROR: Invalid params in tool call")
            return JSONRPCResponse(
                id = request.id,
                error = JSONRPCError(-32602, "Invalid params")
            )
        }

        System.err.println("$logTag Calling tool: ${params.name}")
        System.err.println("$logTag Tool arguments: ${params.arguments}")

        val resultText = try {
            // Find the provider that supports this tool
            val provider = toolProviders.firstOrNull { it.supportsTool(params.name) }
                ?: throw IllegalArgumentException("Unknown tool: ${params.name}")

            // Delegate to the provider
            provider.handleToolCall(params.name, params.arguments)
        } catch (e: Exception) {
            System.err.println("$logTag ERROR: Tool call failed: ${e.message}")
            e.printStackTrace(System.err)
            return JSONRPCResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    CallToolResult(
                        content = listOf(TextContent(text = "Error: ${e.message}")),
                        isError = true
                    )
                )
            )
        }

        val result = CallToolResult(
            content = listOf(TextContent(text = resultText))
        )

        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }
}
