package com.configset.server.auth

import com.auth0.jwt.interfaces.JWTVerifier
import com.configset.sdk.extension.createLoggerStatic

private val log = createLoggerStatic<OAuth2Authenticator>()

class OAuth2Authenticator(
    private val verificationAlgorithm: JWTVerifier,
) : Authenticator {

    override fun getUserInfo(accessToken: String): UserInfo {
        return try {
            val decodedJwt = verificationAlgorithm.verify(accessToken)
            val roles = (decodedJwt.claims["resource_access"]
                ?.asMap()
                ?.get("demo-clientId") as? LinkedHashMap<*, *>)
                ?.get("roles") as? List<String>
            requireNotNull(roles)

            LoggedIn(decodedJwt.claims["preferred_username"]!!.asString(), roles.toHashSet())
        } catch (e: Exception) {
            log.info("Unable to process access_token", e)
            Anonymous
        }
    }
}
