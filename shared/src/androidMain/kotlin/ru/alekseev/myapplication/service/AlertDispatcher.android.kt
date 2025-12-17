package ru.alekseev.myapplication.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ru.alekseev.myapplication.data.dto.AlertDispatchResult
import ru.alekseev.myapplication.data.dto.AlertSeverityDto
import ru.alekseev.myapplication.data.dto.UserAlertDto

/**
 * Android implementation of AlertDispatcher using system notifications
 */
class AndroidAlertDispatcher(private val context: Context) : AlertDispatcher {

    companion object {
        private const val CHANNEL_ID = "reminder_alerts"
        private const val CHANNEL_NAME = "Reminder Alerts"
        private const val CHANNEL_DESCRIPTION = "Notifications for reminder summaries and updates"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override suspend fun dispatch(alert: UserAlertDto): AlertDispatchResult {
        return try {
            if (!isAvailable()) {
                return AlertDispatchResult.Failure("Notifications not available")
            }

            val notificationId = alert.id.hashCode()

            // Determine icon based on severity
            val icon = when (alert.severity) {
                AlertSeverityDto.ERROR -> android.R.drawable.ic_dialog_alert
                AlertSeverityDto.WARNING -> android.R.drawable.ic_dialog_info
                AlertSeverityDto.SUCCESS -> android.R.drawable.ic_dialog_info
                AlertSeverityDto.INFO -> android.R.drawable.ic_dialog_info
            }

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(icon)
                .setContentTitle(alert.title)
                .setContentText(alert.message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
                .setPriority(when (alert.severity) {
                    AlertSeverityDto.ERROR -> NotificationCompat.PRIORITY_HIGH
                    AlertSeverityDto.WARNING -> NotificationCompat.PRIORITY_DEFAULT
                    AlertSeverityDto.SUCCESS -> NotificationCompat.PRIORITY_DEFAULT
                    AlertSeverityDto.INFO -> NotificationCompat.PRIORITY_DEFAULT
                })
                .setAutoCancel(true)

            // Add category if provided
            alert.category?.let {
                builder.setCategory(it)
            }

            // Check permission before showing notification
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return AlertDispatchResult.Failure("Notification permission not granted")
                }
            }

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }

            AlertDispatchResult.Success
        } catch (e: Exception) {
            AlertDispatchResult.Failure("Failed to dispatch alert: ${e.message}")
        }
    }

    override fun isAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    override suspend fun requestPermissions(): Boolean {
        // On Android 13+, permissions need to be requested from an Activity
        // This method just checks current status
        return isAvailable()
    }
}

// Factory function for Android
actual fun createAlertDispatcher(context: Any?): AlertDispatcher {
    val androidContext = context as? Context
        ?: throw IllegalArgumentException(
            "Android platform requires Context. Pass application context to createAlertDispatcher(context)."
        )
    return AndroidAlertDispatcher(androidContext)
}
