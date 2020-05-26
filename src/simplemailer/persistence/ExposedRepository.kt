package dev.simonestefani.simplemailer.persistence

import dev.simonestefani.simplemailer.models.Email
import dev.simonestefani.simplemailer.models.Emails
import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.models.Users
import dev.simonestefani.simplemailer.persistence.DatabaseFactory.asyncQuery
import io.ktor.util.KtorExperimentalAPI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement

@KtorExperimentalAPI
class ExposedRepository : ApplicationRepository {
    override suspend fun createUser(email: String, name: String, passwordHash: String): User? {
        var statement: InsertStatement<Number>? = null

        asyncQuery {
            statement = Users.insert { user ->
                user[Users.email] = email
                user[Users.name] = name
                user[Users.passwordHash] = passwordHash
            }
        }

        return toUser(statement?.resultedValues?.first())
    }

    override suspend fun findUser(userId: Int) = asyncQuery {
        Users.select { Users.id.eq(userId) }.map { toUser(it) }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = asyncQuery {
        Users.select { Users.email.eq(email) }.map { toUser(it) }.singleOrNull()
    }

    override suspend fun createEmail(from: User, to: String, subject: String, content: String): Email? {
        var statement: InsertStatement<Number>? = null

        asyncQuery {
            statement = Emails.insert { email ->
                email[senderId] = from.id
                email[fromEmail] = from.email
                email[toEmail] = to
                email[Emails.subject] = subject
                email[Emails.content] = content
                email[createdAt] = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
                email[updatedAt] = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC)
            }
        }

        return toEmail(statement?.resultedValues?.first())
    }

    override suspend fun findEmailsBySender(senderId: Int) = asyncQuery {
        Emails.select { Emails.senderId.eq(senderId) }
            .orderBy(Emails.createdAt to SortOrder.DESC)
            .limit(5)
            .map { toEmail(it) }
    }

    private fun toUser(row: ResultRow?): User? {
        return row?.let {
            User(
                id = row[Users.id].value,
                email = row[Users.email],
                name = row[Users.name],
                passwordHash = row[Users.passwordHash]
            )
        }
    }

    private fun toEmail(row: ResultRow?): Email? {
        return row?.let {
            Email(
                id = row[Emails.id].value,
                senderId = row[Emails.senderId],
                fromEmail = row[Emails.fromEmail],
                toEmail = row[Emails.toEmail],
                subject = row[Emails.subject],
                content = row[Emails.content],
                createdAt = row[Emails.createdAt].toInstant(ZoneOffset.UTC),
                updatedAt = row[Emails.updatedAt].toInstant(ZoneOffset.UTC)
            )
        }
    }
}
