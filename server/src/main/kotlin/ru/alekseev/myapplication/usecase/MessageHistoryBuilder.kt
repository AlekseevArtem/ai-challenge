package ru.alekseev.myapplication.usecase

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.gateway.DocumentRetriever
import ru.alekseev.myapplication.domain.model.Message
import ru.alekseev.myapplication.domain.model.Summary
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.domain.rag.SimilarityThresholdFilter
import ru.alekseev.myapplication.repository.ChatRepository

/**
 * Builds message history for Claude API from conversation context.
 * Handles:
 * - Adding conversation summaries as context
 * - Including uncompressed messages
 * - Injecting RAG context from relevant documents
 * - Formatting messages for Claude API
 *
 * Single Responsibility: Transform domain conversation state into Claude API format.
 */
class MessageHistoryBuilder(
    private val chatRepository: ChatRepository,
    private val documentRetriever: DocumentRetriever
) {
    /**
     * Build complete message history for Claude API request.
     * Includes summaries, uncompressed messages, RAG context, and current user message.
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
        val summaries = chatRepository.getAllSummaries(userId)
        val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

        val messagesForApi = mutableListOf<ClaudeMessage>()

        // Add summaries as system context
        addSummariesContext(messagesForApi, summaries)

        // Add uncompressed messages
        addUncompressedMessages(messagesForApi, uncompressedMessages)

        // Add RAG context based on mode
        addRagContext(messagesForApi, currentMessage, ragMode)

        // Add current user message
        messagesForApi.add(
            ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(currentMessage))
        )

        return messagesForApi
    }

    /**
     * Add conversation summaries as context at the beginning.
     */
    private fun addSummariesContext(
        messages: MutableList<ClaudeMessage>,
        summaries: List<Summary>
    ) {
        if (summaries.isNotEmpty()) {
            val summaryContext = summaries.joinToString("\n\n") {
                "Previous conversation summary: ${it.summaryText}"
            }
            messages.add(
                ClaudeMessage(
                    role = "user",
                    content = ClaudeMessageContent.Text(summaryContext)
                )
            )
            messages.add(
                ClaudeMessage(
                    role = "assistant",
                    content = ClaudeMessageContent.Text("I understand the context from previous conversations.")
                )
            )
        }
    }

    /**
     * Add uncompressed messages to maintain recent conversation history.
     */
    private fun addUncompressedMessages(
        messages: MutableList<ClaudeMessage>,
        uncompressedMessages: List<Message>
    ) {
        uncompressedMessages.forEach { msg ->
            messages.add(
                ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(msg.userMessage))
            )
            messages.add(
                ClaudeMessage(role = "assistant", content = ClaudeMessageContent.Text(msg.assistantMessage))
            )
        }
    }

    /**
     * Add RAG context from document search based on the configured mode.
     */
    private suspend fun addRagContext(
        messages: MutableList<ClaudeMessage>,
        currentMessage: String,
        ragMode: RagMode
    ) {
        when (ragMode) {
            is RagMode.Disabled -> {
                // No RAG context
            }
            is RagMode.Enabled -> {
                // RAG without filtering
                if (documentRetriever.isReady()) {
                    println("[MessageHistoryBuilder] RAG enabled without filtering")
                    val ragContext = documentRetriever.getContextForQuery(currentMessage, topK = 3, filter = null)
                    if (ragContext.isNotBlank()) {
                        messages.add(
                            ClaudeMessage(
                                role = "user",
                                content = ClaudeMessageContent.Text(ragContext)
                            )
                        )
                        messages.add(
                            ClaudeMessage(
                                role = "assistant",
                                content = ClaudeMessageContent.Text("I understand. I'll use this context from the project codebase to answer your question.")
                            )
                        )
                    }
                }
            }
            is RagMode.EnabledWithFiltering -> {
                // RAG with similarity threshold filtering
                if (documentRetriever.isReady()) {
                    println("[MessageHistoryBuilder] RAG enabled with filtering (threshold: ${ragMode.threshold})")
                    val filter = SimilarityThresholdFilter(ragMode.threshold)
                    val ragContext = documentRetriever.getContextForQuery(currentMessage, topK = 3, filter = filter)
                    if (ragContext.isNotBlank()) {
                        messages.add(
                            ClaudeMessage(
                                role = "user",
                                content = ClaudeMessageContent.Text(ragContext)
                            )
                        )
                        messages.add(
                            ClaudeMessage(
                                role = "assistant",
                                content = ClaudeMessageContent.Text("I understand. I'll use this filtered context from the project codebase to answer your question.")
                            )
                        )
                    } else {
                        println("[MessageHistoryBuilder] No chunks passed the similarity threshold, proceeding without RAG context")
                    }
                }
            }
        }
    }
}
