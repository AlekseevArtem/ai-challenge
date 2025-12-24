package ru.alekseev.myapplication.domain.model

import kotlinx.serialization.Serializable

/**
 * Server-side representation of RAG mode.
 * Corresponds to client-side RagMode in feature-settings module.
 */
@Serializable
sealed interface RagMode {
    @Serializable
    data object Disabled : RagMode

    @Serializable
    data object Enabled : RagMode

    @Serializable
    data class EnabledWithFiltering(val threshold: Float) : RagMode
}
