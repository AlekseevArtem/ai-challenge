package ru.alekseev.myapplication.core.common

// Порт сервера (внутренний)
const val SERVER_PORT = 8080

// Внешний порт для подключения клиента (Docker использует 8081)
const val SERVER_EXTERNAL_PORT = 8081

// WebSocket URL для подключения клиента
const val SERVER_WS_URL = "ws://localhost:$SERVER_EXTERNAL_PORT/chat"
