package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.UserAlertDto

/**
 * Web (WasmJS) implementation of AlertDispatcher
 * Uses a callback mechanism to display in-app notifications
 */
class WasmWebAlertDispatcher(
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

// Factory function for Web (WasmJS)
actual fun createAlertDispatcher(context: Any?): AlertDispatcher {
    return WasmWebAlertDispatcher { alert ->
        // TODO: Connect to state flow or UI component to display alerts
        // Alert received: ${alert.title} - ${alert.message}
    }
}
