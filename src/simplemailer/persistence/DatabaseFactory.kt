package dev.simonestefani.simplemailer.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.simonestefani.simplemailer.models.Emails
import dev.simonestefani.simplemailer.models.Users
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        Database.connect(hikari())

        // Creates tables if don't exist
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Emails)
        }
    }

    private fun hikari(): HikariDataSource {
        val config = HikariConfig()

        config.driverClassName = System.getenv("JDBC_DRIVER")
        config.jdbcUrl = System.getenv("JDBC_DATABASE_URL")
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        System.getenv("DB_USER")?.let { user -> config.username = user }
        System.getenv("DB_PASSWORD")?.let { password -> config.password = password }

        config.validate()

        return HikariDataSource(config)
    }

    suspend fun <T> asyncQuery(block: () -> T): T = withContext(Dispatchers.IO) { transaction { block() } }
}
