package dev.simonestefani.simplemailer.models

import java.io.Serializable
import java.time.Instant
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.`java-time`.datetime

data class Email(
    val id: Int,
    val senderId: Int,
    val fromEmail: String,
    val toEmail: String,
    val subject: String,
    val content: String,
    val createdAt: Instant,
    val updatedAt: Instant
) : Serializable

object Emails : IntIdTable() {
    val senderId = integer("sender_id")
    val fromEmail = varchar("from_email", 256)
    val toEmail = varchar("to_email", 256)
    val subject = text("subject")
    val content = text("content")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

fun Email.serialize(): Map<String, Any> {
    return mapOf(
        "id" to id,
        "from" to fromEmail,
        "to" to toEmail,
        "subject" to subject,
        "content" to content,
        "sent_at" to createdAt
    )
}
