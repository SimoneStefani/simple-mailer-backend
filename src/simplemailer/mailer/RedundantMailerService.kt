package dev.simonestefani.simplemailer.mailer

import dev.simonestefani.simplemailer.models.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RedundantMailerService(
    private val primaryMailerService: MailerService,
    private val backupMailerService: MailerService
) {
    suspend fun sendAsync(email: Email) {
        withContext(Dispatchers.IO) { send(email) }
    }

    private fun send(email: Email) {
        try {
            primaryMailerService.send(email)
        } catch (e: IOException) {
            try {
                backupMailerService.send(email)
            } catch (e: IOException) {
                // TODO throw custom exception
                throw IOException(e)
            }
        }
    }
}
