package dev.simonestefani.simplemailer.mailer

import dev.simonestefani.simplemailer.models.Email
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RedundantMailerService(
    private val primaryMailerService: MailerService,
    private val backupMailerService: MailerService
) {
    suspend fun sendAsync(email: Email) {
        withContext(Dispatchers.IO) { send(email) }
    }

    // Attempt to send email with primary service. In case of exception switch
    // to secondary service.
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
