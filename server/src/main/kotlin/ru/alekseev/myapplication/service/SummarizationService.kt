package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.dto.ClaudeMessage
import ru.alekseev.myapplication.dto.ClaudeRequest

class SummarizationService(
    private val claudeApiService: ClaudeApiService,
) {

    suspend fun createSummary(messages: List<Pair<String, String>>): String {

        println("SummarizationService: createSummary $messages")

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
                    content = summaryPrompt
                )
            )
        )

        val response = claudeApiService.sendMessage(request)

        println("SummarizationService: createSummary ${response.content}")
        return response.content?.firstOrNull { it.type == "text" }?.text
            ?: "Summary could not be generated"
    }
}
