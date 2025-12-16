package ru.alekseev.myapplication.service

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.alekseev.myapplication.data.dto.ClaudeTool
import ru.alekseev.myapplication.data.dto.MCPTool

/**
 * Manager for multiple MCP clients.
 * Handles connecting to MCP servers, collecting tools, and routing tool calls.
 */
class MCPManager {
    private val clients = mutableMapOf<String, MCPClient>()
    private val toolToClient = mutableMapOf<String, String>()
    private val mutex = Mutex()

    /**
     * Register an MCP client with a unique name
     */
    fun registerClient(name: String, client: MCPClient) {
        clients[name] = client
    }

    /**
     * Connect to all registered MCP clients
     */
    suspend fun connectAll() {
        mutex.withLock {
            clients.values.forEach { client ->
                try {
                    client.connect()
                    System.err.println("Successfully connected to MCP client: $client")
                } catch (e: Exception) {
                    System.err.println("Failed to connect to MCP client $client: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Get all available tools from all connected MCP clients
     */
    suspend fun getAllTools(): List<ClaudeTool> {
        val allTools = mutableListOf<ClaudeTool>()

        clients.forEach { (clientName, client) ->
            if (client.isConnected) {
                try {
                    val mcpTools = client.listTools()
                    mcpTools.forEach { mcpTool ->
                        // Map tool to client for routing
                        toolToClient[mcpTool.name] = clientName

                        // Convert MCPTool to ClaudeTool
                        allTools.add(
                            ClaudeTool(
                                name = mcpTool.name,
                                description = mcpTool.description,
                                inputSchema = mcpTool.inputSchema
                            )
                        )
                    }
                } catch (e: Exception) {
                    System.err.println("Failed to list tools from MCP client $clientName: ${e.message}")
                    e.printStackTrace()
                }
            }
        }

        return allTools
    }

    /**
     * Call a tool by routing to the appropriate MCP client
     */
    suspend fun callTool(toolName: String, arguments: kotlinx.serialization.json.JsonObject?): String {
        val clientName = toolToClient[toolName]
            ?: throw IllegalArgumentException("Unknown tool: $toolName")

        val client = clients[clientName]
            ?: throw IllegalStateException("MCP client not found: $clientName")

        if (!client.isConnected) {
            throw IllegalStateException("MCP client is not connected: $clientName")
        }

        val result = client.callTool(toolName, arguments)

        if (result.isError) {
            throw Exception("Tool call failed: ${result.content.firstOrNull()?.text ?: "Unknown error"}")
        }

        return result.content.firstOrNull()?.text ?: ""
    }

    /**
     * Disconnect all MCP clients
     */
    suspend fun disconnectAll() {
        mutex.withLock {
            clients.values.forEach { client ->
                try {
                    client.disconnect()
                } catch (e: Exception) {
                    System.err.println("Error disconnecting MCP client: ${e.message}")
                }
            }
            toolToClient.clear()
        }
    }

    /**
     * Get information about registered clients
     */
    fun getClientsInfo(): Map<String, Boolean> {
        return clients.mapValues { (_, client) -> client.isConnected }
    }
}
