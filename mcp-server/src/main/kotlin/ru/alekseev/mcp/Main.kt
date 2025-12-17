package ru.alekseev.mcp

import kotlinx.serialization.json.Json
import ru.alekseev.mcp.models.JSONRPCError
import ru.alekseev.mcp.models.JSONRPCRequest
import ru.alekseev.mcp.models.JSONRPCResponse
import ru.alekseev.mcp.services.calendar.CalendarToolProvider
import ru.alekseev.mcp.services.calendar.GoogleCalendarService
import ru.alekseev.mcp.services.reminder.ReminderService
import ru.alekseev.mcp.services.reminder.ReminderToolProvider
import java.io.BufferedReader
import java.io.InputStreamReader

fun main() {
    System.err.println("MCP Server starting (Google Calendar + Reminders)...")

    // Create services
    val calendarService = GoogleCalendarService()
    val reminderService = ReminderService()

    // Create tool providers
    val toolProviders = listOf(
        CalendarToolProvider(calendarService),
        ReminderToolProvider(reminderService)
    )

    // Create MCP server with all providers
    val server = MCPServer(toolProviders)
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val reader = BufferedReader(InputStreamReader(System.`in`))

    System.err.println("MCP Server running on stdio with Calendar and Reminder tools")

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
