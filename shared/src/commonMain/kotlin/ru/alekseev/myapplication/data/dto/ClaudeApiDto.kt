package ru.alekseev.myapplication.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class ClaudeRequest(
    val model: String = "claude-haiku-4-5-20251001", // claude-sonnet-4-5-20250929
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    val messages: List<ClaudeMessage>,
    val stream: Boolean = false,
    val tools: List<ClaudeTool>? = null,
)

@Serializable
data class ClaudeMessage(
    val role: String, // "user" or "assistant"
    val content: ClaudeMessageContent,
)

@Serializable(with = ClaudeMessageContentSerializer::class)
sealed class ClaudeMessageContent {
    @Serializable
    data class Text(val text: String) : ClaudeMessageContent()

    @Serializable
    data class ContentBlocks(val blocks: List<ClaudeContent>) : ClaudeMessageContent()
}

@Serializable
data class ClaudeResponse(
    val id: String? = null,
    val type: String? = null,
    val role: String? = null,
    val content: List<ClaudeContent>? = null,
    val model: String? = null,
    @SerialName("stop_reason")
    val stopReason: String? = null,
    @SerialName("stop_sequence")
    val stopSequence: String? = null,
    val usage: ClaudeUsage? = null,
)

@Serializable
data class ClaudeContent(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val name: String? = null,
    val input: JsonObject? = null,
    @SerialName("tool_use_id")
    val toolUseId: String? = null,
    val content: JsonElement? = null,
    @SerialName("is_error")
    val isError: Boolean? = null,
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int,
)

@Serializable
data class ClaudeTool(
    val name: String,
    val description: String,
    @SerialName("input_schema")
    val inputSchema: JsonObject,
)