package ru.alekseev.mcp.services.reminder

import AddReminderParams
import Reminder
import UpdateReminderParams
import java.sql.Connection
import java.sql.DriverManager
import ru.alekseev.myapplication.core.common.logTag
import java.sql.ResultSet
import java.sql.Types
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderService(private val dbPath: String = "reminders.db") {
    private var connection: Connection? = null

    init {
        initDatabase()
    }

    private fun initDatabase() {
        connection = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        connection?.createStatement()?.execute(
            """
            CREATE TABLE IF NOT EXISTS reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                priority TEXT DEFAULT 'medium',
                completed INTEGER DEFAULT 0,
                created_at INTEGER NOT NULL,
                due_date INTEGER,
                tags TEXT
            )
            """.trimIndent()
        )
        System.err.println("$logTag Database initialized at $dbPath")
    }


    fun addReminder(params: AddReminderParams): String {
        val now = System.currentTimeMillis()
        val statement = connection?.prepareStatement(
            """
            INSERT INTO reminders (title, description, priority, created_at, due_date, tags)
            VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ) ?: throw IllegalStateException("Database not initialized")

        statement.setString(1, params.title)
        statement.setString(2, params.description)
        statement.setString(3, params.priority)
        statement.setLong(4, now)
        params.dueDate?.let { statement.setLong(5, it) } ?: statement.setNull(5, Types.INTEGER)
        statement.setString(6, params.tags.joinToString(","))

        statement.executeUpdate()
        val generatedKeys = statement.generatedKeys
        val id = if (generatedKeys.next()) generatedKeys.getLong(1) else -1

        System.err.println("$logTag Added reminder: id=$id, title=${params.title}")
        return "✓ Напоминание добавлено (ID: $id)\nЗаголовок: ${params.title}\nПриоритет: ${params.priority}"
    }

    fun listReminders(completed: Boolean? = null, priority: String? = null): String {
        val query = buildString {
            append("SELECT * FROM reminders WHERE 1=1")
            if (completed != null) {
                append(" AND completed = ${if (completed) 1 else 0}")
            }
            if (priority != null) {
                append(" AND priority = '$priority'")
            }
            append(" ORDER BY completed ASC, priority DESC, due_date ASC, created_at DESC")
        }

        val statement =
            connection?.createStatement() ?: throw IllegalStateException("Database not initialized")
        val resultSet = statement.executeQuery(query)
        val reminders = mutableListOf<Reminder>()

        while (resultSet.next()) {
            reminders.add(resultSet.toReminder())
        }

        System.err.println("$logTag Listed ${reminders.size} reminders")

        if (reminders.isEmpty()) {
            return "Нет напоминаний"
        }

        return buildString {
            appendLine("📋 Всего напоминаний: ${reminders.size}\n")
            reminders.forEachIndexed { index, reminder ->
                val status = if (reminder.completed) "✓" else "○"
                val priorityEmoji = when (reminder.priority) {
                    "high" -> "🔴"
                    "medium" -> "🟡"
                    "low" -> "🟢"
                    else -> "⚪"
                }
                appendLine("$status $priorityEmoji [${reminder.id}] ${reminder.title}")
                if (reminder.description != null) {
                    appendLine("   ${reminder.description}")
                }
                if (reminder.dueDate != null) {
                    val dueDate = formatTimestamp(reminder.dueDate)
                    appendLine("   📅 Срок: $dueDate")
                }
                if (reminder.tags.isNotEmpty()) {
                    appendLine("   🏷️ ${reminder.tags.joinToString(", ")}")
                }
                if (index < reminders.size - 1) appendLine()
            }
        }
    }

    fun getSummary(): String {
        val statement =
            connection?.createStatement() ?: throw IllegalStateException("Database not initialized")

        val totalResult = statement.executeQuery("SELECT COUNT(*) FROM reminders")
        val total = if (totalResult.next()) totalResult.getInt(1) else 0

        val completedResult =
            statement.executeQuery("SELECT COUNT(*) FROM reminders WHERE completed = 1")
        val completed = if (completedResult.next()) completedResult.getInt(1) else 0

        val highPriorityResult =
            statement.executeQuery("SELECT COUNT(*) FROM reminders WHERE priority = 'high' AND completed = 0")
        val highPriority = if (highPriorityResult.next()) highPriorityResult.getInt(1) else 0

        // Get top 3 important uncompleted tasks
        val topTasksResult = statement.executeQuery(
            """
            SELECT * FROM reminders
            WHERE completed = 0
            ORDER BY
                CASE priority
                    WHEN 'high' THEN 1
                    WHEN 'medium' THEN 2
                    WHEN 'low' THEN 3
                END,
                due_date ASC,
                created_at DESC
            LIMIT 3
            """.trimIndent()
        )

        val topTasks = mutableListOf<Reminder>()
        while (topTasksResult.next()) {
            topTasks.add(topTasksResult.toReminder())
        }

        System.err.println("$logTag Generated summary: total=$total, completed=$completed, highPriority=$highPriority")

        return buildString {
            appendLine("📊 Сводка по напоминаниям")
            appendLine("━━━━━━━━━━━━━━━━━━━━")
            appendLine("📝 Всего задач: $total")
            appendLine("✅ Выполнено: $completed")
            appendLine("⏳ Осталось: ${total - completed}")
            if (highPriority > 0) {
                appendLine("🔴 Важных задач: $highPriority")
            }

            if (topTasks.isNotEmpty()) {
                appendLine("\n🎯 Топ-${topTasks.size} задачи:")
                topTasks.forEachIndexed { index, task ->
                    val priorityEmoji = when (task.priority) {
                        "high" -> "🔴"
                        "medium" -> "🟡"
                        "low" -> "🟢"
                        else -> "⚪"
                    }
                    appendLine("${index + 1}. $priorityEmoji ${task.title}")
                    if (task.dueDate != null) {
                        val dueDate = formatTimestamp(task.dueDate)
                        appendLine("   📅 Срок: $dueDate")
                    }
                }
            }
        }
    }

    fun markCompleted(id: Long, completed: Boolean = true): String {
        val statement = connection?.prepareStatement(
            "UPDATE reminders SET completed = ? WHERE id = ?"
        ) ?: throw IllegalStateException("Database not initialized")

        statement.setInt(1, if (completed) 1 else 0)
        statement.setLong(2, id)

        val updated = statement.executeUpdate()

        return if (updated > 0) {
            System.err.println("$logTag Marked reminder $id as ${if (completed) "completed" else "uncompleted"}")
            "✓ Напоминание ${if (completed) "выполнено" else "отмечено как невыполненное"} (ID: $id)"
        } else {
            System.err.println("$logTag Reminder $id not found")
            "✗ Напоминание с ID $id не найдено"
        }
    }

    fun updateReminder(params: UpdateReminderParams): String {
        val updates = mutableListOf<String>()
        val values = mutableListOf<Any?>()

        params.title?.let {
            updates.add("title = ?")
            values.add(it)
        }
        params.description?.let {
            updates.add("description = ?")
            values.add(it)
        }
        params.priority?.let {
            updates.add("priority = ?")
            values.add(it)
        }
        params.completed?.let {
            updates.add("completed = ?")
            values.add(if (it) 1 else 0)
        }
        params.dueDate?.let {
            updates.add("due_date = ?")
            values.add(it)
        }
        params.tags?.let {
            updates.add("tags = ?")
            values.add(it.joinToString(","))
        }

        if (updates.isEmpty()) {
            return "✗ Нет данных для обновления"
        }

        val query = "UPDATE reminders SET ${updates.joinToString(", ")} WHERE id = ?"
        val statement = connection?.prepareStatement(query)
            ?: throw IllegalStateException("Database not initialized")

        values.forEachIndexed { index, value ->
            when (value) {
                is String -> statement.setString(index + 1, value)
                is Int -> statement.setInt(index + 1, value)
                is Long -> statement.setLong(index + 1, value)
            }
        }
        statement.setLong(values.size + 1, params.id)

        val updated = statement.executeUpdate()

        return if (updated > 0) {
            System.err.println("$logTag Updated reminder ${params.id}")
            "✓ Напоминание обновлено (ID: ${params.id})"
        } else {
            System.err.println("$logTag Reminder ${params.id} not found")
            "✗ Напоминание с ID ${params.id} не найдено"
        }
    }

    fun deleteReminder(id: Long): String {
        val statement = connection?.prepareStatement(
            "DELETE FROM reminders WHERE id = ?"
        ) ?: throw IllegalStateException("Database not initialized")

        statement.setLong(1, id)
        val deleted = statement.executeUpdate()

        return if (deleted > 0) {
            System.err.println("$logTag Deleted reminder $id")
            "✓ Напоминание удалено (ID: $id)"
        } else {
            System.err.println("$logTag Reminder $id not found")
            "✗ Напоминание с ID $id не найдено"
        }
    }

    private fun ResultSet.toReminder(): Reminder {
        val tagsString = getString("tags")
        val tags = if (tagsString.isNullOrBlank()) emptyList() else tagsString.split(",")

        return Reminder(
            id = getLong("id"),
            title = getString("title"),
            description = getString("description"),
            priority = getString("priority"),
            completed = getInt("completed") == 1,
            createdAt = getLong("created_at"),
            dueDate = getLong("due_date").takeIf { !wasNull() },
            tags = tags
        )
    }

    private fun formatTimestamp(timestamp: Long): String {
        val instant = Instant.ofEpochMilli(timestamp)
        val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun close() {
        connection?.close()
        System.err.println("$logTag Database connection closed")
    }
}