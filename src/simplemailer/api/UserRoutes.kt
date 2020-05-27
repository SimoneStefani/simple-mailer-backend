package dev.simonestefani.simplemailer.api

import dev.simonestefani.allowUserRegistration
import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.models.serialize
import dev.simonestefani.simplemailer.persistence.ExposedRepository
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
import io.ktor.util.KtorExperimentalAPI

@KtorExperimentalAPI
fun Route.usersRoutes(repo: ExposedRepository, jwtService: JwtService, hashFunction: (String) -> String) {
    route("/users") {
        /**
         * POST /v1/users/register
         *
         * Create a new user in the database and return a valid JWT.
         * @return 201 Created
         */
        post("/register") {
            if (!application.allowUserRegistration) {
                return@post call.respond(HttpStatusCode.Unauthorized)
            }

            // Parse and validate request body
            val params = call.receive<Map<String, String>>()
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val name = params["name"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

            try {
                // Persist user
                val newUser = repo.createUser(email, name, hashFunction(password))
                newUser?.id?.let {
                    call.respond(HttpStatusCode.Created, mapOf("jwt" to jwtService.generateToken(newUser)))
                }
            } catch (e: Throwable) {
                application.log.error("Failed to register user", e)
                call.respond(HttpStatusCode.InternalServerError, "Problems creating User")
            }
        }

        /**
         * POST /v1/users/login
         *
         * Sign in a user and return a valid JWT.
         * @return 200 OK
         */
        post("/login") {
            // Parse and validate request body
            val params = call.receive<Map<String, String>>()
            val password = params["password"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)
            val email = params["email"] ?: return@post call.respond(HttpStatusCode.UnprocessableEntity)

            try {
                // Find user by email
                val currentUser = repo.findUserByEmail(email)
                currentUser?.id?.let {
                    // Validate password
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
            /**
             * GET /v1/users/profile
             *
             * Get the profile of an authenticated user.
             * @return 200 OK
             */
            get("/profile") {
                when (val profile: User? = call.authentication.principal()) {
                    null -> call.respond(HttpStatusCode.Unauthorized, "Couldn't get current user")
                    else -> call.respond(HttpStatusCode.OK, profile.serialize())
                }
            }
        }
    }
}
