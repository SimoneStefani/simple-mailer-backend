package dev.simonestefani.simplemailer.mailer

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.commit451.mailgun.SendMessageRequest
import java.io.IOException

class MailgunService(private val mg: Mailgun) : MailerService {
    override fun send(email: dev.simonestefani.simplemailer.models.Email) {
        val from = Contact(email.fromEmail, "Sender")
        val to = listOf(Contact(email.toEmail, "recipient"))

        val requestBuilder = SendMessageRequest.Builder(from).to(to)
            .subject(email.subject)
            .text(email.content)

        val result = mg.sendMessage(requestBuilder.build()).blockingGet()

        if (result.id == null) {
            throw IOException("Failed to send email to ${email.toEmail} with Mailgun")
        }
    }
}
