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
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.route
import io.ktor.routing.routing
import io.ktor.server.netty.EngineMain
import io.ktor.util.KtorExperimentalAPI
import io.sentry.Sentry

@KtorExperimentalAPI
val Application.envKind
    get() = environment.config.property("ktor.environment").getString()

@KtorExperimentalAPI
val Application.isDev
    get() = envKind == "dev"

fun main(args: Array<String>): Unit = EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    // Services configuration from application.conf
    val sendgridApiKey: String = environment.config.property("ktor.services.sendgridApiKey").getString()
    val mailgunDomain: String = environment.config.property("ktor.services.mailgunDomain").getString()
    val mailgunApiKey: String = environment.config.property("ktor.services.mailgunApiKey").getString()
    val sentryDsn: String = environment.config.property("ktor.services.sentryDsn").getString()

    // Error reporting
    Sentry.init(sentryDsn)

    // Initialize connection pool to DB and expose a repository
    DatabaseFactory.init(environment.config)
    val repo = ExposedRepository()

    // Initialize email services
    val sendGrid = SendGrid(sendgridApiKey)
    val mailgun = Mailgun.Builder(mailgunDomain, mailgunApiKey).build()

    // Initialize app services
    val jwtService = JwtService(environment.config)
    val mailerService = RedundantMailerService(
        primaryMailerService = SendGridService(sendGrid),
        backupMailerService = MailgunService(mailgun)
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

    // Serve default status pages (e.g. 404)
    install(StatusPages)

    // Log calls in dev environment
    if (isDev) install(CallLogging)

    // Enable CORS
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Get)
        method(HttpMethod.Post)
        method(HttpMethod.Head)
        header(HttpHeaders.AccessControlAllowOrigin)
        header(HttpHeaders.Origin)
        header(HttpHeaders.Accept)
        header(HttpHeaders.AcceptLanguage)
        header(HttpHeaders.ContentLanguage)
        header(HttpHeaders.ContentType)
        header(HttpHeaders.Authorization)
        anyHost() // TODO make more strict
    }

    // Setup authentication provider with JWT schema
    install(Authentication) {
        jwt("jwt") {
            verifier(jwtService.verifier)
            validate { credential ->
                val claim = credential.payload.getClaim("id").asInt()
                repo.findUser(claim)
            }
        }
    }

    // Use Gson to serialize/deserialize content to/from JSON
    install(ContentNegotiation) { gson() }

    routing {
        route("v1") {
            usersRoutes(repo, jwtService, hashFunction)
            emailsRoutes(repo, mailerService)
        }
    }
}
