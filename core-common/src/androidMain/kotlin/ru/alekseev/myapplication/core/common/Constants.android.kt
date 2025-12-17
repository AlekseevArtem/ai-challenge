package ru.alekseev.myapplication.core.common

/**
 * Server host configuration for Android
 *
 * Choose one based on your setup:
 * - Android Emulator: "10.0.2.2" (special IP to access host machine)
 * - Physical Device / Network: "192.168.0.14" (host machine IP in local network)
 *
 * To find your host machine IP:
 * - macOS/Linux: ifconfig | grep "inet " | grep -v 127.0.0.1
 * - Windows: ipconfig
 */
actual val SERVER_HOST: String = "192.168.0.18" // Physical device
// actual val SERVER_HOST: String = "10.0.2.2" // Emulator