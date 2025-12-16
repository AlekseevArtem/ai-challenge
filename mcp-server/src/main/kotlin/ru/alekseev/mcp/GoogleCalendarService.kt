package ru.alekseev.mcp

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventAttendee
import com.google.api.services.calendar.model.EventDateTime
import java.io.File
import java.io.FileReader

class GoogleCalendarService {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val scopes = listOf(CalendarScopes.CALENDAR)
    private val credentialsFilePath = System.getenv("GOOGLE_CALENDAR_CREDENTIALS_PATH")
        ?: throw IllegalStateException("GOOGLE_CALENDAR_CREDENTIALS_PATH environment variable is not set")
    private val tokensDirectoryPath = System.getenv("GOOGLE_CALENDAR_TOKENS_PATH")
        ?: throw IllegalStateException("GOOGLE_CALENDAR_TOKENS_PATH environment variable is not set")

    init {
        System.err.println("[GoogleCalendarService] Initializing GoogleCalendarService")
        System.err.println("[GoogleCalendarService] Credentials path: $credentialsFilePath")
        System.err.println("[GoogleCalendarService] Tokens path: $tokensDirectoryPath")
    }

    private fun getCredentials(): Credential {
        System.err.println("[GoogleCalendarService] Loading credentials from: $credentialsFilePath")

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            FileReader(credentialsFilePath)
        )

