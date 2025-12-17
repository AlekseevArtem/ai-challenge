package ru.alekseev.myapplication.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import ru.alekseev.myapplication.ActivityProvider
import ru.alekseev.myapplication.AndroidNotificationPermissionController
import ru.alekseev.myapplication.NotificationPermissionController
import ru.alekseev.myapplication.service.AlertDispatcher
import ru.alekseev.myapplication.service.createAlertDispatcher

actual val platformModule = module {
    single<AlertDispatcher> {
        createAlertDispatcher(androidContext())

    }
    single { ActivityProvider() }
    single<NotificationPermissionController> {
        AndroidNotificationPermissionController(
            activityProvider = get()
        )
    }
}
