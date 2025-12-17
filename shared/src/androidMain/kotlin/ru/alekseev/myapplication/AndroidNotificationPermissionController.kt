package ru.alekseev.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AndroidNotificationPermissionController(
    private val activityProvider: ActivityProvider,
) : NotificationPermissionController {

    private var launcher: ActivityResultLauncher<String>? = null

    private fun ensureLauncher(activity: ComponentActivity) {
        if (launcher == null) {
            launcher = activity.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}
        }
    }

    override suspend fun requestPermissionIfNeeded() {
        val activity = activityProvider.get() ?: return

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        ensureLauncher(activity)

        val granted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
