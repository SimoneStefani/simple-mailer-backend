package dev.simonestefani.simplemailer.models

import io.ktor.auth.Principal
import org.jetbrains.exposed.dao.id.IntIdTable
import java.io.Serializable

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String
) : Serializable, Principal

object Users : IntIdTable() {
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