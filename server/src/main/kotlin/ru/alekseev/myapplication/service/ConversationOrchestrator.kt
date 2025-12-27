package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.ClaudeContent
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.data.dto.ClaudeResponse
import ru.alekseev.myapplication.domain.observability.ConversationMetricsCollector
import kotlin.system.measureTimeMillis

/**
 * Orchestrates multi-turn conversations with Claude API.
 * Handles tool use loops where Claude requests tools, executes them,
 * and continues the conversation with results.
 *
 * Responsibilities:
 * - Managing conversation state across multiple turns
 * - Detecting and handling tool_use stop reasons
 * - Executing tools via MCP
 * - Building conversation history with tool results
 * - Recording metrics for tool execution and performance
 */
class ConversationOrchestrator(
    private val mcpManager: MCPManager,
    private val conversationMetrics: ConversationMetricsCollector
) {
    /**
     * Execute a multi-turn conversation with tool use support.
     * Continues the conversation until Claude stops requesting tools.
     *
     * @param initialRequest The initial request with messages and tools
     * @param executeRequest Function to execute a single API request (handles HTTP, retry, etc.)
     * @return The final response after all tool uses are complete
     */
    suspend fun orchestrateConversation(
        initialRequest: ClaudeRequest,
        executeRequest: suspend (ClaudeRequest) -> ClaudeResponse
    ): ClaudeResponse {
        var currentRequest = initialRequest
        val conversationMessages = initialRequest.messages.toMutableList()
        var iterationCount = 0

        println("[ConversationOrchestrator] Starting conversation loop...")

        while (true) {
            iterationCount++
            println("[ConversationOrchestrator] ===== Iteration $iterationCount =====")
            println("[ConversationOrchestrator] Sending request to Claude API...")
            println("[ConversationOrchestrator] Current conversation has ${conversationMessages.size} messages")

            val response = executeRequest(currentRequest)

            // Check if response contains tool_use
            val toolUses = response.content?.filter { it.type == "tool_use" } ?: emptyList()
            println("[ConversationOrchestrator] Found ${toolUses.size} tool_use blocks in response")

            if (toolUses.isEmpty() || response.stopReason != "tool_use") {
                // No tool calls, return the response
                println("[ConversationOrchestrator] No more tool calls. Stop reason: ${response.stopReason}")
                println("[ConversationOrchestrator] Returning final response after $iterationCount iteration(s)")
                return response
            }

            println("[ConversationOrchestrator] Processing ${toolUses.size} tool use(s): ${toolUses.map { it.name }}")

            // Add assistant's response to conversation
            println("[ConversationOrchestrator] Adding assistant response to conversation history")
            conversationMessages.add(
                ClaudeMessage(
                    role = "assistant",
                    content = ClaudeMessageContent.ContentBlocks(response.content ?: emptyList())
                )
            )

            // Execute tool calls
            println("[ConversationOrchestrator] Executing ${toolUses.size} tool call(s)...")
            val toolResults = executeTools(toolUses)

            // Add tool results to conversation
            println("[ConversationOrchestrator] Adding ${toolResults.size} tool result(s) to conversation")
            conversationMessages.add(
                ClaudeMessage(
                    role = "user",
                    content = ClaudeMessageContent.ContentBlocks(toolResults)
                )
            )

            // Continue conversation with tool results
            println("[ConversationOrchestrator] Continuing conversation with updated messages")
            currentRequest = currentRequest.copy(messages = conversationMessages)
        }
    }

    /**
     * Execute all tool calls and return their results.
     */
    private suspend fun executeTools(toolUses: List<ClaudeContent>): List<ClaudeContent> {
        val toolResults = mutableListOf<ClaudeContent>()

        for ((index, toolUse) in toolUses.withIndex()) {
            val toolName = toolUse.name
            val toolInput = toolUse.input
            val toolUseId = toolUse.id

            if (toolName == null || toolUseId == null) {
                println("[ConversationOrchestrator] WARNING: Tool use ${index + 1} missing name or id, skipping")
                continue
            }

            try {
                println("[ConversationOrchestrator] [${index + 1}/${toolUses.size}] Calling tool: $toolName")
                println("[ConversationOrchestrator] Tool input: $toolInput")

                // Measure tool execution time
                var result: String
                val toolDuration = measureTimeMillis {
                    result = mcpManager.callTool(toolName, toolInput)
                }

                val truncatedResult = if (result.length > 200) {
                    "${result.take(200)}... (${result.length} chars total)"
                } else {
                    result
                }
                println("[ConversationOrchestrator] Tool $toolName completed successfully")
                println("[ConversationOrchestrator] Tool result: $truncatedResult")

                // Record successful tool call
                conversationMetrics.recordToolCall(toolName, success = true, toolDuration, errorMessage = null)

                toolResults.add(
                    ClaudeContent(
                        type = "tool_result",
                        toolUseId = toolUseId,
                        content = kotlinx.serialization.json.JsonPrimitive(result)
                    )
                )
            } catch (e: Exception) {
                println("[ConversationOrchestrator] ERROR: Tool $toolName failed with error: ${e.message}")
                e.printStackTrace()

                // Record failed tool call
                conversationMetrics.recordToolCall(toolName, success = false, 0, errorMessage = e.message)

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

        return toolResults
    }
}