        System.err.println("[GoogleCalendarService] Building OAuth flow")
        val flow = GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            clientSecrets,
            scopes
        )
            .setDataStoreFactory(FileDataStoreFactory(File(tokensDirectoryPath)))
            .setAccessType("offline")
            .build()

        // Try to load existing credentials first
        System.err.println("[GoogleCalendarService] Attempting to load existing credentials")
        val credential = flow.loadCredential("user")

        // Check if credential is valid (not null and has necessary tokens)
        if (credential != null) {
            System.err.println("[GoogleCalendarService] Credential loaded from storage")
            System.err.println("[GoogleCalendarService] Has access token: ${credential.accessToken != null}")
            System.err.println("[GoogleCalendarService] Has refresh token: ${credential.refreshToken != null}")

            if (credential.refreshToken != null) {
                System.err.println("[GoogleCalendarService] Valid credentials found")
                return credential
            } else {
                System.err.println("[GoogleCalendarService] WARNING: Credential exists but missing refresh token")
            }
        } else {
            System.err.println("[GoogleCalendarService] No stored credentials found")
        }

        // Check if we're in a headless/Docker environment
        val isHeadless = System.getenv("DISPLAY") == null ||
                        System.getenv("MCP_HEADLESS") == "true" ||
                        !java.awt.GraphicsEnvironment.isHeadless().not()

        if (isHeadless) {
            System.err.println("[GoogleCalendarService] ERROR: Running in headless environment, cannot start OAuth flow")
            System.err.println("[GoogleCalendarService] ERROR: Please run authorization locally first:")
            System.err.println("[GoogleCalendarService] ERROR:   ./authorize-google-calendar.sh")
            throw IllegalStateException(
                "Google Calendar is not authorized. Please run './authorize-google-calendar.sh' locally to authorize access."
            )
        }

        // If no stored credentials found, start OAuth flow
        System.err.println("[GoogleCalendarService] Starting interactive OAuth flow")
        // Use port 0 to let the OS assign an available port automatically
        val receiver = LocalServerReceiver.Builder().setPort(0).build()

        // Redirect stdout temporarily to stderr to prevent JSON-RPC pollution
        val originalOut = System.out
        try {
            System.setOut(System.err)
            System.err.println("[GoogleCalendarService] OAuth receiver started on port: ${receiver.port}")
            val newCredential = AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
            System.err.println("[GoogleCalendarService] OAuth flow completed successfully")
            return newCredential
        } finally {
            System.setOut(originalOut)
        }
    }

    private val calendar: Calendar by lazy {
        System.err.println("[GoogleCalendarService] Initializing Google Calendar API client")
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val cal = Calendar.Builder(httpTransport, jsonFactory, getCredentials())
            .setApplicationName("Google Calendar MCP Server")
            .build()
        System.err.println("[GoogleCalendarService] Google Calendar API client initialized successfully")
        cal
    }

    data class ListEventsParams(
        val maxResults: Int = 10,
        val timeMin: String? = null,
        val timeMax: String? = null,
        val calendarId: String = "primary"
    )

    fun listEvents(params: ListEventsParams): String {
        System.err.println("[GoogleCalendarService] listEvents called with params: $params")

        val timeMin = params.timeMin?.let { DateTime(it) }
            ?: DateTime(System.currentTimeMillis())

        System.err.println("[GoogleCalendarService] Fetching events from calendar: ${params.calendarId}")
        val request = calendar.events().list(params.calendarId)
            .setMaxResults(params.maxResults)
            .setTimeMin(timeMin)
            .setSingleEvents(true)
            .setOrderBy("startTime")

        params.timeMax?.let { request.timeMax = DateTime(it) }

        val events = request.execute()
        val items = events.items
        System.err.println("[GoogleCalendarService] Retrieved ${items.size} events")

        if (items.isEmpty()) {
            return "No upcoming events found."
        }

        return buildString {
            appendLine("Found ${items.size} event(s):\n")
            items.forEach { event ->
                val start = event.start.dateTime ?: event.start.date
                val end = event.end.dateTime ?: event.end.date
                appendLine("- ${event.summary} (ID: ${event.id})")
                appendLine("  Start: $start")
                appendLine("  End: $end")
                event.description?.let { appendLine("  Description: $it") }
                appendLine()
            }
        }
    }

    data class CreateEventParams(
        val summary: String,
        val description: String? = null,
        val startDateTime: String,
        val endDateTime: String,
        val timeZone: String = "UTC",
        val attendees: List<String>? = null,
        val calendarId: String = "primary"
    )

    fun createEvent(params: CreateEventParams): String {
        System.err.println("[GoogleCalendarService] createEvent called with params: $params")

        val event = Event().apply {
            summary = params.summary
            description = params.description
            start = EventDateTime()
                .setDateTime(DateTime(params.startDateTime))
                .setTimeZone(params.timeZone)
            end = EventDateTime()
                .setDateTime(DateTime(params.endDateTime))
                .setTimeZone(params.timeZone)

            params.attendees?.let { emails ->
                attendees = emails.map { EventAttendee().setEmail(it) }
                System.err.println("[GoogleCalendarService] Adding ${emails.size} attendees")
            }
        }

        System.err.println("[GoogleCalendarService] Creating event in calendar: ${params.calendarId}")
        val createdEvent = calendar.events()
            .insert(params.calendarId, event)
            .execute()

        System.err.println("[GoogleCalendarService] Event created successfully with ID: ${createdEvent.id}")

        return buildString {
            appendLine("Event created successfully!")
            appendLine("ID: ${createdEvent.id}")
            appendLine("Summary: ${createdEvent.summary}")
            appendLine("Start: ${createdEvent.start.dateTime}")
            appendLine("End: ${createdEvent.end.dateTime}")
            appendLine("Link: ${createdEvent.htmlLink}")
        }
    }

    data class UpdateEventParams(
        val eventId: String,
        val summary: String? = null,
        val description: String? = null,
        val startDateTime: String? = null,
        val endDateTime: String? = null,
        val timeZone: String? = null,
        val calendarId: String = "primary"
    )

    fun updateEvent(params: UpdateEventParams): String {
        System.err.println("[GoogleCalendarService] updateEvent called for eventId: ${params.eventId}")

        System.err.println("[GoogleCalendarService] Fetching existing event from calendar: ${params.calendarId}")
        val existingEvent = calendar.events()
            .get(params.calendarId, params.eventId)
            .execute()

        System.err.println("[GoogleCalendarService] Existing event found: ${existingEvent.summary}")

        existingEvent.apply {
            params.summary?.let {
                System.err.println("[GoogleCalendarService] Updating summary to: $it")
                summary = it
            }
            params.description?.let {
                System.err.println("[GoogleCalendarService] Updating description")
                description = it
            }

            if (params.startDateTime != null) {
                System.err.println("[GoogleCalendarService] Updating start time to: ${params.startDateTime}")
                start = EventDateTime()
                    .setDateTime(DateTime(params.startDateTime))
                    .setTimeZone(params.timeZone ?: start.timeZone)
            }

            if (params.endDateTime != null) {
                System.err.println("[GoogleCalendarService] Updating end time to: ${params.endDateTime}")
                end = EventDateTime()
                    .setDateTime(DateTime(params.endDateTime))
                    .setTimeZone(params.timeZone ?: end.timeZone)
            }
        }

        System.err.println("[GoogleCalendarService] Updating event in calendar")
        val updatedEvent = calendar.events()
            .update(params.calendarId, params.eventId, existingEvent)
            .execute()

        System.err.println("[GoogleCalendarService] Event updated successfully: ${updatedEvent.id}")

        return buildString {
            appendLine("Event updated successfully!")
            appendLine("ID: ${updatedEvent.id}")
            appendLine("Summary: ${updatedEvent.summary}")
            appendLine("Start: ${updatedEvent.start.dateTime}")
            appendLine("End: ${updatedEvent.end.dateTime}")
        }
    }

    data class DeleteEventParams(
        val eventId: String,
        val calendarId: String = "primary"
    )

    fun deleteEvent(params: DeleteEventParams): String {
        System.err.println("[GoogleCalendarService] deleteEvent called for eventId: ${params.eventId}, calendarId: ${params.calendarId}")

        calendar.events()
            .delete(params.calendarId, params.eventId)
            .execute()

        System.err.println("[GoogleCalendarService] Event deleted successfully: ${params.eventId}")

        return "Event ${params.eventId} deleted successfully from calendar ${params.calendarId}."
    }
}
