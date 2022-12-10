package com.configset.common.backend.auth

import com.auth0.jwt.interfaces.DecodedJWT
import java.time.Instant

class JwtAuthentication(
    private val decodedJwt: DecodedJWT,
    private val clientId: String,
    override val userName: String,
    override val accessToken: String,
) : UserInfo {

    override val roles: Set<Role> = run {
        val roles: List<String> = (
                decodedJwt.claims["resource_access"]
                    ?.asMap()
                    ?.get(clientId) as? LinkedHashMap<*, *>
                )
            ?.get("roles") as? List<String> ?: emptyList()
        roles.map { parseRole(it) }
    }.toHashSet()

    override fun accessTokenExpired(instant: Instant): Boolean {
        return decodedJwt.expiresAt.toInstant() < instant
    }
}

fun parseRole(role: String): Role {
    return when {
        role == "admin" -> Admin
        role == "hostCreator" -> HostCreator
        role.startsWith("applicationOwner_") -> ApplicationOwner(role.split("_", limit = 2)[1])
        else -> error("Unknown role $role")
    }
}
