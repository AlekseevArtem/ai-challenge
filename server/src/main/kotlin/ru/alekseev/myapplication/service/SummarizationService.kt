package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway

class SummarizationService(
    private val claudeGateway: ClaudeGateway,
) {

    suspend fun createSummary(messages: List<Pair<String, String>>): String {

        val conversationText = messages.mapIndexed { index, (userMsg, assistantMsg) ->
            """
            Message ${index + 1}:
            User: $userMsg
            Assistant: $assistantMsg
            """.trimIndent()
        }.joinToString("\n\n")

        val summaryPrompt = """
            Please provide a concise summary of the following conversation.
            Focus on key facts, decisions, and context that would be important for continuing the conversation.
            Keep the summary brief but informative.

            Conversation:
            $conversationText

            Summary:
        """.trimIndent()

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = "user",
                    content = ClaudeMessageContent.Text(summaryPrompt)
                )
            )
        )

        val response = claudeGateway.sendMessage(request)

        return response.content?.firstOrNull { it.type == "text" }?.text
            ?: "Summary could not be generated"
    }
}
