package ru.alekseev.myapplication.di

import org.koin.dsl.module
import ru.alekseev.myapplication.EmptyNotificationPermissionController
import ru.alekseev.myapplication.NotificationPermissionController
import ru.alekseev.myapplication.service.AlertDispatcher
import ru.alekseev.myapplication.service.createAlertDispatcher

actual val platformModule = module {
    single<AlertDispatcher> {
        createAlertDispatcher()
    }

    single<NotificationPermissionController> {
        EmptyNotificationPermissionController()
    }
}
