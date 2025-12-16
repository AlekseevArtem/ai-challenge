package ru.alekseev.myapplication.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// JSON-RPC Request/Response Models for MCP
@Serializable
data class MCPRequest(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val method: String,
    val params: JsonObject? = null
)

@Serializable
data class MCPResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: MCPError? = null
)

@Serializable
data class MCPError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

// MCP Protocol Models
@Serializable
data class MCPServerInfo(
    val name: String,
    val version: String
)

@Serializable
data class MCPServerCapabilities(
    val tools: JsonObject? = null
)

@Serializable
data class MCPInitializeResult(
    val protocolVersion: String = "2024-11-05",
    val serverInfo: MCPServerInfo,
    val capabilities: MCPServerCapabilities
)

@Serializable
data class MCPTool(
    val name: String,
    val description: String,
    val inputSchema: JsonObject
)

@Serializable
data class MCPListToolsResult(
    val tools: List<MCPTool>
)

@Serializable
data class MCPCallToolParams(
    val name: String,
    val arguments: JsonObject? = null
)

@Serializable
data class MCPTextContent(
    val type: String = "text",
    val text: String
)

@Serializable
data class MCPCallToolResult(
    val content: List<MCPTextContent>,
    val isError: Boolean = false
)
