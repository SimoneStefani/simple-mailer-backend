package dev.simonestefani.simplemailer.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import dev.simonestefani.simplemailer.models.User
import io.ktor.config.ApplicationConfig
import io.ktor.util.KtorExperimentalAPI
import java.util.Date

const val ONE_DAY = 3_600_000 * 24

@KtorExperimentalAPI
class JwtService(appConfig: ApplicationConfig) {

    private val issuer = "simplemailer"
    private val subject = "Authentication"
    private val jwtSecret = appConfig.property("ktor.security.jwtSecret").getString()
    private val algorithm = Algorithm.HMAC512(jwtSecret)

    val verifier: JWTVerifier = JWT
        .require(algorithm)
        .withIssuer(issuer)
        .build()

    fun generateToken(user: User): String = JWT.create()
        .withSubject(subject)
        .withIssuer(issuer)
        .withClaim("id", user.id)
        .withExpiresAt(expiresAt())
        .sign(algorithm)

    private fun expiresAt() = Date(System.currentTimeMillis() + ONE_DAY)
}
