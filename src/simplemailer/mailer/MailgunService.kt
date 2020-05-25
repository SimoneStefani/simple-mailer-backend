package dev.simonestefani.simplemailer.mailer

import com.commit451.mailgun.Contact
import com.commit451.mailgun.Mailgun
import com.commit451.mailgun.SendMessageRequest

class MailgunService(private val mg: Mailgun) : MailerService {
    override fun send(email: dev.simonestefani.simplemailer.models.Email) {
        val from = Contact(email.fromEmail, "Sender")
        val to = listOf(Contact(email.toEmail, "recipient"))

        val requestBuilder = SendMessageRequest.Builder(from).to(to)
            .subject(email.subject)
            .text(email.content)

        mg.sendMessage(requestBuilder.build()).blockingGet()
    }
}
