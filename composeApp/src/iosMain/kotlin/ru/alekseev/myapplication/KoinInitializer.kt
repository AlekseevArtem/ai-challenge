package ru.alekseev.myapplication

import com.example.myapplication.feature_main.di.featureMainModule
import com.example.myapplication.feature_settings.di.featureSettingsModule
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatformTools
import ru.alekseev.myapplication.di.platformModule

/**
 * Initialize Koin for iOS platform.
 * Uses lazy initialization to ensure it's only called once.
 */
private object KoinInitializer {
    val isInitialized: Boolean by lazy {
        startKoin {
            modules(
                platformModule,
                featureMainModule,
                featureSettingsModule
            )
        }
        true
    }
}

/**
 * Ensures Koin is initialized.
 * Safe to call multiple times - will only initialize once.
 */
fun initKoin() {
    // Check if Koin is already initialized
    try {
        KoinPlatformTools.defaultContext().get()
        // Already initialized
        return
    } catch (_: Exception) {
        // Not initialized yet, initialize it
        KoinInitializer.isInitialized
    }
}
