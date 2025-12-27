package ru.alekseev.myapplication.domain.context

import ru.alekseev.myapplication.data.dto.ClaudeMessage
import ru.alekseev.myapplication.data.dto.ClaudeMessageContent
import ru.alekseev.myapplication.domain.entity.RagMode
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.repository.ChatRepository

/**
 * Provides recent uncompressed conversation messages as context.
 *
 * These are the most recent messages that haven't been summarized yet.
 * They maintain full conversation detail for recent interactions.
 *
 * Format: Alternating user/assistant messages in chronological order
 */
class UncompressedMessagesContextSource(
    private val chatRepository: ChatRepository
) : ContextSource {
    override suspend fun getContext(
        userId: UserId,
        currentMessage: String,
        ragMode: RagMode
    ): List<ClaudeMessage> {
        val uncompressedMessages = chatRepository.getUncompressedMessages(userId)

        if (uncompressedMessages.isEmpty()) {
            return emptyList()
        }

        val messages = mutableListOf<ClaudeMessage>()

        uncompressedMessages.forEach { msg ->
            messages.add(
                ClaudeMessage(role = "user", content = ClaudeMessageContent.Text(msg.userMessage))
            )
            messages.add(
                ClaudeMessage(role = "assistant", content = ClaudeMessageContent.Text(msg.assistantMessage))
            )
        }

        return messages
    }
}
