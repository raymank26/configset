package com.configset.sdk.auth

import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Instant

class JwtAuthentication(
    private val decodedJwt: DecodedJWT,
    private val clientId: String,
    override val userName: String,
    override val accessToken: String,
) : UserInfo {

    override val roles: Set<String> = run {
        (decodedJwt.claims["resource_access"]
            ?.asMap()
            ?.get(clientId) as? LinkedHashMap<*, *>)
            ?.get("roles") as? List<String> ?: emptyList()
    }.toHashSet()

    override fun accessTokenExpired(instant: Instant): Boolean {
        return decodedJwt.expiresAt.toInstant() < instant
    }
}