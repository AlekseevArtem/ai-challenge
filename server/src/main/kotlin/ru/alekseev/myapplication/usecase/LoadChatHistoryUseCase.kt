package ru.alekseev.myapplication.usecase

import kotlinx.serialization.json.Json
import ru.alekseev.myapplication.data.dto.ChatMessageDto
import ru.alekseev.myapplication.domain.model.UserId
import ru.alekseev.myapplication.mapper.toBothMessageDtos
import ru.alekseev.myapplication.repository.ChatRepository

/**
 * Use case for loading chat history for a specific user.
 * Converts domain messages to DTOs that can be sent to the client.
 *
 * Now works with domain entities instead of database entities.
 */
class LoadChatHistoryUseCase(
    private val chatRepository: ChatRepository,
    private val json: Json
) {
    /**
     * Loads all messages for the given user and converts them to ChatMessageDto pairs.
     * Each domain message produces both a user message and an assistant message.
     *
     * @param userId The user identifier (domain value object)
     * @return List of ChatMessageDto objects representing the conversation history
     */
    suspend operator fun invoke(userId: UserId): List<ChatMessageDto> {
        val messages = chatRepository.getAllMessages(userId)

        return messages.flatMap { message ->
            val (userMessage, assistantMessage) = message.toBothMessageDtos(json)
            listOf(userMessage, assistantMessage)
        }
    }
}
