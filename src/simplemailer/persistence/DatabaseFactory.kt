package dev.simonestefani.simplemailer.persistence

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.simonestefani.simplemailer.models.Emails
import dev.simonestefani.simplemailer.models.Users
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

@KtorExperimentalAPI
object DatabaseFactory {
    fun init(appConfig: ApplicationConfig) {
        Database.connect(hikari(appConfig))

        // Creates tables if don't exist
        transaction {
            SchemaUtils.create(Users)
            SchemaUtils.create(Emails)
        }
    }

    // Setup DB connection pool with Hikari
    private fun hikari(appConfig: ApplicationConfig): HikariDataSource {
        val config = HikariConfig()

        config.driverClassName = appConfig.property("ktor.db.jdbcDriver").getString()
        config.jdbcUrl = appConfig.property("ktor.db.jdbcBaseUrl").getString()
        config.maximumPoolSize = 3
        config.isAutoCommit = false
        config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"

        appConfig.propertyOrNull("ktor.db.user")?.getString()?.let { user -> config.username = user }
        appConfig.propertyOrNull("ktor.db.password")?.getString()?.let { password -> config.password = password }

        config.validate()

        return HikariDataSource(config)
    }

    // Create wrapper function to handle DB requests asynchronously
    suspend fun <T> asyncQuery(block: () -> T): T = withContext(Dispatchers.IO) { transaction { block() } }
}
