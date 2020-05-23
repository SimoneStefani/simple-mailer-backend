package dev.simonestefani.simplemailer.persistence

import dev.simonestefani.simplemailer.models.Email
import dev.simonestefani.simplemailer.models.User

interface ApplicationRepository {
    // User
    suspend fun createUser(email: String, name: String, passwordHash: String): User?
    suspend fun findUser(userId: Int): User?
    suspend fun findUserByEmail(email: String): User?

    // Email
    suspend fun createEmail(from: User, to: String, subject: String, content: String): Email?
    suspend fun findEmailsBySender(senderId: Int): List<Email?>
}
