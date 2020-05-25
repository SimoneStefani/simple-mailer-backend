// package dev.simonestefani
//
// import com.google.gson.Gson
// import dev.simonestefani.simplemailer.authentication.JwtService
// import dev.simonestefani.simplemailer.models.User
// import dev.simonestefani.simplemailer.persistence.ExposedRepository
// import io.ktor.http.HttpMethod
// import io.ktor.http.HttpStatusCode
// import io.ktor.server.testing.handleRequest
// import io.ktor.server.testing.setBody
// import io.ktor.server.testing.withTestApplication
// import io.ktor.util.KtorExperimentalAPI
// import io.mockk.clearAllMocks
// import io.mockk.coEvery
// import io.mockk.coVerify
// import io.mockk.every
// import io.mockk.mockk
// import io.mockk.verify
// import io.netty.handler.codec.http.HttpHeaders.addHeader
// import org.junit.jupiter.api.AfterEach
// import org.junit.jupiter.api.BeforeEach
// import org.junit.jupiter.api.Test
// import org.junit.jupiter.api.TestInstance
// import kotlin.test.assertEquals
//
// @KtorExperimentalAPI
// @TestInstance(TestInstance.Lifecycle.PER_CLASS)
// class UsersRoutesTest {
//
//    private lateinit var repo: ExposedRepository
//    private lateinit var jwtService: JwtService
//
//    @BeforeEach
//    fun setUp() {
//        repo = mockk()
//        jwtService = mockk()
//    }
//
//    @AfterEach
//    fun tearDown() {
//        clearAllMocks()
//    }
//
//    @Test
//    fun testRoot() {
//        withTestApplication({ moduleWithDependencies(repo, jwtService) }) {
//            // given
//            val user = User(1, "john@email.com", "John", "1234")
//            val password = "secret"
//
//            coEvery { repo.createUser(user.email, user.name, password) } returns user
//            every { jwtService.generateToken(user) } returns user.passwordHash
//
//            // when
//            val call = handleRequest(HttpMethod.Get, "/v1/") {
//                setBody(Gson().toJson(user.copy(passwordHash = password)))
//            }
//
//            // then
//            assertEquals(HttpStatusCode.Created, call.response.status())
//
//            coVerify { repo.createUser(user.email, user.name, password) }
//            verify { jwtService.generateToken(user) }
//        }
//    }
// }
