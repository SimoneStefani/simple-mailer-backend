package dev.simonestefani.simplemailer.mailer

import com.sendgrid.Method
import com.sendgrid.Request
import com.sendgrid.SendGrid
import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email

class SendGridService(private val sg: SendGrid): MailerService {
    override fun send(email: dev.simonestefani.simplemailer.models.Email) {
        val mail = Mail(
            Email(email.fromEmail),
            email.subject,
            Email(email.toEmail),
            Content("text/plain", email.content)
        )

        val request = Request()
        request.method = Method.POST
        request.endpoint = "mail/send"
        request.body = mail.build()

        sg.api(request)
    }
}