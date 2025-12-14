package ru.alekseev.myapplication

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform