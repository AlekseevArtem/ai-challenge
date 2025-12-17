package ru.alekseev.myapplication.data.datasource

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json

expect fun createHttpClient(json: Json): HttpClient
