package dev.simonestefani.simplemailer.persistence

import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.models.Users
import dev.simonestefani.simplemailer.persistence.DatabaseFactory.asyncQuery
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement

class SimpleMailerRepository: ApplicationRepository {
    override suspend fun createUser(email: String, name: String, passwordHash: String) : User? {
        var statement : InsertStatement<Number>? = null

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

    override suspend fun findUserByEmail(email: String)= asyncQuery {
        Users.select { Users.email.eq(email) }.map { toUser(it) }.singleOrNull()
    }

    private fun toUser(row: ResultRow?): User? {
        return row?.let {
            User(
                id = row[Users.id],
                email = row[Users.email],
                name = row[Users.name],
                passwordHash = row[Users.passwordHash]
            )
        }
    }
}