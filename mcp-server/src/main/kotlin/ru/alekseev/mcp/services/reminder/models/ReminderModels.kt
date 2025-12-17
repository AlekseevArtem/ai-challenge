import kotlinx.serialization.Serializable


@Serializable
data class Reminder(
    val id: Long,
    val title: String,
    val description: String?,
    val priority: String,
    val completed: Boolean,
    val createdAt: Long,
    val dueDate: Long?,
    val tags: List<String>,
)

@Serializable
data class AddReminderParams(
    val title: String,
    val description: String? = null,
    val priority: String = "medium",
    val dueDate: Long? = null,
    val tags: List<String> = emptyList(),
)

@Serializable
data class UpdateReminderParams(
    val id: Long,
    val title: String? = null,
    val description: String? = null,
    val priority: String? = null,
    val completed: Boolean? = null,
    val dueDate: Long? = null,
    val tags: List<String>? = null,
)