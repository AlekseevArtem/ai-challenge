package ru.alekseev.mcp.devops

import kotlinx.serialization.json.JsonObject
import ru.alekseev.mcp.devops.models.Tool

/**
 * Interface for MCP tool providers.
 */
interface MCPToolProvider {
    /**
     * Returns the list of tools provided by this provider
     */
    fun getTools(): List<Tool>

    /**
     * Handles the execution of a tool
     * @param toolName - name of the tool to execute
     * @param arguments - arguments for the tool
     * @return result text from the tool execution
     * @throws IllegalArgumentException if tool is not supported or arguments are invalid
     */
    fun handleToolCall(toolName: String, arguments: JsonObject?): String

    /**
     * Checks if this provider supports the given tool
     */
    fun supportsTool(toolName: String): Boolean {
        return getTools().any { it.name == toolName }
    }
}
