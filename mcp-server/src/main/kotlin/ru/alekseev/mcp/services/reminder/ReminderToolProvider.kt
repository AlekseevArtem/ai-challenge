package ru.alekseev.mcp.services.reminder

import AddReminderParams
import UpdateReminderParams
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import ru.alekseev.mcp.MCPToolProvider
import ru.alekseev.mcp.models.Tool

/**
 * Provides Reminder tools for MCP server
 */
class ReminderToolProvider(
    private val reminderService: ReminderService
) : MCPToolProvider {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    override fun getTools(): List<Tool> = listOf(
        Tool(
            name = "add_reminder",
            description = "Add a new reminder/task to the list. Use this when user wants to remember something or create a todo item.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("title", buildJsonObject {
                        put("type", "string")
                        put("description", "Title of the reminder")
                    })
                    put("description", buildJsonObject {
                        put("type", "string")
                        put("description", "Detailed description (optional)")
                    })
                    put("priority", buildJsonObject {
                        put("type", "string")
                        put("description", "Priority level: 'high', 'medium', or 'low' (default: 'medium')")
                        put("enum", buildJsonArray {
                            add("high")
                            add("medium")
                            add("low")
                        })
                        put("default", "medium")
                    })
                    put("dueDate", buildJsonObject {
                        put("type", "number")
                        put("description", "Due date as Unix timestamp in milliseconds (optional)")
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "Tags for categorization (optional)")
                        put("items", buildJsonObject {
                            put("type", "string")
                        })
                    })
                })
                put("required", buildJsonArray {
                    add("title")
                })
            }
        ),
        Tool(
            name = "list_reminders",
            description = "List all reminders/tasks. Can filter by completion status and priority.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("completed", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Filter by completion status. null = all, true = completed only, false = uncompleted only")
                    })
                    put("priority", buildJsonObject {
                        put("type", "string")
                        put("description", "Filter by priority: 'high', 'medium', or 'low'")
                        put("enum", buildJsonArray {
                            add("high")
                            add("medium")
                            add("low")
                        })
                    })
                })
            }
        ),
        Tool(
            name = "get_reminders_summary",
            description = "Get a brief summary of all reminders including statistics and top priority tasks. Perfect for periodic status updates.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {})
            }
        ),
        Tool(
            name = "mark_reminder_completed",
            description = "Mark a reminder as completed or uncompleted.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", "number")
                        put("description", "ID of the reminder")
                    })
                    put("completed", buildJsonObject {
                        put("type", "boolean")
                        put("description", "Completion status (default: true)")
                        put("default", true)
                    })
                })
                put("required", buildJsonArray {
                    add("id")
                })
            }
        ),
        Tool(
            name = "update_reminder",
            description = "Update an existing reminder's properties.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", "number")
                        put("description", "ID of the reminder to update")
                    })
                    put("title", buildJsonObject {
                        put("type", "string")
                        put("description", "New title (optional)")
                    })
                    put("description", buildJsonObject {
                        put("type", "string")
                        put("description", "New description (optional)")
                    })
                    put("priority", buildJsonObject {
                        put("type", "string")
                        put("description", "New priority (optional)")
                        put("enum", buildJsonArray {
                            add("high")
                            add("medium")
                            add("low")
                        })
                    })
                    put("completed", buildJsonObject {
                        put("type", "boolean")
                        put("description", "New completion status (optional)")
                    })
                    put("dueDate", buildJsonObject {
                        put("type", "number")
                        put("description", "New due date as Unix timestamp (optional)")
                    })
                    put("tags", buildJsonObject {
                        put("type", "array")
                        put("description", "New tags (optional)")
                        put("items", buildJsonObject {
                            put("type", "string")
                        })
                    })
                })
                put("required", buildJsonArray {
                    add("id")
                })
            }
        ),
        Tool(
            name = "delete_reminder",
            description = "Delete a reminder permanently.",
            inputSchema = buildJsonObject {
                put("type", "object")
                put("properties", buildJsonObject {
                    put("id", buildJsonObject {
                        put("type", "number")
                        put("description", "ID of the reminder to delete")
                    })
                })
                put("required", buildJsonArray {
                    add("id")
                })
            }
        )
    )

    override fun handleToolCall(toolName: String, arguments: JsonObject?): String {
        System.err.println("[ReminderToolProvider] Calling tool: $toolName")
        System.err.println("[ReminderToolProvider] Tool arguments: $arguments")

        return when (toolName) {
            "add_reminder" -> {
                val args = parseParams<AddReminderParams>(arguments)
                    ?: throw IllegalArgumentException("Missing required arguments")
                System.err.println("[ReminderToolProvider] add_reminder: title=${args.title}, priority=${args.priority}")
                reminderService.addReminder(args).also {
                    System.err.println("[ReminderToolProvider] add_reminder completed successfully")
                }
            }

            "list_reminders" -> {
                val completed = arguments?.get("completed")?.jsonPrimitive?.booleanOrNull
                val priority = arguments?.get("priority")?.jsonPrimitive?.contentOrNull
                System.err.println("[ReminderToolProvider] list_reminders: completed=$completed, priority=$priority")
                reminderService.listReminders(completed, priority).also {
                    System.err.println("[ReminderToolProvider] list_reminders completed successfully")
                }
            }

            "get_reminders_summary" -> {
                System.err.println("[ReminderToolProvider] get_reminders_summary")
                reminderService.getSummary().also {
                    System.err.println("[ReminderToolProvider] get_reminders_summary completed successfully")
                }
            }

            "mark_reminder_completed" -> {
                val id = arguments?.get("id")?.jsonPrimitive?.longOrNull
                    ?: throw IllegalArgumentException("id is required")
                val completed = arguments?.get("completed")?.jsonPrimitive?.booleanOrNull ?: true
                System.err.println("[ReminderToolProvider] mark_reminder_completed: id=$id, completed=$completed")
                reminderService.markCompleted(id, completed).also {
                    System.err.println("[ReminderToolProvider] mark_reminder_completed completed successfully")
                }
            }

            "update_reminder" -> {
                val args = parseParams<UpdateReminderParams>(arguments)
                    ?: throw IllegalArgumentException("Missing required arguments")
                System.err.println("[ReminderToolProvider] update_reminder: id=${args.id}")
                reminderService.updateReminder(args).also {
                    System.err.println("[ReminderToolProvider] update_reminder completed successfully")
                }
            }

            "delete_reminder" -> {
                val id = arguments?.get("id")?.jsonPrimitive?.longOrNull
                    ?: throw IllegalArgumentException("id is required")
                System.err.println("[ReminderToolProvider] delete_reminder: id=$id")
                reminderService.deleteReminder(id).also {
                    System.err.println("[ReminderToolProvider] delete_reminder completed successfully")
                }
            }

            else -> throw IllegalArgumentException("Unknown tool: $toolName")
        }
    }

    private inline fun <reified T> parseParams(args: JsonObject?): T? {
        return args?.let { json.decodeFromJsonElement<T>(it) }
    }
}
