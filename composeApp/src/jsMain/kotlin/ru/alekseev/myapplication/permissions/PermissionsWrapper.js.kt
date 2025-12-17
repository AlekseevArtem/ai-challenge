package ru.alekseev.myapplication.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Empty permissions wrapper for JS platform
 * Notification permissions are not supported on this platform
 */
class EmptyPermissionsWrapper : PermissionsWrapper {

    @Composable
    override fun BindEffect() {
        // No-op for JS
    }

    override suspend fun requestNotificationPermission() {
        // No-op for JS - permissions not supported
    }
}

@Composable
actual fun rememberPermissionsWrapper(): PermissionsWrapper {
    return remember {
        EmptyPermissionsWrapper()
    }
}
