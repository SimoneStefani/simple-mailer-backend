package dev.simonestefani.simplemailer.models

import io.ktor.auth.Principal
import java.io.Serializable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String
) : Serializable, Principal

object Users : Table() {
    val id : Column<Int> = integer("id").autoIncrement().primaryKey()
    val email = varchar("email", 256).uniqueIndex()
    val name = varchar("name", 256)
    val passwordHash = varchar("password_hash", 256)
}

fun User.serialize(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "email" to email,
        "name" to name
    )
}