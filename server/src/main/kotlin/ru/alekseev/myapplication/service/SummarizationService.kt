package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.core.common.ClaudeRoles
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.data.dto.ClaudeRequest
import ru.alekseev.myapplication.domain.gateway.ClaudeGateway

class SummarizationService(
    private val claudeGateway: ClaudeGateway,
) {

    suspend fun createSummary(
        messages: List<Pair<String, String>>,
        previousSummary: String? = null
    ): String {

        val conversationText = messages.mapIndexed { index, (userMsg, assistantMsg) ->
            """
            Message ${index + 1}:
            User: $userMsg
            Assistant: $assistantMsg
            """.trimIndent()
        }.joinToString("\n\n")

        val summaryPrompt = buildString {
            appendLine("You are summarizing a conversation to maintain context efficiently.")
            appendLine()

            if (previousSummary != null) {
                appendLine("PREVIOUS SUMMARY:")
                appendLine(previousSummary)
                appendLine()
                appendLine("NEW MESSAGES SINCE LAST SUMMARY:")
            } else {
                appendLine("CONVERSATION MESSAGES:")
            }

            appendLine(conversationText)
            appendLine()
            appendLine("Please create a SINGLE cumulative summary that:")
            if (previousSummary != null) {
                appendLine("- Integrates information from the PREVIOUS SUMMARY")
                appendLine("- Incorporates NEW MESSAGES")
                appendLine("- Updates or replaces outdated information from previous summary")
            } else {
                appendLine("- Captures all important information from the conversation")
            }
            appendLine("- Uses this EXACT structure:")
            appendLine()
            appendLine("PROJECT CONTEXT:")
            appendLine("- [Key facts about the project, codebase, technologies used]")
            appendLine()
            appendLine("CURRENT GOALS:")
            appendLine("- [What the user is trying to achieve]")
            appendLine()
            appendLine("DECISIONS MADE:")
            appendLine("- [Important technical decisions, architectural choices]")
            appendLine()
            appendLine("IMPORTANT CONSTRAINTS:")
            appendLine("- [Limitations, requirements, things to avoid]")
            appendLine()
            appendLine("OPEN QUESTIONS / TODO:")
            appendLine("- [Unresolved issues, pending tasks]")
            appendLine()
            appendLine("IMPORTANT: This summary will REPLACE all previous summaries. Include ALL relevant context.")
        }.trimIndent()

        val request = ClaudeRequest(
            messages = listOf(
                ClaudeMessage(
                    role = ClaudeRoles.USER,
                    content = ClaudeMessageContent.Text(summaryPrompt)
                )
            )
        )

        val response = claudeGateway.sendMessage(request)

        return response.content?.firstOrNull { it.type == "text" }?.text
            ?: "Summary could not be generated"
    }
}
