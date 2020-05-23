package dev.simonestefani

import dev.simonestefani.simplemailer.api.emailsRoutes
import dev.simonestefani.simplemailer.api.usersRoutes
import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.authentication.hash
import dev.simonestefani.simplemailer.persistence.DatabaseFactory
import dev.simonestefani.simplemailer.persistence.ExposedRepository
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.jwt.jwt
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    DatabaseFactory.init()
    val repo = ExposedRepository()

    val jwtService = JwtService()
    val hashFunction = { s: String -> hash(s) }

    install(StatusPages)

    install(Authentication) {
        jwt("jwt") {
            verifier(jwtService.verifier)
            validate { credential ->
                val claim = credential.payload.getClaim("id").asInt()
                repo.findUser(claim)
            }
        }
    }

    install(ContentNegotiation) {
        gson()
    }

    routing {
        route("v1") {
            usersRoutes(repo, jwtService, hashFunction)
            emailsRoutes(repo)
        }
    }
}
