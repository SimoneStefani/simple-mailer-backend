package dev.simonestefani.simplemailer.models

import io.ktor.auth.Principal
import java.io.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

// User entity class
data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String
) : Serializable, Principal

// Companion object to User entity class with DB table mapping
object Users : IntIdTable() {
    val email = varchar("email", 256).uniqueIndex()
    val name = varchar("name", 256)
    val passwordHash = varchar("password_hash", 256)
}

// Custom serialization
fun User.serialize(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "email" to email,
        "name" to name
    )
}
