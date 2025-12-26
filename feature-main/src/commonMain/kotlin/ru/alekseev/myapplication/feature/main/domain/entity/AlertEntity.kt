package ru.alekseev.myapplication.feature.main.domain.entity

/**
 * Domain entity representing a user alert/notification
 */
data class AlertEntity(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long,
    val category: String? = null,
    val actionLabel: String? = null,
    val actionData: String? = null
)

enum class AlertSeverity {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}
