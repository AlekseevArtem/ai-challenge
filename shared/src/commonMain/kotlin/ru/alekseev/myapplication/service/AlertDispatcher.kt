package ru.alekseev.myapplication.service

import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.UserAlertDto

/**
 * Platform-agnostic interface for dispatching user alerts.
 * Implementations should handle platform-specific notification mechanisms:
 * - Android: System notifications or local notifications
 * - iOS: Local notifications or UNUserNotificationCenter
 * - Web/Desktop: In-app notifications (toast, snackbar, etc.)
 */
interface AlertDispatcher {
    /**
     * Dispatch an alert to the user
     * @param alert The alert to display
     * @return Result of the dispatch operation
     */
    suspend fun dispatch(alert: UserAlertDto): AlertDispatchResult

    /**
     * Check if alert dispatching is available on this platform
     * @return true if alerts can be dispatched
     */
    fun isAvailable(): Boolean

    /**
     * Request necessary permissions for alert dispatching (e.g., notification permissions)
     * @return true if permissions are granted or not required
     */
    suspend fun requestPermissions(): Boolean
}

/**
 * Factory function to create platform-specific AlertDispatcher
 * Implementation should be provided in each platform's source set
 * @param context Platform-specific context (e.g., Android Context). Can be null for platforms that don't need it.
 */
expect fun createAlertDispatcher(context: Any? = null): AlertDispatcher
