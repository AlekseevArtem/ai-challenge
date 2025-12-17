package ru.alekseev.myapplication

import androidx.activity.ComponentActivity

class ActivityProvider {
    private var activity: ComponentActivity? = null

    fun set(activity: ComponentActivity) {
        this.activity = activity
    }

    fun clear(activity: ComponentActivity) {
        if (this.activity === activity) {
            this.activity = null
        }
    }

    fun get(): ComponentActivity? = activity
}