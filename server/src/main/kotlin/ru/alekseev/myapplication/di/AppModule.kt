package ru.alekseev.myapplication.di

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import org.koin.dsl.module
import ru.alekseev.myapplication.db.ChatDatabase
import ru.alekseev.myapplication.repository.ChatRepository
import ru.alekseev.myapplication.repository.ChatRepositoryImpl
import ru.alekseev.myapplication.service.ClaudeApiService
import ru.alekseev.myapplication.service.SummarizationService
import java.util.Properties

val databaseModule = module {
    single<SqlDriver> {
        // Use system temp directory or user home for database file
        val dbPath = System.getProperty("user.home") + "/chat.db"
        println("Database path: $dbPath")

        val driver = JdbcSqliteDriver(
            url = "jdbc:sqlite:$dbPath",
            properties = Properties().apply {
                put("foreign_keys", "true")
            }
        )

        // Create tables if they don't exist
        try {
            ChatDatabase.Schema.create(driver)
            println("Database tables created successfully")
        } catch (e: Exception) {
            // Tables might already exist, which is fine
            println("Database initialization: ${e.message}")
        }

        driver
    }

    single {
        ChatDatabase(driver = get())
    }
}

val serviceModule = module {
    single { ClaudeApiService() }
    single { SummarizationService(get()) }
}

val repositoryModule = module {
    single<ChatRepository> { ChatRepositoryImpl(get()) }
}

val appModules = listOf(databaseModule, serviceModule, repositoryModule)
