package ru.alekseev.myapplication.core.common

import kotlinx.serialization.json.Json

/**
 * Factory for creating configured Json instances with consistent settings across the application.
 * This ensures all JSON serialization/deserialization uses the same configuration.
 */
object JsonFactory {
    /**
     * Creates a Json instance with standard application-wide configuration:
     * - classDiscriminator = "type" for polymorphic serialization
     * - ignoreUnknownKeys = true for backward compatibility
     * - prettyPrint = true for readable output
     * - isLenient = true for relaxed parsing
     * - encodeDefaults = true to include default values
     */
    fun create(): Json = Json {
        classDiscriminator = "type"
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
        encodeDefaults = true
    }
}
