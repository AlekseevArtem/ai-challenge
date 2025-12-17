package ru.alekseev.myapplication

import android.app.Application
import com.example.myapplication.feature_main.di.featureMainModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import ru.alekseev.myapplication.di.platformModule

class MyApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidLogger()
            androidContext(this@MyApplication)
            modules(
                platformModule,
                featureMainModule
            )
        }
    }
}
