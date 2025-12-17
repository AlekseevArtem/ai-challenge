package ru.alekseev.myapplication.data.datasource

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.pingInterval
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlin.time.DurationUnit
import kotlin.time.toDuration

actual fun createHttpClient(json: Json): HttpClient =
    HttpClient(OkHttp) {
        install(WebSockets) {
            pingInterval = 20_000.toDuration(DurationUnit.MILLISECONDS)
        }
        install(ContentNegotiation) {
            json(json)
        }
    }