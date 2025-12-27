package ru.alekseev.myapplication.usecase

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.context.ContextSource
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId

/**
 * Builds message history for Claude API from multiple context sources.
 *
 * Responsibilities:
 * - Orchestrate multiple context sources (summaries, messages, RAG, memory)
 * - Assemble contexts in the correct order
 * - Add current user message
 *
 * Benefits of new architecture:
 * - Easy to add new context types (just add a ContextSource implementation)
 * - Each context source is independently testable
 * - Clear separation of concerns
 * - No longer a god class - delegates to context sources
 *
 * Adding memory: Just add MemoryContextSource to contextSources list in DI!
 */
class MessageHistoryBuilder(
    private val contextSources: List<ContextSource>
) {
    /**
     * Build complete message history for Claude API request.
     * Aggregates context from all sources and adds current user message.
     *
     * Context sources are executed in order:
     * 1. Summaries (long-term compressed history)
     * 2. Uncompressed messages (recent conversation)
     * 3. RAG context (relevant code/documents)
     * 4. Memory (to be added - long-term facts/preferences)
     *
     * @param userId The user identifier
     * @param currentMessage The current user message text
     * @param ragMode The RAG mode (disabled, enabled, or enabled with filtering)
     * @return List of Claude messages ready for API request
     */
    suspend fun buildMessageHistory(
        userId: UserId,
        currentMessage: String,
        ragMode: RagMode
    ): List<ClaudeMessage> {
        val messagesForApi = mutableListOf<ClaudeMessage>()

        // Aggregate context from all sources
        for (source in contextSources) {
            val contextMessages = source.getContext(userId, currentMessage, ragMode)
            messagesForApi.addAll(contextMessages)
        }

        // Add current user message
        messagesForApi.add(
            ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(currentMessage))
        )

        return messagesForApi
    }
}
