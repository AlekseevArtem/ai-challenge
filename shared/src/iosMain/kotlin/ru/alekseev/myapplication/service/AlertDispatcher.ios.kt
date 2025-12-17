package ru.alekseev.myapplication.service

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UserNotifications.*
import platform.Foundation.NSUUID
import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.AlertSeverityDto
import ru.alekseev.myapplication.data.dto.UserAlertDto
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS implementation of AlertDispatcher using UNUserNotificationCenter
 */
@OptIn(ExperimentalForeignApi::class)
class IOSAlertDispatcher : AlertDispatcher {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override suspend fun dispatch(alert: UserAlertDto): AlertDispatchResult {
        return try {
            if (!isAvailable()) {
                return AlertDispatchResult.Failure("Notifications not available")
            }

            // Create notification content
            val content = UNMutableNotificationContent().apply {
                setTitle(alert.title)
                setBody(alert.message)

                // Set sound based on severity
                when (alert.severity) {
                    AlertSeverityDto.ERROR, AlertSeverityDto.WARNING -> {
                        setSound(UNNotificationSound.defaultSound())
                    }
                    else -> {
                        setSound(UNNotificationSound.defaultSound())
                    }
                }

                // Set category if provided
                alert.category?.let { setCategoryIdentifier(it) }
            }

            // Create trigger (deliver immediately)
            val trigger = null // null means deliver immediately

            // Create request
            val identifier = alert.id.ifEmpty { NSUUID().UUIDString() }
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = identifier,
                content = content,
                trigger = trigger
            )

            // Add notification request
            suspendCoroutine { continuation ->
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        continuation.resume(
                            AlertDispatchResult.Failure("Failed to schedule notification: ${error.localizedDescription}")
                        )
                    } else {
                        continuation.resume(AlertDispatchResult.Success)
                    }
                }
            }
        } catch (e: Exception) {
            AlertDispatchResult.Failure("Failed to dispatch alert: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        // Always return true, actual permission check happens in requestPermissions
        return true
    }

    override suspend fun requestPermissions(): Boolean {
        return suspendCoroutine { continuation ->
            notificationCenter.requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, error ->
                if (error != null) {
                    continuation.resume(false)
                } else {
                    continuation.resume(granted)
                }
            }
        }
    }
}

// Factory function for iOS
actual fun createAlertDispatcher(context: Any?): AlertDispatcher {
    return IOSAlertDispatcher()
}
