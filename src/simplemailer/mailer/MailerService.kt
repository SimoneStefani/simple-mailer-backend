package dev.simonestefani.simplemailer.mailer

import dev.simonestefani.simplemailer.models.Email

interface MailerService {
    fun send(email: Email)
}
