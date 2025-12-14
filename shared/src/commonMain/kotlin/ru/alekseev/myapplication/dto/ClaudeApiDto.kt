package ru.alekseev.myapplication.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClaudeRequest(
    val model: String = "claude-haiku-4-5-20251001", // claude-sonnet-4-5-20250929
    @SerialName("max_tokens")
    val maxTokens: Int = 1024,
    val messages: List<ClaudeMessage>,
    val stream: Boolean = false,
)

@Serializable
data class ClaudeMessage(
    val role: String, // "user" or "assistant"
    val content: String,
)

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
)

@Serializable
data class ClaudeUsage(
    @SerialName("input_tokens")
    val inputTokens: Int,
    @SerialName("output_tokens")
    val outputTokens: Int,
)