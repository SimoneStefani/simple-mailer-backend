package simplemailer.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import dev.simonestefani.moduleWithDependencies
import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.mailer.RedundantMailerService
import dev.simonestefani.simplemailer.models.Email
import dev.simonestefani.simplemailer.models.User
import dev.simonestefani.simplemailer.persistence.ExposedRepository
import io.ktor.application.Application
import io.ktor.config.MapApplicationConfig
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.setBody
import io.ktor.server.testing.withTestApplication
import io.ktor.util.KtorExperimentalAPI
import io.mockk.Called
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.just
import io.mockk.mockk
import java.time.Instant
import java.util.Date
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailRoutesTest {

    private val repo: ExposedRepository = mockk()
    private val mailerService: RedundantMailerService = mockk()

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `it returns a list of most recent emails for a user`() = withServer {
        // given
        val user = User(1, "john@email.com", "John", "1234")
        val emails = (1..10).map {
            Email(
                it,
                user.id,
                user.email,
                "recipient$it@email.com",
                "subject-$it",
                "content-$it",
                Instant.now(),
                Instant.now()
            )
        }

        coEvery { repo.findUser(user.id) } returns user
        coEvery { repo.findEmailsBySender(user.id) } returns emails

        // when
        val call = handleRequest(HttpMethod.Get, "/v1/emails") {
            addJwtHeader(user)
        }

        // then
        assertEquals(HttpStatusCode.OK, call.response.status())

        coVerify {
            repo.findUser(any())
            repo.findEmailsBySender(user.id)
            mailerService wasNot Called
        }
        confirmVerified(repo, mailerService)
    }

    @Test
    fun `it persists and send a new email`() = withServer {
        // given
        val user = User(1, "john@email.com", "John", "1234")
        val email =
            Email(1, user.id, user.email, "recipient@email.com", "subject-", "content-", Instant.now(), Instant.now())
        val payload = mapOf(
            "to" to email.toEmail,
            "subject" to email.subject,
            "content" to email.content
        )

        coEvery { repo.findUser(user.id) } returns user
        coEvery { repo.createEmail(user, email.toEmail, email.subject, email.content) } returns email
        coEvery { mailerService.sendAsync(email) } just Runs

        // when
        val call = handleRequest(HttpMethod.Post, "/v1/emails") {
            addJwtHeader(user)
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody(Gson().toJson(payload))
        }

        // then
        assertEquals(HttpStatusCode.Created, call.response.status())

        coVerify {
            repo.findUser(any())
            repo.createEmail(user, email.toEmail, email.subject, email.content)
            mailerService.sendAsync(email)
        }
        confirmVerified(repo, mailerService)
    }

    private fun withServer(block: TestApplicationEngine.() -> Unit) {
        withTestApplication(
            {
                configureEnvironment()
                moduleWithDependencies(repo, JwtService(environment.config), mailerService)
            },
            block
        )
    }

    private fun Application.configureEnvironment() {
        (environment.config as MapApplicationConfig).apply {
            put("ktor.environment", "dev")
            put("ktor.security.jwtSecret", "898748274728934843")
        }
    }

    private fun TestApplicationRequest.addJwtHeader(user: User) = run {
        val jwt = JWT.create()
            .withSubject("Authentication")
            .withIssuer("simplemailer")
            .withClaim("id", user.id)
            .withExpiresAt(Date(System.currentTimeMillis() + 3_600_000 * 24))
            .sign(Algorithm.HMAC512("898748274728934843"))
        addHeader("Authorization", "Bearer $jwt")
    }
}
