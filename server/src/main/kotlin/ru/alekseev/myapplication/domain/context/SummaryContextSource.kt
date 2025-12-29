package ru.alekseev.myapplication.domain.context

import ru.alekseev.myapplication.core.common.ClaudeRoles
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.repository.ChatRepository

/**
 * Provides conversation summaries as context.
 *
 * Summaries represent compressed historical conversations that help maintain
 * long-term context without excessive token usage.
 *
 * Format: User message with summary text + Assistant acknowledgment
 */
class SummaryContextSource(
    private val chatRepository: ChatRepository
) : ContextSource {
    override suspend fun getContext(
        userId: UserId,
        currentMessage: String,
        ragMode: RagMode
    ): List<ClaudeMessage> {
        val summaries = chatRepository.getAllSummaries(userId)

        if (summaries.isEmpty()) {
            return emptyList()
        }

        val messages = mutableListOf<ClaudeMessage>()

        val summaryContext = summaries.joinToString("\n\n") {
            "Previous conversation summary: ${it.summaryText}"
        }

        messages.add(
            ClaudeMessage(
                role = ClaudeRoles.USER,
                content = ClaudeMessageContent.Text(summaryContext)
            )
        )
        messages.add(
            ClaudeMessage(
                role = ClaudeRoles.ASSISTANT,
                content = ClaudeMessageContent.Text("I understand the context from previous conversations.")
            )
        )

        return messages
    }
}
