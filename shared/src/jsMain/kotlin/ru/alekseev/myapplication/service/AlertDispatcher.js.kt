package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.UserAlertDto

/**
 * Web (JS) implementation of AlertDispatcher
 * Uses a callback mechanism to display in-app notifications
 */
class WebAlertDispatcher(
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
        return true // Always available for web in-app notifications
    }

    override suspend fun requestPermissions(): Boolean {
        return true // No permissions needed for in-app notifications
    }
}

// Factory function for Web (JS)
actual fun createAlertDispatcher(context: Any?): AlertDispatcher {
    return WebAlertDispatcher { alert ->
        console.log("Alert received: ${alert.title} - ${alert.message}")
        // In actual implementation, this would be connected to a state flow or UI component
    }
}
