package dev.simonestefani.simplemailer.api

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

fun Route.emailsRoutes(repo: ExposedRepository) {
    route("/emails") {
        authenticate("jwt") {
            get {
                val profile: User = call.authentication.principal() ?: return@get call.respond(HttpStatusCode.Unauthorized)

                repo.findEmailsBySender(profile.id).let { emails ->
                    call.respond(HttpStatusCode.OK, emails.mapNotNull { it?.serialize() })
                }
            }

            post {
                val profile: User = call.authentication.principal() ?: return@post call.respond(HttpStatusCode.Unauthorized)

                val params = call.receive<Map<String, String>>()
                val to = params["to"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
                val subject = params["subject"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
                val content = params["content"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

                repo.createEmail(profile, to, subject, content)?.also { newEmail ->
                    // TODO send email
                    call.respond(HttpStatusCode.Created, newEmail.serialize())
                } ?: call.respond(HttpStatusCode.InternalServerError)
            }
        }
    }
}
