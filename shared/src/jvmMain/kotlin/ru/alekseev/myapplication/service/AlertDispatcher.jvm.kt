package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.UserAlertDto

/**
 * JVM (Desktop) implementation of AlertDispatcher
 * Uses a callback mechanism to display in-app notifications
 */
class DesktopAlertDispatcher(
    private val onAlertCallback: (UserAlertDto) -> Unit
) : AlertDispatcher {

    override suspend fun dispatch(alert: UserAlertDto): AlertDispatchResult {
        return try {
            onAlertCallback(alert)
            AlertDispatchResult.Success
        } catch (e: Exception) {
            AlertDispatchResult.Failure("Failed to dispatch alert: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        return true // Always available for desktop in-app notifications
    }

    override suspend fun requestPermissions(): Boolean {
        return true // No permissions needed for in-app notifications
    }
}

// Factory function for Desktop (JVM)
actual fun createAlertDispatcher(context: Any?): AlertDispatcher {
    return DesktopAlertDispatcher { alert ->
        println("Alert received: ${alert.title} - ${alert.message}")
        // In actual implementation, this would be connected to a state flow or UI component
    }
}
