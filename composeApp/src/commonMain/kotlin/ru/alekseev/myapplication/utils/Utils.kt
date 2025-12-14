package ru.alekseev.myapplication.utils

fun formatUsd(value: Double): String {
    return "$" + (kotlin.math.round(value * 1_000_000) / 1_000_000).toString()
}