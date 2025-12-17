package ru.alekseev.myapplication

interface NotificationPermissionController {
    suspend fun requestPermissionIfNeeded()
}

class EmptyNotificationPermissionController :
    NotificationPermissionController {
    override suspend fun requestPermissionIfNeeded() {}
}