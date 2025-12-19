package ru.alekseev.mcp.devops.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// JSON-RPC Request/Response Models
@Serializable
data class JSONRPCRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class JSONRPCResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: JSONRPCError? = null
)

@Serializable
data class JSONRPCError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

// MCP Protocol Models
@Serializable
data class ServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class ServerCapabilities(
    val tools: JsonObject? = null
)

@Serializable
data class InitializeResult(
    val protocolVersion: String = "2024-11-05",
    val serverInfo: ServerInfo,
    val capabilities: ServerCapabilities
)

@Serializable
data class Tool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class ListToolsResult(
    val tools: List<Tool>
)

@Serializable
data class CallToolParams(
    val name: String,
    val arguments: JsonObject? = null
)

@Serializable
data class TextContent(
    val type: String = "text",
    val text: String
)

@Serializable
data class CallToolResult(
    val content: List<TextContent>,
    val isError: Boolean = false
)
