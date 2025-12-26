package ru.alekseev.myapplication.domain.entity

/**
 * Application-wide settings.
 * This is a shared domain entity accessible to all feature modules.
 */
data class AppSettings(
    val ragMode: RagMode = RagMode.Disabled
)
