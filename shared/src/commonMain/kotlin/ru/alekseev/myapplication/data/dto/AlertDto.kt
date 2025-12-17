@file:OptIn(ExperimentalTime::class)

package ru.alekseev.myapplication.data.dto

import kotlinx.serialization.Serializable
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Data transfer object for user alerts/notifications
 * Platform-agnostic representation of an alert message
 */
@Serializable
data class UserAlertDto(
    val id: String,
    val title: String,
    val message: String,
    val severity: AlertSeverityDto = AlertSeverityDto.INFO,
    val timestamp: Long = Clock.System.now().toEpochMilliseconds(),
    val category: String? = null,
    val actionLabel: String? = null,
    val actionData: String? = null
)

@Serializable
enum class AlertSeverityDto {
    INFO,
    WARNING,
    ERROR,
    SUCCESS
}

/**
 * Result of dispatching an alert
 */
sealed class AlertDispatchResult {
    data object Success : AlertDispatchResult()
    data class Failure(val reason: String) : AlertDispatchResult()
}
