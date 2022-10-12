package com.configset.dashboard

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

object AccessTokenTestUtils {

    fun createAccessToken(): String {
        return JWT.create()
            .withPayload(mapOf("foo" to "bar"))
            .withExpiresAt(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)))
            .sign(Algorithm.HMAC256("123981823"))
    }
}