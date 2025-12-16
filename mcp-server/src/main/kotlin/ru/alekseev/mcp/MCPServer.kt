package ru.alekseev.mcp

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

class MCPServer(private val calendarService: GoogleCalendarService) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun handleRequest(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("[MCPServer] Handling request: method=${request.method}, id=${request.id}")
        return try {
            val response = when (request.method) {
                "initialize" -> handleInitialize(request)
                "tools/list" -> handleListTools(request)
                "tools/call" -> handleCallTool(request)
                else -> {
                    System.err.println("[MCPServer] ERROR: Method not found: ${request.method}")
                    JSONRPCResponse(
                        id = request.id,
                        error = JSONRPCError(-32601, "Method not found: ${request.method}")
                    )
                }
            }
            System.err.println("[MCPServer] Successfully handled request: method=${request.method}")
            response
        } catch (e: Exception) {
            System.err.println("[MCPServer] ERROR: Internal error handling request: ${e.message}")
            e.printStackTrace(System.err)
            JSONRPCResponse(
                id = request.id,
                error = JSONRPCError(-32603, "Internal error: ${e.message}")
            )
        }
    }

    private fun handleInitialize(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("[MCPServer] Initializing server...")
        val result = InitializeResult(
            serverInfo = ServerInfo(
                name = "google-calendar-mcp",
                version = "1.0.0"
            ),
            capabilities = ServerCapabilities(
                tools = buildJsonObject { }
            )
        )
        System.err.println("[MCPServer] Server initialized: name=google-calendar-mcp, version=1.0.0")

        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleListTools(request: JSONRPCRequest): JSONRPCResponse {
        System.err.println("[MCPServer] Listing available tools...")
        val tools = listOf(
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
        System.err.println("[MCPServer] Found ${tools.size} tools: ${tools.map { it.name }}")

        val result = ListToolsResult(tools)
        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun handleCallTool(request: JSONRPCRequest): JSONRPCResponse {
        val params = request.params?.let {
            json.decodeFromJsonElement<CallToolParams>(it)
        } ?: run {
            System.err.println("[MCPServer] ERROR: Invalid params in tool call")
            return JSONRPCResponse(
                id = request.id,
                error = JSONRPCError(-32602, "Invalid params")
            )
        }

        System.err.println("[MCPServer] Calling tool: ${params.name}")
        System.err.println("[MCPServer] Tool arguments: ${params.arguments}")

        val resultText = try {
            when (params.name) {
                "list_events" -> {
                    val args = params.arguments?.let { parseListEventsParams(it) }
                        ?: GoogleCalendarService.ListEventsParams()
                    System.err.println("[MCPServer] list_events: maxResults=${args.maxResults}, calendarId=${args.calendarId}")
                    val result = calendarService.listEvents(args)
                    System.err.println("[MCPServer] list_events completed successfully")
                    result
                }

                "create_event" -> {
                    val args = params.arguments?.let { parseCreateEventParams(it) }
                        ?: throw IllegalArgumentException("Missing required arguments")
                    System.err.println("[MCPServer] create_event: summary=${args.summary}, start=${args.startDateTime}, end=${args.endDateTime}")
                    val result = calendarService.createEvent(args)
                    System.err.println("[MCPServer] create_event completed successfully")
                    result
                }

                "update_event" -> {
                    val args = params.arguments?.let { parseUpdateEventParams(it) }
                        ?: throw IllegalArgumentException("Missing required arguments")
                    System.err.println("[MCPServer] update_event: eventId=${args.eventId}")
                    val result = calendarService.updateEvent(args)
                    System.err.println("[MCPServer] update_event completed successfully")
                    result
                }

                "delete_event" -> {
                    val args = params.arguments?.let { parseDeleteEventParams(it) }
                        ?: throw IllegalArgumentException("Missing required arguments")
                    System.err.println("[MCPServer] delete_event: eventId=${args.eventId}, calendarId=${args.calendarId}")
                    val result = calendarService.deleteEvent(args)
                    System.err.println("[MCPServer] delete_event completed successfully")
                    result
                }

                else -> {
                    System.err.println("[MCPServer] ERROR: Unknown tool: ${params.name}")
                    throw IllegalArgumentException("Unknown tool: ${params.name}")
                }
            }
        } catch (e: Exception) {
            System.err.println("[MCPServer] ERROR: Tool call failed: ${e.message}")
            e.printStackTrace(System.err)
            return JSONRPCResponse(
                id = request.id,
                result = json.encodeToJsonElement(
                    CallToolResult(
                        content = listOf(TextContent(text = "Error: ${e.message}")),
                        isError = true
                    )
                )
            )
        }

        val result = CallToolResult(
            content = listOf(TextContent(text = resultText))
        )

        return JSONRPCResponse(
            id = request.id,
            result = json.encodeToJsonElement(result)
        )
    }

    private fun parseListEventsParams(args: JsonObject): GoogleCalendarService.ListEventsParams {
        return GoogleCalendarService.ListEventsParams(
            maxResults = args["maxResults"]?.jsonPrimitive?.intOrNull ?: 10,
            timeMin = args["timeMin"]?.jsonPrimitive?.contentOrNull,
            timeMax = args["timeMax"]?.jsonPrimitive?.contentOrNull,
            calendarId = args["calendarId"]?.jsonPrimitive?.contentOrNull ?: "primary"
        )
    }

    private fun parseCreateEventParams(args: JsonObject): GoogleCalendarService.CreateEventParams {
        return GoogleCalendarService.CreateEventParams(
            summary = args["summary"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("summary is required"),
            description = args["description"]?.jsonPrimitive?.contentOrNull,
            startDateTime = args["startDateTime"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("startDateTime is required"),
            endDateTime = args["endDateTime"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("endDateTime is required"),
            timeZone = args["timeZone"]?.jsonPrimitive?.contentOrNull ?: "UTC",
            attendees = args["attendees"]?.jsonArray?.map { it.jsonPrimitive.content },
            calendarId = args["calendarId"]?.jsonPrimitive?.contentOrNull ?: "primary"
        )
    }

    private fun parseUpdateEventParams(args: JsonObject): GoogleCalendarService.UpdateEventParams {
        return GoogleCalendarService.UpdateEventParams(
            eventId = args["eventId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("eventId is required"),
            summary = args["summary"]?.jsonPrimitive?.contentOrNull,
            description = args["description"]?.jsonPrimitive?.contentOrNull,
            startDateTime = args["startDateTime"]?.jsonPrimitive?.contentOrNull,
            endDateTime = args["endDateTime"]?.jsonPrimitive?.contentOrNull,
            timeZone = args["timeZone"]?.jsonPrimitive?.contentOrNull,
            calendarId = args["calendarId"]?.jsonPrimitive?.contentOrNull ?: "primary"
        )
    }

    private fun parseDeleteEventParams(args: JsonObject): GoogleCalendarService.DeleteEventParams {
        return GoogleCalendarService.DeleteEventParams(
            eventId = args["eventId"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("eventId is required"),
            calendarId = args["calendarId"]?.jsonPrimitive?.contentOrNull ?: "primary"
        )
    }
}
