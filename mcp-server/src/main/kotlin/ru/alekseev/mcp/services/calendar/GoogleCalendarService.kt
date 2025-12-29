package ru.alekseev.mcp.services.calendar

import CreateEventParams
import DeleteEventParams
import ListEventsParams
import UpdateEventParams
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
import ru.alekseev.myapplication.core.common.OAuthDefaults
import ru.alekseev.myapplication.core.common.logTag
import java.awt.GraphicsEnvironment
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
        System.err.println("$logTag Initializing GoogleCalendarService")
        System.err.println("$logTag Credentials path: $credentialsFilePath")
        System.err.println("$logTag Tokens path: $tokensDirectoryPath")
    }

    private fun getCredentials(): Credential {
        System.err.println("$logTag Loading credentials from: $credentialsFilePath")

        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            FileReader(credentialsFilePath)
        )

        System.err.println("$logTag Building OAuth flow")
        val flow = GoogleAuthorizationCodeFlow.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            jsonFactory,
            clientSecrets,
            scopes
        )
            .setDataStoreFactory(FileDataStoreFactory(File(tokensDirectoryPath)))
            .setAccessType(OAuthDefaults.ACCESS_TYPE_OFFLINE)
            .build()

        // Try to load existing credentials first
        System.err.println("$logTag Attempting to load existing credentials")
        val credential = flow.loadCredential(OAuthDefaults.CREDENTIAL_USER_ID)

        // Check if credential is valid (not null and has necessary tokens)
        if (credential != null) {
            System.err.println("$logTag Credential loaded from storage")
            System.err.println("$logTag Has access token: ${credential.accessToken != null}")
            System.err.println("$logTag Has refresh token: ${credential.refreshToken != null}")

            if (credential.refreshToken != null) {
                System.err.println("$logTag Valid credentials found")
                return credential
            } else {
                System.err.println("$logTag WARNING: Credential exists but missing refresh token")
            }
        } else {
            System.err.println("$logTag No stored credentials found")
        }

        // Check if we're in a headless/Docker environment
        val isHeadless = System.getenv("DISPLAY") == null ||
                        System.getenv("MCP_HEADLESS") == "true" ||
                        !GraphicsEnvironment.isHeadless().not()

        if (isHeadless) {
            System.err.println("$logTag ERROR: Running in headless environment, cannot start OAuth flow")
            System.err.println("$logTag ERROR: Please run authorization locally first:")
            System.err.println("$logTag ERROR:   ./authorize-google-calendar.sh")
            throw IllegalStateException(
                "Google Calendar is not authorized. Please run './authorize-google-calendar.sh' locally to authorize access."
            )
        }

        // If no stored credentials found, start OAuth flow
        System.err.println("$logTag Starting interactive OAuth flow")
        // Use port 0 to let the OS assign an available port automatically
        val receiver = LocalServerReceiver.Builder().setPort(0).build()

        // Redirect stdout temporarily to stderr to prevent JSON-RPC pollution
        val originalOut = System.out
        try {
            System.setOut(System.err)
            System.err.println("$logTag OAuth receiver started on port: ${receiver.port}")
            val newCredential = AuthorizationCodeInstalledApp(flow, receiver).authorize(OAuthDefaults.CREDENTIAL_USER_ID)
            System.err.println("$logTag OAuth flow completed successfully")
            return newCredential
        } finally {
            System.setOut(originalOut)
        }
    }

    private val calendar: Calendar by lazy {
        System.err.println("$logTag Initializing Google Calendar API client")
        val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
        val cal = Calendar.Builder(httpTransport, jsonFactory, getCredentials())
            .setApplicationName("Google Calendar MCP Server")
            .build()
        System.err.println("$logTag Google Calendar API client initialized successfully")
        cal
    }

    fun listEvents(params: ListEventsParams): String {
        System.err.println("$logTag listEvents called with params: $params")

        val timeMin = params.timeMin?.let { DateTime(it) }
            ?: DateTime(System.currentTimeMillis())

        System.err.println("$logTag Fetching events from calendar: ${params.calendarId}")
        val request = calendar.events().list(params.calendarId)
            .setMaxResults(params.maxResults)
            .setTimeMin(timeMin)
            .setSingleEvents(true)
            .setOrderBy("startTime")

        params.timeMax?.let { request.timeMax = DateTime(it) }

        val events = request.execute()
        val items = events.items
        System.err.println("$logTag Retrieved ${items.size} events")

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

    fun createEvent(params: CreateEventParams): String {
        System.err.println("$logTag createEvent called with params: $params")

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
                System.err.println("$logTag Adding ${emails.size} attendees")
            }
        }

        System.err.println("$logTag Creating event in calendar: ${params.calendarId}")
        val createdEvent = calendar.events()
            .insert(params.calendarId, event)
            .execute()

        System.err.println("$logTag Event created successfully with ID: ${createdEvent.id}")

        return buildString {
            appendLine("Event created successfully!")
            appendLine("ID: ${createdEvent.id}")
            appendLine("Summary: ${createdEvent.summary}")
            appendLine("Start: ${createdEvent.start.dateTime}")
            appendLine("End: ${createdEvent.end.dateTime}")
            appendLine("Link: ${createdEvent.htmlLink}")
        }
    }

    fun updateEvent(params: UpdateEventParams): String {
        System.err.println("$logTag updateEvent called for eventId: ${params.eventId}")

        System.err.println("$logTag Fetching existing event from calendar: ${params.calendarId}")
        val existingEvent = calendar.events()
            .get(params.calendarId, params.eventId)
            .execute()

        System.err.println("$logTag Existing event found: ${existingEvent.summary}")

        existingEvent.apply {
            params.summary?.let {
                System.err.println("$logTag Updating summary to: $it")
                summary = it
            }
            params.description?.let {
                System.err.println("$logTag Updating description")
                description = it
            }

            if (params.startDateTime != null) {
                System.err.println("$logTag Updating start time to: ${params.startDateTime}")
                start = EventDateTime()
                    .setDateTime(DateTime(params.startDateTime))
                    .setTimeZone(params.timeZone ?: start.timeZone)
            }

            if (params.endDateTime != null) {
                System.err.println("$logTag Updating end time to: ${params.endDateTime}")
                end = EventDateTime()
                    .setDateTime(DateTime(params.endDateTime))
                    .setTimeZone(params.timeZone ?: end.timeZone)
            }
        }

        System.err.println("$logTag Updating event in calendar")
        val updatedEvent = calendar.events()
            .update(params.calendarId, params.eventId, existingEvent)
            .execute()

        System.err.println("$logTag Event updated successfully: ${updatedEvent.id}")

        return buildString {
            appendLine("Event updated successfully!")
            appendLine("ID: ${updatedEvent.id}")
            appendLine("Summary: ${updatedEvent.summary}")
            appendLine("Start: ${updatedEvent.start.dateTime}")
            appendLine("End: ${updatedEvent.end.dateTime}")
        }
    }

    fun deleteEvent(params: DeleteEventParams): String {
        System.err.println("$logTag deleteEvent called for eventId: ${params.eventId}, calendarId: ${params.calendarId}")

        calendar.events()
            .delete(params.calendarId, params.eventId)
            .execute()

        System.err.println("$logTag Event deleted successfully: ${params.eventId}")

        return "Event ${params.eventId} deleted successfully from calendar ${params.calendarId}."
    }
}