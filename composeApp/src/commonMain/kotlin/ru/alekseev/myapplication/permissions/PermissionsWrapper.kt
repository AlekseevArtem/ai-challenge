package ru.alekseev.myapplication.permissions

import androidx.compose.runtime.Composable

/**
 * Wrapper for permissions functionality that works across all platforms
 */
interface PermissionsWrapper {
    /**
     * Binds the permissions controller lifecycle to the current composition
     */
    @Composable
    fun BindEffect()

    /**
     * Requests notification permission
     * @throws PermissionDeniedException if permission is denied
     */
    suspend fun requestNotificationPermission()
}

/**
 * Exception thrown when permission is denied
 */
class PermissionDeniedException(message: String? = null) : Exception(message)

/**
 * Factory function to create platform-specific permissions wrapper
 */
@Composable
expect fun rememberPermissionsWrapper(): PermissionsWrapper