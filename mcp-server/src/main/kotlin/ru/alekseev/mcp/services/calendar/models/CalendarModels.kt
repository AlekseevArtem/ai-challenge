import kotlinx.serialization.Serializable

@Serializable
data class ListEventsParams(
    val maxResults: Int = 10,
    val timeMin: String? = null,
    val timeMax: String? = null,
    val calendarId: String = "primary",
)


@Serializable
data class CreateEventParams(
    val summary: String,
    val description: String? = null,
    val startDateTime: String,
    val endDateTime: String,
    val timeZone: String = "UTC",
    val attendees: List<String>? = null,
    val calendarId: String = "primary",
)


@Serializable
data class UpdateEventParams(
    val eventId: String,
    val summary: String? = null,
    val description: String? = null,
    val startDateTime: String? = null,
    val endDateTime: String? = null,
    val timeZone: String? = null,
    val calendarId: String = "primary",
)

@Serializable
data class DeleteEventParams(
    val eventId: String,
    val calendarId: String = "primary",
)