package ru.alekseev.myapplication.domain.model

/**
 * Domain model representing a complete conversation context.
 * Contains both summaries (compressed old messages) and recent uncompressed messages.
 */
data class Conversation(
    val summaries: List<Summary>,
    val messages: List<Message>,
    val userId: UserId
) {
    /**
     * Check if the conversation is empty.
     */
    fun isEmpty(): Boolean = summaries.isEmpty() && messages.isEmpty()

    /**
     * Get total number of items (summaries + messages).
     */
    fun size(): Int = summaries.size + messages.size
}
