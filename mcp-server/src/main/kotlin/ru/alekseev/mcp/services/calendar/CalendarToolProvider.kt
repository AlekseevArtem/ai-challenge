package ru.alekseev.mcp.services.calendar

import CreateEventParams
import DeleteEventParams
import ListEventsParams
import UpdateEventParams
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put
import ru.alekseev.mcp.MCPToolProvider
import ru.alekseev.mcp.models.Tool
import ru.alekseev.myapplication.core.common.JsonFactory
import ru.alekseev.myapplication.core.common.logTag

/**
 * Provides Google Calendar tools for MCP server
 */
class CalendarToolProvider(
    private val calendarService: GoogleCalendarService
) : MCPToolProvider {

    private val json = JsonFactory.create()

    override fun getTools(): List<Tool> = listOf(
        Tool(
            name = "list_events",
            description = "List upcoming events from Google Calendar. You can specify the number of events to retrieve (default: 10) and a time range.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("maxResults", buildJsonObject {
                        put("type", "number")
                        put("description", "Maximum number of events to return (default: 10)")
                        put("default", 10)
                    })
                    put("timeMin", buildJsonObject {
                        put("type", "string")
                        put("description", "Start time for events (ISO 8601 format). Defaults to now.")
                    })
                    put("timeMax", buildJsonObject {
                        put("type", "string")
                        put("description", "End time for events (ISO 8601 format). Optional.")
                    })
                    put("calendarId", buildJsonObject {
                        put("type", "string")
                        put("description", "Calendar ID to query (default: 'primary')")
                        put("default", "primary")
                    })
                })
            }
        ),
        Tool(
            name = "create_event",
            description = "Create a new event in Google Calendar with specified details.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("summary", buildJsonObject {
                        put("type", "string")
                        put("description", "Event title/summary")
                    })
                    put("description", buildJsonObject {
                        put("type", "string")
                        put("description", "Event description (optional)")
                    })
                    put("startDateTime", buildJsonObject {
                        put("type", "string")
                        put("description", "Start date-time in ISO 8601 format")
                    })
                    put("endDateTime", buildJsonObject {
                        put("type", "string")
                        put("description", "End date-time in ISO 8601 format")
                    })
                    put("timeZone", buildJsonObject {
                        put("type", "string")
                        put("description", "Time zone (default: 'UTC')")
                        put("default", "UTC")
                    })
                    put("attendees", buildJsonObject {
                        put("type", "array")
                        put("description", "List of attendee email addresses (optional)")
                        put("items", buildJsonObject {
                            put("type", "string")
                        })
                    })
                    put("calendarId", buildJsonObject {
                        put("type", "string")
                        put("description", "Calendar ID (default: 'primary')")
                        put("default", "primary")
                    })
                })
                put("required", buildJsonArray {
                    add("summary")
                    add("startDateTime")
                    add("endDateTime")
                })
            }
        ),
        Tool(
            name = "update_event",
            description = "Update an existing event in Google Calendar.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("eventId", buildJsonObject {
                        put("type", "string")
                        put("description", "ID of the event to update")
                    })
                    put("summary", buildJsonObject {
                        put("type", "string")
                        put("description", "New event title/summary (optional)")
                    })
                    put("description", buildJsonObject {
                        put("type", "string")
                        put("description", "New event description (optional)")
                    })
                    put("startDateTime", buildJsonObject {
                        put("type", "string")
                        put("description", "New start date-time in ISO 8601 format (optional)")
                    })
                    put("endDateTime", buildJsonObject {
                        put("type", "string")
                        put("description", "New end date-time in ISO 8601 format (optional)")
                    })
                    put("timeZone", buildJsonObject {
                        put("type", "string")
                        put("description", "Time zone (optional)")
                    })
                    put("calendarId", buildJsonObject {
                        put("type", "string")
                        put("description", "Calendar ID (default: 'primary')")
                        put("default", "primary")
                    })
                })
                put("required", buildJsonArray {
                    add("eventId")
                })
            }
        ),
        Tool(
            name = "delete_event",
            description = "Delete an event from Google Calendar.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("eventId", buildJsonObject {
                        put("type", "string")
                        put("description", "ID of the event to delete")
                    })
                    put("calendarId", buildJsonObject {
                        put("type", "string")
                        put("description", "Calendar ID (default: 'primary')")
                        put("default", "primary")
                    })
                })
                put("required", buildJsonArray {
                    add("eventId")
                })
            }
        )
    )

    override fun handleToolCall(toolName: String, arguments: JsonObject?): String {
        System.err.println("$logTag Calling tool: $toolName")
        System.err.println("$logTag Tool arguments: $arguments")

        return when (toolName) {
            "list_events" -> {
                val args = parseParams<ListEventsParams>(arguments) ?: ListEventsParams()
                System.err.println("$logTag list_events: maxResults=${args.maxResults}, calendarId=${args.calendarId}")
                calendarService.listEvents(args).also {
                    System.err.println("$logTag list_events completed successfully")
                }
            }

            "create_event" -> {
                val args = parseParams<CreateEventParams>(arguments)
                    ?: throw IllegalArgumentException("Missing required arguments")
                System.err.println("$logTag create_event: summary=${args.summary}, start=${args.startDateTime}, end=${args.endDateTime}")
                calendarService.createEvent(args).also {
                    System.err.println("$logTag create_event completed successfully")
                }
            }

            "update_event" -> {
                val args = parseParams<UpdateEventParams>(arguments)
                    ?: throw IllegalArgumentException("Missing required arguments")
                System.err.println("$logTag update_event: eventId=${args.eventId}")
                calendarService.updateEvent(args).also {
                    System.err.println("$logTag update_event completed successfully")
                }
            }

            "delete_event" -> {
                val args = parseParams<DeleteEventParams>(arguments)
                    ?: throw IllegalArgumentException("Missing required arguments")
                System.err.println("$logTag delete_event: eventId=${args.eventId}, calendarId=${args.calendarId}")
                calendarService.deleteEvent(args).also {
                    System.err.println("$logTag delete_event completed successfully")
                }
            }

            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }
    }

    private inline fun <reified T> parseParams(args: JsonObject?): T? {
        return args?.let { json.decodeFromJsonElement<T>(it) }
    }
}