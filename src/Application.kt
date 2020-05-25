package dev.simonestefani

import com.commit451.mailgun.Mailgun
import com.sendgrid.SendGrid
import dev.simonestefani.simplemailer.api.emailsRoutes
import dev.simonestefani.simplemailer.api.usersRoutes
import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.authentication.hash
import dev.simonestefani.simplemailer.mailer.MailgunService
import dev.simonestefani.simplemailer.mailer.RedundantMailerService
import dev.simonestefani.simplemailer.mailer.SendGridService
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

    val sendGrid = SendGrid(System.getenv("SENDGRID_API_KEY"))
    val mailgun = Mailgun
        .Builder(System.getenv("MAILGUN_DOMAIN"), System.getenv("MAILGUN_API_KEY"))
        .build()

    val repo = ExposedRepository()
    val jwtService = JwtService()
    val mailerService = RedundantMailerService(
        primaryMailerService = MailgunService(mailgun),
        backupMailerService = SendGridService(sendGrid)
    )

    moduleWithDependencies(repo, jwtService, mailerService)
}

@KtorExperimentalAPI
fun Application.moduleWithDependencies(
    repo: ExposedRepository,
    jwtService: JwtService,
    mailerService: RedundantMailerService
) {
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
            emailsRoutes(repo, mailerService)
        }
    }
}
