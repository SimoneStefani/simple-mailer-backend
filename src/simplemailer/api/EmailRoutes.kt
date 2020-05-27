package dev.simonestefani.simplemailer.api

import dev.simonestefani.simplemailer.mailer.RedundantMailerService
import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.models.serialize
import dev.simonestefani.simplemailer.persistence.ExposedRepository
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import io.ktor.util.KtorExperimentalAPI
import java.io.IOException

@KtorExperimentalAPI
fun Route.emailsRoutes(repo: ExposedRepository, mailerService: RedundantMailerService) {
    route("/emails") {
        authenticate("jwt") {
            /**
             * GET /v1/emails
             *
             * Get a list with the five most recent emails sent by the authenticated user
             * @return 200 OK
             */
            get {
                // Abort if not authenticate

                val pew = call.authentication
                println(pew)
                val profile: User =
                    call.authentication.principal() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                repo.findEmailsBySender(profile.id).let { emails ->
                    call.respond(HttpStatusCode.OK, emails.mapNotNull { it?.serialize() })
                }
            }

            /**
             * POST /v1/emails
             *
             * Persist a new email in storage and send the message through a mail service
             * @return 201 Created
             */
            post {
                // Abort if not authenticated
                val profile: User =
                    call.authentication.principal() ?: return@post call.respond(HttpStatusCode.Unauthorized)

                // Parse and validate request body
                val params = call.receive<Map<String, String>>()
                val to = params["to"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
                val subject = params["subject"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
                val content = params["content"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

                // Persist email entity and send message
                repo.createEmail(profile, to, subject, content)?.also { newEmail ->
                    try {
                        mailerService.sendAsync(newEmail)
                    } catch (e: IOException) {
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                    call.respond(HttpStatusCode.Created, newEmail.serialize())
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
