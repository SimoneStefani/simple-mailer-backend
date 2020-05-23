package dev.simonestefani.simplemailer.api

import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.models.serialize
import dev.simonestefani.simplemailer.persistence.SimpleMailerRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route

fun Route.usersRoutes(repo: SimpleMailerRepository, jwtService: JwtService, hashFunction: (String) -> String) {
    route("/users") {
        post("/register") {
            val params = call.receive<Map<String, String>>()
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val name = params["name"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

            try {
                val newUser = repo.createUser(email, name, hashFunction(password))
                newUser?.id?.let {
                    call.respond(HttpStatusCode.Created, mapOf("jwt" to jwtService.generateToken(newUser)))
                }
            } catch (e: Throwable) {
                application.log.error("Failed to register user", e)
                call.respond(HttpStatusCode.InternalServerError, "Problems creating User")
            }
        }

        post("/login") {
            val params = call.receive<Map<String, String>>()
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

            try {
                val currentUser = repo.findUserByEmail(email)
                currentUser?.id?.let {
                    when (currentUser.passwordHash == hashFunction(password)) {
                        true -> call.respond(HttpStatusCode.OK, mapOf("jwt" to jwtService.generateToken(currentUser)))
                        false -> call.respond(HttpStatusCode.Unauthorized, "The provided credentials are invalid")
                    }
                }
            } catch (e: Throwable) {
                application.log.error("Failed to login user", e)
                call.respond(HttpStatusCode.InternalServerError, "Problems retrieving User")
            }
        }

        authenticate("jwt") {
            get("/profile") {
                when (val profile: User? = call.authentication.principal()) {
                    null -> call.respond(HttpStatusCode.InternalServerError, "Couldn't get current user")
                    else -> call.respond(HttpStatusCode.OK, profile.serialize())
                }
            }
        }
    }
}