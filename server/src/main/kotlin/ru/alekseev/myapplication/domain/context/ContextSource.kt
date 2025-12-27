package ru.alekseev.myapplication.domain.context

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId

/**
 * Abstraction for sources of context in conversation history.
 *
 * Each ContextSource is responsible for providing a specific type of context:
 * - Conversation summaries (previous compressed conversations)
 * - Uncompressed messages (recent conversation history)
 * - RAG context (relevant code/documents)
 * - Memory (long-term facts, preferences) - to be added
 *
 * Benefits of this abstraction:
 * - Easy to add new context types (e.g., memory, web search)
 * - Testable in isolation
 * - Composable via strategy pattern
 * - Clear separation of concerns
 */
interface ContextSource {
    /**
     * Get context messages for the given user and current message.
     *
     * @param userId The user making the request
     * @param currentMessage The current user message
     * @param ragMode The RAG mode (only relevant for RAGContextSource)
     * @return List of Claude messages representing this context, empty if no context
     */
    suspend fun getContext(
        userId: UserId,
        currentMessage: String,
        ragMode: RagMode = RagMode.Disabled
    ): List<ClaudeMessage>
}
