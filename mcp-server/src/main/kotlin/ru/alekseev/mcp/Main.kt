package ru.alekseev.mcp

import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    System.err.println("Google Calendar MCP Server starting...")

    val calendarService = GoogleCalendarService()
    val server = MCPServer(calendarService)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val reader = BufferedReader(InputStreamReader(System.`in`))

    System.err.println("Google Calendar MCP Server running on stdio")

    try {
        while (true) {
            val line = reader.readLine() ?: break

            if (line.isBlank()) continue

            System.err.println("Received request: $line")

            try {
                val request = json.decodeFromString<JSONRPCRequest>(line)
                val response = server.handleRequest(request)
                val responseJson = json.encodeToString(JSONRPCResponse.serializer(), response)

                println(responseJson)
                System.out.flush()

                System.err.println("Sent response: $responseJson")
            } catch (e: Exception) {
                System.err.println("Error processing request: ${e.message}")
                e.printStackTrace(System.err)

                val errorResponse = JSONRPCResponse(
                    error = JSONRPCError(-32700, "Parse error: ${e.message}")
                )
                val errorJson = json.encodeToString(JSONRPCResponse.serializer(), errorResponse)
                println(errorJson)
                System.out.flush()
            }
        }
    } catch (e: Exception) {
        System.err.println("Fatal error: ${e.message}")
        e.printStackTrace(System.err)
    }
}
