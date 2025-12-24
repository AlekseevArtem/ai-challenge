package ru.alekseev.myapplication.domain.entity

import kotlinx.serialization.Serializable

@Serializable
sealed interface RagMode {
    @Serializable
    data object Disabled : RagMode

    @Serializable
    data object Enabled : RagMode

    @Serializable
    data class EnabledWithFiltering(val threshold: Float) : RagMode
}
