package ru.alekseev.myapplication.domain.context

import ru.alekseev.myapplication.core.common.ClaudeRoles
import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.repository.ChatRepository

/**
 * Provides conversation summary as context.
 *
 * This source provides a SINGLE cumulative summary that represents the entire
 * conversation history up to the most recent summarization point.
 *
 * The summary is structured with sections:
 * - PROJECT CONTEXT: Key facts about the project
 * - CURRENT GOALS: What the user is trying to achieve
 * - DECISIONS MADE: Important technical decisions
 * - IMPORTANT CONSTRAINTS: Limitations and requirements
 * - OPEN QUESTIONS / TODO: Unresolved issues and pending tasks
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

        // Since we maintain only ONE cumulative summary, get the latest (and only) one
        val latestSummary = summaries.lastOrNull()

        if (latestSummary == null) {
            return emptyList()
        }

        val messages = mutableListOf<ClaudeMessage>()

        messages.add(
            ClaudeMessage(
                role = ClaudeRoles.USER,
                content = ClaudeMessageContent.Text("Here is the summary of our previous conversation:\n\n${latestSummary.summaryText}")
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
