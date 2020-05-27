package dev.simonestefani.simplemailer.authentication

import io.ktor.application.Application
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.hex
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@KtorExperimentalAPI
fun Application.hash(password: String): String {
    val jwtSecret = environment.config.property("ktor.security.secretKey").getString()

    val hashKey = hex(jwtSecret)
    val hmacKey = SecretKeySpec(hashKey, "HmacSHA1")
    val hmac = Mac.getInstance("HmacSHA1")

    hmac.init(hmacKey)

    return hex(hmac.doFinal(password.toByteArray(Charsets.UTF_8)))
}
