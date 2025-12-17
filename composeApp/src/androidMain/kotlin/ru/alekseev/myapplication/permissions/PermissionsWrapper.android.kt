package ru.alekseev.myapplication.permissions

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.PermissionsController
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.PermissionsControllerFactory
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.notifications.REMOTE_NOTIFICATION

class MokoPermissionsWrapper(
    private val controller: PermissionsController
) : PermissionsWrapper {

    @Composable
    override fun BindEffect() {
        BindEffect(controller)
    }

    override suspend fun requestNotificationPermission() {
        try {
            controller.providePermission(Permission.REMOTE_NOTIFICATION)
        } catch (e: DeniedException) {
            throw PermissionDeniedException("Permission denied: ${e.message}")
        } catch (e: DeniedAlwaysException) {
            throw PermissionDeniedException("Permission denied always: ${e.message}")
        }
    }
}

@Composable
actual fun rememberPermissionsWrapper(): PermissionsWrapper {
    val factory: PermissionsControllerFactory = rememberPermissionsControllerFactory()
    val controller: PermissionsController = remember(factory) {
        factory.createPermissionsController()
    }
    return remember(controller) {
        MokoPermissionsWrapper(controller)
    }
}