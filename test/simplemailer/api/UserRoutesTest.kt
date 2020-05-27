package simplemailer.api

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.google.gson.Gson
import dev.simonestefani.moduleWithDependencies
import dev.simonestefani.simplemailer.authentication.JwtService
import dev.simonestefani.simplemailer.authentication.hash
import dev.simonestefani.simplemailer.mailer.RedundantMailerService
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
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import java.util.Date
import kotlin.test.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@KtorExperimentalAPI
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRoutesTest {

    private val repo: ExposedRepository = mockk()
    private val mailerService: RedundantMailerService = mockk()

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `it can register a new user`() = withServer {
        // given
        val plaintextPassword = "secret"
        val user = User(1, "john@email.com", "John", application.hash(plaintextPassword))
        val payload = mapOf("name" to user.name, "email" to user.email, "password" to plaintextPassword)

        coEvery { repo.createUser(user.email, user.name, user.passwordHash) } returns user

        // when
        val call = handleRequest(HttpMethod.Post, "/v1/users/register") {
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody(Gson().toJson(payload))
        }

        // then
        assertEquals(HttpStatusCode.Created, call.response.status())

        coVerify {
            repo.createUser(user.email, user.name, any())
            mailerService wasNot Called
        }
        confirmVerified(repo, mailerService)
    }

    @Test
    fun `it can login an existing user`() = withServer {
        // given
        val plaintextPassword = "secret"
        val user = User(1, "john@email.com", "John", application.hash(plaintextPassword))
        val payload = mapOf(
            "email" to user.email,
            "password" to plaintextPassword
        )

        coEvery { repo.findUserByEmail(user.email) } returns user

        // when
        val call = handleRequest(HttpMethod.Post, "/v1/users/login") {
            addHeader(HttpHeaders.ContentType, "application/json")
            setBody(Gson().toJson(payload))
        }

        // then
        assertEquals(HttpStatusCode.OK, call.response.status())

        coVerify {
            repo.findUserByEmail(user.email)
            mailerService wasNot Called
        }
        confirmVerified(repo, mailerService)
    }

    @Test
    fun `it can get the profile of the authenticated user`() = withServer {
        // given
        val user = User(1, "john@email.com", "John", application.hash("secret"))

        coEvery { repo.findUser(user.id) } returns user

        // when
        val call = handleRequest(HttpMethod.Get, "/v1/users/profile") {
            addHeader(HttpHeaders.ContentType, "application/json")
            addJwtHeader(user)
        }

        // then
        assertEquals(HttpStatusCode.OK, call.response.status())

        coVerify {
            repo.findUser(user.id)
            mailerService wasNot Called
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
            put("ktor.allowUserRegistration", "true")
            put("ktor.security.jwtSecret", "898748274728934843")
            put("ktor.security.secretKey", "898748274728934843")
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
